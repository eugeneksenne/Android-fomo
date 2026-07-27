# Discover Architecture

The Discover feature is now organized as a thin orchestration shell plus feature modules. All files remain in the `com.example.feature.discover` package so existing navigation and Compose tests can continue to reference public composables without migration churn.

## Structure

```text
feature/discover/
  DiscoverScreen.kt              # Orchestration shell: state collection, section order, overlay state
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
    FlashDropsOverlay.kt
    SmartPlacesOverlay.kt
  dialogs/
    FlashDropClaimDialog.kt
    FlashDropRouteDialog.kt
    SmartPlaceRouteDialog.kt
    GlobalPlanContextSheet.kt
```

## Shell responsibilities

`DiscoverScreen.kt` should remain below 360 lines and only handle:

- collecting repository state;
- rendering the 12-section ordering;
- maintaining overlay/dialog selection state;
- dispatching navigation callbacks;
- emitting high-level analytics hooks.

Business rules, card rendering, overlay UI and reusable loading/empty/error/offline states belong in feature modules.

## Section standard

Each section should use the shared `SectionHeader` pattern:

```text
Title                         See all →
Subtitle
```

`SectionHeader` emits `discover_section_viewed` and `discover_see_all_clicked` events through `DiscoverAnalytics`.

## Performance standards

- Main Discover `LazyColumn` uses stable `key` and `contentType` values for every section.
- Horizontal lists use stable item keys where model IDs are available.
- Expensive derived values should be wrapped in `remember` or `derivedStateOf`.
- Avoid local business logic in composables when repository actions can own the state transition.

## State standards

Sections backed by dynamic data should support:

- loading skeletons via `DiscoverLoadingSkeleton`;
- empty states via `DiscoverEmptyState`;
- error states via `DiscoverErrorState`;
- offline states via `DiscoverOfflineState`.

Current repositories mostly expose seeded `StateFlow<List<T>>` values, so full loading/error/offline wiring should be completed as repository states evolve from raw lists to richer state models.

## Testing

`DiscoverArchitectureTest` enforces:

- Discover shell line-count ceiling;
- required module files;
- removal of deprecated quick banners from `DiscoverScreen.kt`.
