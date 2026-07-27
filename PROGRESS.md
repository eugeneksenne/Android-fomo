# FOMO Progress

## Current Focus
Discover screen — production hardening (module split complete, overlay-state consolidation complete)

## Completed So Far
- Split the shared Discover UI out of the legacy `DiscoverSections.kt` monolith and **removed the monolith**.
- Reduced `DiscoverScreen.kt` to a route shell and overlay orchestration (~170 lines, ceiling 220).
- Extracted overlay/dialog selection state into `DiscoverOverlayState` (single source of truth, replacing a dozen independent `mutableStateOf` flags) and overlay rendering into `DiscoverOverlayHost`.
- Preserved the Discover section order from the current implementation.
- Preserved the section-level `See all` callbacks for Discover modules.
- Kept the hero section at the top of the Discover layout.
- Kept the Discover overlays and preview flows wired to the existing navigation callbacks.
- Added `DiscoverSeeAllRoutingTest` verifying every section's "See all" affordance routes to the correct destination.
- Added `DiscoverOverlayStateTest` unit-testing `DiscoverOverlayState`'s open/dismiss/dismissAll contract and overlay-slot independence.

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

## Remaining / Deferred
- Repository evolution from seeded `StateFlow<List<T>>` to richer states so sections can exercise `DiscoverLoadingSkeleton` / `DiscoverErrorState` in production paths.
- Shared element transitions between cards and venue/event detail surfaces (Compose `SharedTransitionLayout`; deferred — high-risk without an emulator build).
- Compose UI screenshot (Roborazzi) recordings for the Discover shell.
- Trimming the copy-pasted import blocks in extracted modules (warnings only; deferred to avoid behavioural risk without an emulator build).

## Notes
- The Discover refactor is complete; remaining work is hardening and richer repository states.
- A review pass after the extractions is complete: static verification passed (duplicates, annotations, balance, signatures).
