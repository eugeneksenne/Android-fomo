# FOMO Progress

## Current Focus
Map screen — module split complete (production hardening + review next)

## Completed So Far
### Discover
- Split the shared Discover UI out of the legacy `DiscoverSections.kt` monolith and **removed the monolith**.
- Reduced `DiscoverScreen.kt` to a route shell and overlay orchestration (~170 lines, ceiling 220).
- Extracted overlay/dialog selection state into `DiscoverOverlayState` (single source of truth, replacing a dozen independent `mutableStateOf` flags) and overlay rendering into `DiscoverOverlayHost`.
- Preserved the Discover section order from the current implementation.
- Preserved the section-level `See all` callbacks for Discover modules.
- Kept the hero section at the top of the Discover layout.
- Kept the Discover overlays and preview flows wired to the existing navigation callbacks.
- Added `DiscoverSeeAllRoutingTest` verifying every section's "See all" affordance routes to the correct destination.
- Added `DiscoverOverlayStateTest` unit-testing `DiscoverOverlayState`'s open/dismiss/dismissAll contract and overlay-slot independence.

### Map
- Replaced all local `mutableStateOf(...)` in `MapScreen.kt` (~14 independent fields: category, selected pin, heatmap, bottom tab, 4 dialog booleans, website viewer url/title, city-status index, plus the session-added-venues list) with a single `MapScreenState` (`state/MapScreenState.kt`), following the same pattern as `DiscoverOverlayState`.
- Split `MapScreen.kt` exactly like Discover into `state/`, `components/`, `cards/`, `map/`, `overlays/`, `dialogs/`, `util/` per the target folder structure. `MapScreen.kt` shrank from 2,424 lines to 283.
- Moved all WebView/Leaflet code into `map/` (`VenueMapCanvas.kt`, `WebViewMap.kt`, `LeafletHtml.kt`) — Leaflet HTML/CSS/JS verified **byte-for-byte identical** to the pre-refactor inline strings (diffed programmatically after variable-name normalisation).
- Extracted marker generation into `util/MarkerGenerator.kt` (venue markers incl. Flash Drop/Live/Event/Sponsored/Trending/Hot/Closing Soon badge flags, friend markers, and the Add Place "custom venue" marker script).
- Extracted venue filtering into `util/VenueFilter.kt` (category normalisation, `filterByCategory`, `hasEvent`) and ranking into `util/VenueRanking.kt` (`nearestVenue`, `vibeScore`).
- Extracted `util/VenueActionLabels.kt` to de-duplicate the category→action-button copy that was previously copy-pasted across `NearestVenueCard`, `HorizontalVenueCard`, and `VenueDetailsPanel`.
- Extracted `MapTopBar`, `CountryPackChips`, `SectionHeader` (bottom-sheet tab header), `MapFloatingButtons`, `CityStatusChip` into `components/` — all under 250 lines.
- Extracted `NearestVenueCard` (split into `NearestVenueHeader`/`NearestVenueHeroImage`/`NearestVenueDetailsColumn`/`NearestVenueActionRow` sub-composables to satisfy the 250-line guideline), `NearbyVenueCarousel` (incl. `HorizontalVenueCard`/`HorizontalFriendCard`), and `VenuePreviewCard`/`FriendDetailsPanel` into `cards/`.
- Extracted `VenuePreviewOverlay` (selected-pin panel host) and `WebsiteViewer` into `overlays/`.
- Extracted `AddPlaceDialog`/`AddPlaceFormTab`, `SearchOverlayDialog`, `NotificationsOverlayDialog`, `UserProfileOverlayDialog` into `dialogs/`.
- Added `MapArchitectureTest`, `MapFilterTest`, `MapStateTest`.
- Added `docs/MAP_ARCHITECTURE.md` with the architecture diagram, folder structure, rendering/marker/filtering pipelines, and the Organic Maps + realtime layer migration plans.
- Did **not** rewrite the map engine, replace Leaflet, or migrate to Organic Maps — only architecture was improved, per the refactor brief.

## Discover Refactor Checklist
- [x] Extract shared Discover section composables into dedicated files.
- [x] Reduce `DiscoverScreen.kt` to the route shell and overlay orchestration.
- [x] Preserve the existing Discover section order.
- [x] Preserve section-level `See all` callbacks.
- [x] Keep hero content at the top of Discover.
- [x] Keep Discover overlay flows wired to the existing callbacks.
- [x] Split the remaining large Discover section families into dedicated files.
- [x] Split overlay dialogs and detail views into dedicated files (incl. MyMovesHubOverlay + TonightPlanDialogs).
- [x] Review the refactor for symbol collisions, import cleanup, and behavioural parity.
- [x] Verify the screen still composes and navigates correctly after extraction (static verification: no duplicate declarations, no dangling annotations, balanced files, signature parity).

## Discover Production-Hardening Checklist
- [x] Compile-blocking defects fixed (redeclarations, missing `@Composable`, dangling annotation, missing `dp` import).
- [x] Standardised `SectionHeader` "See all →" pattern + analytics on every carousel section (Tonight panel exempt by design).
- [x] Offline states wired through `NetworkMonitor` for Flash Drops, Events, My Circle, Smart Places, Explore City (+ `ACCESS_NETWORK_STATE` permission).
- [x] Empty states for all data-driven sections; loading skeleton + error components available for richer repository states.
- [x] Stable `key` + `contentType` on every Discover `LazyColumn` item; stable model-ID keys on data-driven carousels.
- [x] System back gesture (`BackHandler`) on every full-screen overlay; native back for AlertDialog/ModalBottomSheet surfaces.
- [x] Overlay open/dismiss analytics and shared fade + slide enter transition via the shell's `DiscoverOverlay` wrapper.
- [x] Card-level analytics (`discover_card_opened`, `discover_action_clicked`) across sections.
- [x] Accessibility: content descriptions + button roles on interactive cards, TalkBack labels on showcase cards and state cards.
- [x] Image preloading: `DiscoverImagePrefetcher` warms Coil caches for hero + first-viewport imagery.
- [x] Architecture + analytics unit tests (JVM, no emulator required).
- [x] Compose UI state tests (Robolectric) for offline / empty / populated section rendering.
- [x] Consolidate overlay/dialog selection state in the shell into a single `DiscoverOverlayState` object.
- [x] Add behavioural routing tests for every section's "See all" (or equivalent) affordance.

## Map Refactor Checklist
- [x] Replace local `mutableStateOf(...)` in `MapScreen.kt` with `MapScreenState`.
- [x] Split `MapScreen.kt` into `state/`, `components/`, `cards/`, `map/`, `overlays/`, `dialogs/`, `util/`.
- [x] Move WebView/Leaflet HTML/JS/marker generation/callbacks into `map/`.
- [x] Extract marker generation into `util/MarkerGenerator.kt`.
- [x] Extract venue filtering into `util/VenueFilter.kt`.
- [x] Extract ranking/recommendation logic into `util/VenueRanking.kt`.
- [x] Extract `MapTopBar`, `CountryPackChips`, `NearestVenueCard`, `NearbyVenueCarousel`, `FloatingButtons` into dedicated files, each component under 250 lines.
- [x] Keep existing WebView/Leaflet functionality and behaviour unchanged (verified via programmatic diff of the HTML/JS payload).
- [x] Add `MapArchitectureTest`.
- [x] Add `MapFilterTest`.
- [x] Add `MapStateTest`.
- [x] Add `docs/MAP_ARCHITECTURE.md`.
- [ ] Architecture / Performance / Accessibility / Compose / Security / Code review pass (see "Remaining / Deferred" below).

## Map Production-Hardening Checklist (Remaining)
- [ ] Architecture review sign-off (structure looks sound; needs a second-pass review against `docs/MAP_ARCHITECTURE.md`).
- [ ] Performance review: the Leaflet HTML is rebuilt via `remember(venues, friends)` on every list change (full WebView reload), which is the same behaviour as before the split but is a known scaling concern once venues become a live/large feed — see "realtime layer integration plan" in the architecture doc.
- [ ] Accessibility review: WebView-rendered map pins are not screen-reader accessible today (pre-existing limitation, not introduced by this refactor); the surrounding native Compose HUD (top bar, chips, cards, floating buttons) largely reuses pre-existing content descriptions but has not had a dedicated TalkBack pass.
- [ ] Compose review: confirm no unnecessary recompositions were introduced by the new `MapScreenState`/`MarkerRenderer` indirection (should be neutral or better than the original inline `mutableStateOf` fields, but unverified without a running emulator).
- [ ] Security review: `WebViewMap` still uses `MIXED_CONTENT_ALWAYS_ALLOW` and loads third-party tiles/Overpass API responses over plain fetch from JS — pre-existing behaviour, flagged here for a follow-up hardening pass (e.g. tightening mixed content mode, validating Overpass responses).
- [ ] Code review: a human/second-agent pass is recommended before merging, since this sandbox has no JDK/Gradle to compile-verify the split (see Notes).

## Remaining / Deferred
- Repository evolution from seeded `StateFlow<List<T>>` to richer states so sections can exercise `DiscoverLoadingSkeleton` / `DiscoverErrorState` in production paths.
- Shared element transitions between cards and venue/event detail surfaces (Compose `SharedTransitionLayout`; deferred — high-risk without an emulator build).
- Compose UI screenshot (Roborazzi) recordings for the Discover shell.
- Trimming the copy-pasted import blocks in extracted modules (warnings only; deferred to avoid behavioural risk without an emulator build).
- Map: Organic Maps migration (see `docs/MAP_ARCHITECTURE.md`'s migration plan) — explicitly out of scope for this refactor.
- Map: realtime social/presence layer (see `docs/MAP_ARCHITECTURE.md`'s realtime integration plan) — explicitly out of scope for this refactor.
- Map: moving the curated per-venue-id badge flags in `MarkerGenerator` (Flash Drop/Live/Sponsored/Trending/Hot) onto real repository fields instead of hardcoded ids.

## Notes
- The Discover refactor is complete; remaining work is hardening and richer repository states.
- A review pass after the Discover extractions is complete: static verification passed (duplicates, annotations, balance, signatures).
- The Map module split is complete; a full Architecture/Performance/Accessibility/Compose/Security/Code review pass is still outstanding (see "Map Production-Hardening Checklist" above). This sandbox has no JDK/Gradle/network access, so all Map refactor verification was done statically: brace/paren balance checks across every new file, cross-referencing every call site's signature against the real composable/function definitions, and a programmatic diff proving the extracted Leaflet HTML/JS is byte-for-byte identical to the pre-refactor inline strings. A real Gradle build/instrumented test run is recommended before merging.
