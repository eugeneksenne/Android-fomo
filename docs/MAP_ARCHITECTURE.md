# Map Architecture

The Map feature is a **venue discovery engine**, not just a map. It is
organized the same way the Discover feature was: a thin orchestration shell
plus feature modules, all remaining in the `com.example.feature.map` package
tree so existing navigation (`FomoApp.kt`'s `composable<MapRoute> { MapScreen(...) }`)
requires no changes.

This document also captures the plan referenced in the "FOMO Map Engine
(Billion-Dollar Production Implementation Plan, Version 4.0)" brief for a
future migration to Organic Maps and a realtime social layer. **That
migration has not started.** The current engine is Leaflet + WebView, and
this refactor deliberately did not change that - see "What this refactor is
not" below.

## Architecture diagram

```text
MapScreen (route shell)
        │
MapScreenState (declarative state holder)
        │
   ┌────┼─────────────┬───────────────┬────────────────┬───────────────────┐
   │    │             │               │                │                   │
MapTopBar  CountryPackChips   NearestVenueCard   VenueMapCanvas   MapFloatingButtons
   │                                    │                │
CityStatusChip                    (WebViewMap +      NearbyVenueCarousel
                                    LeafletHtml +           │
                                    MarkerRenderer)   VenuePreviewOverlay
                                                        (VenueDetailsPanel /
                                                         FriendDetailsPanel)

Dialogs (search / notifications / profile / add place) hosted by MapScreen,
driven entirely by MapScreenState.
```

## Folder structure

```text
feature/map/
  MapScreen.kt                    # Orchestration shell: state collection, layout composition
  state/
    MapScreenState.kt             # SelectedMapItem, MapBottomTab, MapScreenState (single source of truth)
  components/
    MapTopBar.kt
    CountryPackChips.kt
    SectionHeader.kt               # Bottom-sheet Live Spots / Friends Map tab header
    MapFloatingButtons.kt
    CityStatusChip.kt
  cards/
    NearestVenueCard.kt            # Assembles the header/hero/details/action sub-composables below
    NearestVenueHeader.kt
    NearestVenueHeroImage.kt
    NearestVenueDetailsColumn.kt
    NearestVenueActionRow.kt
    NearbyVenueCarousel.kt         # HorizontalVenueCard + HorizontalFriendCard + the carousel shell
    VenuePreviewCard.kt            # VenueDetailsPanel (the compact "selected pin" card)
    FriendDetailsPanel.kt
  map/
    VenueMapCanvas.kt              # Builds Leaflet HTML from venues/friends, hosts WebViewMap
    WebViewMap.kt                  # Raw AndroidView<WebView> + AndroidBridge JS interface
    LeafletHtml.kt                 # The Leaflet HTML/CSS/JS page (unchanged from pre-refactor)
    MarkerRenderer.kt              # WebView.evaluateJavascript(...) command wrappers
  overlays/
    VenuePreviewOverlay.kt         # Renders VenueDetailsPanel/FriendDetailsPanel for the selected pin
    WebsiteViewer.kt               # Thin wrapper around the shared FomoWebsiteViewerSheet
  dialogs/
    AddPlaceDialog.kt              # AddPlaceOverlayDialog + tab selector + venue-generation logic
    AddPlaceFormTab.kt             # The 3 data-entry tabs (Venue / Event / Temp Party)
    SearchOverlayDialog.kt
    NotificationsOverlayDialog.kt
    UserProfileOverlayDialog.kt
  util/
    VenueFilter.kt                 # Category normalisation + filterByCategory + hasEvent
    VenueRanking.kt                 # nearestVenue + vibeScore
    VenueActionLabels.kt            # Shared category -> action-button copy (was duplicated 3x)
    MarkerGenerator.kt              # Builds addVenueMarker/addFriendMarker JS call strings
    MapCoordinates.kt               # getVenueCoordinates lookup + defaultFomoClubVenue fallback
```

## Shell responsibilities

`MapScreen.kt` should stay under ~350 lines and only handle:

- collecting repository state (`VenueRepository.exploreVenuesState`,
  `MyCircleRepository.friendsState`) and merging it with
  `MapScreenState.customAddedVenues`;
- deriving `filteredVenues` (via `VenueFilter.filterByCategory`) and
  `nearestVenue` (via `VenueRanking.nearestVenue`);
- holding the raw `WebView` reference reported back by `VenueMapCanvas`, so
  every other component can issue further JS commands through
  `MarkerRenderer`;
- composing the HUD layout: top bar → chips → nearest venue card → map
  canvas → floating buttons → venue/friend preview → bottom carousel;
- wiring every callback to a `MapScreenState` intent (`state::openSearch`,
  `state::selectCategory`, etc) instead of owning local `mutableStateOf`
  fields itself;
- hosting the four dialogs (search, notifications, profile, add place) and
  the website viewer, each gated on the matching `MapScreenState` flag.

The shell does **not** own overlay/dialog selection state, category state,
heatmap state, or the bottom-tab selection directly - that all lives in
`MapScreenState`, mirroring `DiscoverOverlayState`'s role for the Discover
screen.

## Rendering pipeline

1. `MapScreen` computes `allVenues` (`staticVenues + customAddedVenues`) and
   passes it, plus `friends`, to `VenueMapCanvas`.
2. `VenueMapCanvas` calls `MarkerGenerator.buildVenueMarkersScript` /
   `buildFriendMarkersScript` to produce the initial `addVenueMarker(...)` /
   `addFriendMarker(...)` JS calls, splices them into
   `LeafletHtml.buildLeafletHtml(...)`, and remembers the resulting HTML
   string keyed on `(venues, friends)` so it's only rebuilt when the lists
   actually change.
3. `WebViewMap` loads that HTML into a raw `android.webkit.WebView` via
   `loadDataWithBaseURL`, and exposes an `AndroidBridge` JS interface with
   three callbacks (`onVenueClick`, `onFriendClick`, `onOverpassResult`) that
   `VenueMapCanvas` resolves back into real `ExploreVenue` / `CircleFriend`
   objects (including the `fomo_club` → `defaultFomoClubVenue` fallback).
4. Once the `WebView` is created, `onWebViewReady` reports it back up to
   `MapScreen`, which stores it in a local `webViewRef` and passes it to
   every `MarkerRenderer` call site (top bar recenter, chips filter, nearest
   card route, floating buttons heatmap/recenter/overpass, carousel cards
   route, dialogs' "locate venue" deep links).
5. All outbound map commands (`centerOn`, `drawRoute`, `clearRoute`,
   `toggleHeatmap`, `filterCategory`, `fetchOverpassPOIs`,
   `addCustomVenueMarker`) go through `MarkerRenderer`'s null-safe
   `WebView.evaluateJavascript(...)` wrappers - no call site touches raw JS
   strings directly except `MarkerRenderer` and `MarkerGenerator` themselves.

## Marker pipeline

- `MarkerGenerator.buildAddVenueMarkerCall` (private) determines the same
  curated flag set the pre-refactor screen used: `hasFlashDrop`, `isLive`,
  `hasEvent` (via `VenueFilter.hasEvent`), `friendsCount`, `isSponsored`,
  `isTrending`, `isHot`, `isClosingSoon` - all keyed off specific venue ids
  (`fomo_club`, `d48_midrand`, `konka_soweto`, `four_seasons_westcliff`,
  `taboo_sandton`). This is intentionally unchanged mock curation; a future
  pass should move these flags onto real venue/repository fields (Flash
  Drops repository, live-broadcast state, etc) instead of hardcoded ids.
- `MarkerGenerator.buildAddCustomVenueScript` handles the Add Place dialog's
  "append one marker + fly camera to it" case with a fixed vibe score of 90
  and only the "closing soon" ring flag set, matching the pre-refactor
  behaviour exactly.
- The Leaflet-side `addVenueMarker` / `addFriendMarker` JS functions
  (`LeafletHtml.kt`) render the neon glow rings, badge overlays and glyphs;
  they are unchanged from the pre-refactor inline HTML.

## Filtering pipeline

- `VenueFilter.normalizeCategory` strips emoji/punctuation and lowercases a
  category chip label (e.g. `"🌙 Nightlife"` → `"nightlife"`).
- `VenueFilter.filterByCategory` applies the same three-way branch as the
  pre-refactor inline `filteredVenues`: `"all"` passes everything through,
  `"wellness"` matches both `Recover` and `Wellness` venue categories,
  `"events"` matches a curated id list plus any `evt_`-prefixed venue, and
  everything else does a plain category-string match.
- `VenueRanking.nearestVenue` picks the highest-rated venue from the already
  filtered list, falling back to `defaultFomoClubVenue` when the filtered
  list is empty.
- The Leaflet-side `filterCategory(categoryName)` JS function performs the
  equivalent filter against the map's own marker registry so pins on the map
  match the filtered carousel; `MapScreen` calls both
  `MarkerRenderer.filterCategoryAndFetchOverpass` (map-side filter +
  Overpass POI refresh) whenever `CountryPackChips` reports a new selection.

## State model

`MapScreenState` (in `state/MapScreenState.kt`) is a plain class exposing
`mutableStateOf`-backed properties with private setters, plus public intent
functions:

| Concern | Properties | Intents |
| --- | --- | --- |
| Category filter | `selectedCategory` | `selectCategory(category)` |
| Map selection | `selectedMapItem` | `selectMapItem(item)`, `clearSelection()` |
| Heatmap | `isHeatmapEnabled` | `toggleHeatmap()` |
| Bottom carousel tab | `bottomTab` | `selectBottomTab(tab)` |
| Search dialog | `isSearchOpen` | `openSearch()`, `closeSearch()` |
| Notifications dialog | `isNotificationsOpen` | `openNotifications()`, `closeNotifications()` |
| Profile dialog | `isProfileOpen` | `openProfile()`, `closeProfile()` |
| Add Place dialog | `isAddPlaceOpen` | `openAddPlace()`, `closeAddPlace()` |
| Website viewer | `activeWebsiteUrl`, `activeWebsiteTitle` | `openWebsite(url, title)`, `closeWebsite()` |
| City status badge | `cityStatusIndex` | `cycleCityStatus(statusCount)` |
| Session-added venues | `customAddedVenues` | `addCustomVenue(venue)` |
| Bulk reset | - | `dismissAll()` |

`rememberMapScreenState()` creates and remembers one instance per
`MapScreen` composition, exactly like `rememberDiscoverOverlayState()` does
for Discover.

## What this refactor is not

Per the refactor brief, this pass deliberately did **not**:

- rewrite the map engine (Leaflet is unchanged - HTML/CSS/JS byte-for-byte
  identical to the pre-refactor inline strings, verified during the split);
- migrate to Organic Maps;
- change any user-visible behaviour, marker styling, JS bridge contract, or
  dialog copy.

It only moved existing code into the module boundaries above and replaced
~14 independent `mutableStateOf` shell fields with `MapScreenState`.

## Future: Organic Maps migration plan

**Source repo reviewed:** [github.com/organicmaps/organicmaps](https://github.com/organicmaps/organicmaps)
(Apache License 2.0 for the app; map data itself ships under a separate,
non-free-redistribution license). Organic Maps is **not a drop-in Gradle
dependency** - it's a full offline-maps application built from a C++ core
(rendering, routing, search, indexing) wrapped by a thin Java/Kotlin Android
layer over JNI, plus native map-data downloading/storage. Its repo layout is
roughly:

```text
organicmaps/
  android/                # Java/Kotlin app module, JNI bindings, Gradle build
  iphone/, xcode/         # iOS/macOS targets (not relevant to us)
  3party/                 # Vendored native dependencies (Boost, zlib-ng, etc.)
  drape*, map, routing, search, indexer, storage, platform, geometry, ...
                          # C++ engine (rendering pipeline, routing graph,
                          #   full-text search, mwm/data indexing, low-level
                          #   platform + geometry primitives)
  data/                   # Styles, categories, resources shipped with the app
```

Practically, this means "integrate Organic Maps" is a native-build
integration (CMake + NDK + the vendored C++ engine), not a `implementation("app.organicmaps:sdk:x.y.z")`
one-liner. The long-term target (see the Version 4.0 brief) replaces the
Leaflet/WebView canvas with a native Organic Maps view while keeping every
other layer of this architecture intact:

1. **Introduce `organicmaps/` module boundary.** Vendor/integrate the Organic
   Maps rendering, routing, search and downloader engines (as a native
   module built via CMake/NDK, following upstream's `android/` + C++ core
   layout) behind a narrow Kotlin interface (camera control, marker
   placement, gesture callbacks) that mirrors what `MarkerRenderer` +
   `WebViewMap` expose today.
2. **Replace `VenueMapCanvas`'s implementation only.** Because `MapScreen`
   only depends on `VenueMapCanvas`'s public contract
   (`venues`, `friends`, `onVenueSelected`, `onFriendSelected`,
   `onOverpassResult`, `onWebViewReady`), swapping the WebView/Leaflet
   internals for a native Organic Maps view should not require changes to
   `MapScreen.kt`, any `cards/`, `components/`, `dialogs/` or `overlays/`
   file, or `MapScreenState`.
3. **Replace `MarkerRenderer`'s JS wrappers with native camera/marker calls.**
   Each function (`centerOn`, `drawRoute`, `toggleHeatmap`, `filterCategory`,
   `fetchOverpassPOIs`, `addCustomVenueMarker`) gets a native
   Organic-Maps-backed implementation with the same signature, so call sites
   across the feature module are untouched.
3. **Venue-pin-only philosophy.** Per the brief, the long-term map renders
   venue pins as the *only* permanent pin type; Flash Drops/Live/Events/
   Sponsored/Trending/Closing Soon become badges on venue pins (this is
   already how `MarkerGenerator` models them today), and friend pins become
   conditional on active NightGuard location sharing rather than always-on.
4. **Offline-first data engine.** Country Packs (already modelled in
   `CountryPackRepository`) become the offline map/venue/search backing
   store that Organic Maps' downloader manages.
5. **Rollout strategy.** Ship behind a feature flag so Leaflet remains the
   fallback renderer until the native engine reaches parity on: offline maps,
   offline routing, offline search, marker clustering, and performance
   across low/mid/high-end devices (see the Testing Checklist in the
   Version 4.0 brief).
6. **Data-licensing constraint to resolve up front.** Organic Maps' app code
   is Apache-2.0, but the pre-built `.mwm` map data files it ships are under
   a separate, non-free-redistribution license (per the OpenStreetMap Wiki
   and Wikipedia's Organic Maps entries). FOMO's own venue/Country Pack data
   (`CountryPackRepository`) would need its own OSM-derived extraction/build
   pipeline rather than assuming Organic Maps' data files can be redistributed
   as-is inside FOMO's app package - this should be confirmed with counsel
   before committing to the migration timeline.

## Future: realtime layer integration plan

Today, "live" signals (Flash Drops, live broadcasts, friend presence,
trending/hot status) are static per-id curation baked into
`MarkerGenerator`. The planned realtime layer:

1. **Presence/social engine.** Friend location (`CircleFriend.latitude`/
   `longitude`) and status become a live `Flow` from a presence service
   (already scaffolded by `MyCircleRepository.friendsState`), gated behind
   explicit NightGuard sharing consent per the Version 4.0 brief's privacy
   principle.
2. **Flash Drops / events / trending as live repository state.** Replace the
   curated id lists in `MarkerGenerator` (`hasFlashDrop`, `isLive`,
   `isSponsored`, `isTrending`, `isHot`, `isClosingSoon`) with fields sourced
   from `VenueRepository`'s Flash Drop / event state (already partially
   modelled for Discover) so marker badges reflect real-time data instead of
   hardcoded venue ids.
3. **Incremental sync.** `VenueMapCanvas`'s `remember(venues, friends)` HTML
   rebuild is coarse (full page reload on any list change). A realtime layer
   should move to incremental JS calls (`addVenueMarker`/`removeVenueMarker`/
   `updateVenueBadges`) driven by a diffing layer, avoiding a full WebView
   reload per update - this is the same integration point Organic Maps'
   native marker API would use.
4. **Viewport-based loading.** Once markers are backed by a live feed rather
   than a fully-materialized list, load/unload venues based on the map's
   current viewport (mirroring the Overpass POI query's bounding-box
   pattern already present in `fetchOverpassPOIs`).

## Testing

- `MapArchitectureTest` verifies: no oversized composables, `MapScreen.kt`
  under 350 lines, every expected module file exists, `MapScreenState`
  declares every expected intent function, the shell doesn't reintroduce raw
  overlay `mutableStateOf` fields, Leaflet HTML/WebView construction lives
  only in `map/`, marker generation lives only in `util/`, and category
  filtering isn't inlined back into the shell.
- `MapFilterTest` verifies `VenueFilter` category normalisation/filtering/
  `hasEvent`, `VenueRanking`'s nearest-venue and vibe-score logic,
  `getVenueCoordinates`'s curated + fallback lookup, and `MarkerGenerator`'s
  JS-call construction for venues, friends, and custom Add Place venues.
- `MapStateTest` verifies every `MapScreenState` intent: category/selection,
  heatmap toggle, bottom tab switch, all four dialog open/close pairs and
  their independence, the website viewer open/close pair, city status
  cycling (including wraparound and the zero-count no-op case), session-added
  venues, and `dismissAll()`.
