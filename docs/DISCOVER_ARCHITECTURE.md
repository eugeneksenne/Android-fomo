# Discover Architecture

The Discover feature is organized as a thin orchestration shell plus feature modules. All files remain in the `com.example.feature.discover` package so existing navigation and Compose tests can continue to reference public composables without migration churn.

## Structure

```text
feature/discover/
  DiscoverScreen.kt              # Orchestration shell: state collection, section order, section->intent wiring
  DiscoverOverlayState.kt        # Single source of truth for overlay/dialog selection ("what's open right now")
  DiscoverOverlayHost.kt         # Renders whatever DiscoverOverlayState currently holds + overlay analytics wrapper
  DiscoverAnalytics.kt           # UI analytics facade
  components/
    DiscoverTopBar.kt
    HeroSection.kt
    SectionHeader.kt
    SectionSpacer.kt
    DiscoverStateContent.kt      # Loading / empty / error / offline state components
  sections/
    ClosingSoonSection.kt
    FlashDropsSection.kt
    MyCircleSection.kt
    LiveMomentsSection.kt
    SmartPlacesSection.kt
    TrendingSection.kt
    EventsSection.kt
    ExploreCitySection.kt
    ChannelsSection.kt
    PrepRoomsSection.kt
    TonightSection.kt
  overlays/
    VenuePreviewOverlay.kt
    StoryViewerOverlay.kt
    MyCircleOverlay.kt
    FlashDropsOverlay.kt         # FlashDropsHubOverlay + FlashDropDetailOverlay
    SmartPlacesOverlay.kt
    MyMovesHubOverlay.kt         # Tonight / My Moves hub
    (ExploreTheCityScreen.kt, ChannelsScreen.kt and PrepRoomsScreen.kt co-locate
     their hub overlays with their full-screen destinations)
  dialogs/
    FlashDropClaimDialog.kt
    FlashDropRouteDialog.kt
    SmartPlaceRouteDialog.kt
    GlobalPlanContextSheet.kt
    TonightPlanDialogs.kt        # CreatePlanModalDialog + SplitFareDialog
```

The legacy `DiscoverSections.kt` monolith has been removed; its symbols now live exclusively in the modules above. `DiscoverArchitectureTest` guards against its return.

## Shell responsibilities

`DiscoverScreen.kt` should remain below 220 lines (down from the earlier 360-line ceiling, now that overlay/dialog selection state lives entirely in `DiscoverOverlayState`) and only handle:

- collecting repository state;
- initialising and observing `NetworkMonitor` (offline awareness);
- rendering the 12-section ordering with stable `key` and `contentType` values;
- wiring each section's intents (e.g. `onSeeAllClick`, `onVenueClick`) directly to
  `DiscoverOverlayState` methods (`overlayState::openX`);
- dispatching navigation callbacks;
- delegating overlay/dialog rendering to `DiscoverOverlayHost`.

The shell does **not** own overlay/dialog selection state itself. That single
source of truth lives in `DiscoverOverlayState` (a plain `remember`-backed
class exposing `open*()` / `dismiss*()` intent functions and read-only
`by mutableStateOf` properties). Before this existed, `DiscoverScreen` held a
dozen independent `mutableStateOf` flags directly in the composable body,
which made the shell hard to scan and easy to leave in an inconsistent state.
Now sections just reference `overlayState::openVenuePreview`,
`overlayState::openFlashDropsHub`, etc., as callback values, and
`DiscoverOverlayHost` renders whichever overlay/dialog is currently selected.

Business rules, card rendering, overlay UI and reusable loading/empty/error/offline states belong in feature modules.

### `DiscoverOverlayState`

`DiscoverOverlayState.kt` centralises every overlay/dialog selection value
Discover can show: venue preview, My Circle hub, story viewer, Flash Drop
claim/detail/route, the global plan sheet, Prep Rooms, Channels, Explore The
City, the Flash Drops hub and the Smart Places hub. Each piece of state is a
private-setter `mutableStateOf` paired with public `openX()` / `dismissX()`
functions, plus a `dismissAll()` escape hatch. `rememberDiscoverOverlayState()`
creates and remembers one instance per `DiscoverScreen` composition.

### `DiscoverOverlayHost`

`DiscoverOverlayHost.kt` is a pure function of `DiscoverOverlayState`: given
the current state plus the repository-backed lists it needs (explore venues,
stories, global flash drops) and the shell's navigation callbacks, it renders
whichever overlay/dialog is currently selected. It also hosts the shared
`DiscoverOverlay` wrapper used by every full-screen overlay for
`discover_overlay_opened` / `discover_overlay_dismissed` analytics and the
fade + slide enter transition.

## Section standard

Carousel sections use the shared `SectionHeader` pattern:

```text
Title                         See all →
Subtitle
```

`SectionHeader` emits `discover_section_viewed` and `discover_see_all_clicked` events through `DiscoverAnalytics`, appends the `→` affordance automatically, and exposes the action as an accessibility button.

Exception: `TonightSection` is an intentionally branded "My Moves" panel with its own CTA header and is exempt from the shared header pattern (enforced in tests).

"See all" destinations:

| Section        | Destination                          |
| -------------- | ------------------------------------ |
| Closing Soon   | Smart Places hub (closing venues)    |
| Flash Drops    | FlashDropsHubOverlay                 |
| My Circle      | MyCircleHubOverlay                   |
| Live Moments   | MyCircleHubOverlay (activity feed)   |
| Smart Places   | SmartPlacesHubOverlay                |
| Trending Now   | ExploreTheCityOverlay                |
| Events         | EventsHomeScreen (navigation)        |
| Explore City   | ExploreTheCityOverlay                |
| Channels       | ChannelsOverlay                      |
| Prep Rooms     | PrepRoomsOverlay                     |
| Tonight        | Plans Workspace (header CTA)         |

## State standards

Data-driven sections (`FlashDrops`, `Events`, `MyCircle`, `SmartPlaces`, `ExploreCity`) accept `isOnline: Boolean` and `onRetry: () -> Unit` from the shell and render, in order of precedence:

- `DiscoverOfflineState` when offline and no cached data;
- `DiscoverEmptyState` when online with no data;
- the live carousel otherwise.

`DiscoverLoadingSkeleton` and `DiscoverErrorState` are available in `components/DiscoverStateContent.kt`. They activate when repositories evolve from seeded `StateFlow<List<T>>` values to richer state models carrying `isLoading` / error channels.

## Overlay standards

Every full-screen overlay must:

- handle the system back gesture with `BackHandler { onDismiss() }`;
- be hosted through the `DiscoverOverlay` wrapper in `DiscoverOverlayHost.kt`, which emits `discover_overlay_opened` / `discover_overlay_dismissed` analytics and provides the shared fade + slide enter transition.

`AlertDialog`- and `ModalBottomSheet`-based surfaces (FlashDropClaimDialog, FlashDropRouteDialog, GlobalPlanContextSheet) rely on the platform's native back handling and dismiss animations.

## Performance standards

- The main Discover `LazyColumn` uses stable `key` and `contentType` values for every section item.
- Data-driven horizontal lists use stable model-ID item keys (`FlashDrop.id`, `Event.id`, `ExploreVenue.id`, `CircleStory.id`).
- Expensive derived values are wrapped in `remember` / `remember(key)` (e.g. venue match scores, time-of-day hero content).
- `components/DiscoverImagePrefetcher.kt` warms Coil's memory/disk cache for the hero and first-viewport imagery (static cards + first venues/events/stories), so cards paint synchronously instead of popping in.
- Business logic stays in repository actions; composables only dispatch and render.

## Analytics standards

`DiscoverAnalytics` is the single facade. Current coverage:

- `discover_section_viewed` — emitted by every `SectionHeader`;
- `discover_see_all_clicked` — emitted by every `SectionHeader` action and the Explore City inline action;
- `discover_card_opened` — Flash Drops, Events, My Circle stories, Smart Places, Explore City venues, Channels, Prep Rooms;
- `discover_action_clicked` — Closing Soon claim entry;
- `discover_overlay_opened` / `discover_overlay_dismissed` — emitted by `DiscoverOverlayHost`'s `DiscoverOverlay` wrapper for every full-screen overlay.

Production builds can bridge the facade to Firebase Analytics, Segment or a backend endpoint from one object.

## Accessibility standards

- `SectionHeader` actions expose `contentDescription` + `Role.Button`.
- Interactive cards (Flash Drops, Events, Smart Places, Explore City, Channels, Prep Rooms, story circles) expose merged `contentDescription`s and button roles.
- Static showcase cards (Closing Soon, Live Moments, Trending) expose TalkBack descriptions.
- State cards (`DiscoverSectionStateCard`) announce title and message.

## Testing

`DiscoverArchitectureTest` enforces:

- Discover shell line-count ceiling;
- required module files (including the Tonight hub overlay and plan dialogs);
- removal of the legacy `DiscoverSections.kt` monolith;
- removal of deprecated quick banners from `DiscoverScreen.kt`;
- stable keys + content types in the shell;
- `BackHandler` presence in every full-screen overlay source;
- offline/empty state support in data-driven sections;
- `@Composable` root declarations in every section module;
- the shared `SectionHeader` pattern in carousel sections.

`DiscoverAnalyticsTest` smoke-tests the analytics facade contract.

`DiscoverSectionStatesTest` (Robolectric Compose) verifies the section state contract at the UI level: offline vs empty vs populated rendering for Flash Drops, Events, Smart Places and My Circle, plus the standard `SectionHeader` title/subtitle/`See all →` rendering.

`ExploreTheCityTest` (Robolectric Compose + Roborazzi config) covers the Explore The City section and venue preview overlay interaction flow.

`DiscoverOverlayStateTest` unit-tests `DiscoverOverlayState` directly (no Compose runtime needed beyond `Snapshot`): every `openX()`/`dismissX()` pair, mutual independence between overlay slots, and `dismissAll()`.

`DiscoverSeeAllRoutingTest` (Robolectric Compose) is the behavioural counterpart to `DiscoverArchitectureTest`: it drives every section's "See all" (or equivalent primary) affordance against a real `DiscoverOverlayState` / navigation callback and asserts the *correct* destination opens — Closing Soon → Smart Places hub, Flash Drops → Flash Drops hub, My Circle → My Circle hub, Live Moments → My Circle hub (activity feed), Smart Places → Smart Places hub, Trending Now → Explore The City hub, Events → `onNavigateToEvents`, Explore The City → Explore The City hub, Channels → Channels overlay open flag, Prep Rooms → Prep Rooms overlay open flag, Tonight → `onNavigateToPlansWorkspace`.
