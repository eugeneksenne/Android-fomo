# FOMO Progress

## Current Focus
Discover screen refactor

## Completed So Far
- Split the shared Discover UI into `DiscoverSections.kt`.
- Reduced `DiscoverScreen.kt` to a route shell and overlay orchestration.
- Preserved the Discover section order from the current implementation.
- Preserved the section-level `See all` callbacks for Discover modules.
- Kept the hero section at the top of the Discover layout.
- Kept the Discover overlays and preview flows wired to the existing navigation callbacks.

## Discover Refactor Checklist
- [x] Extract shared Discover section composables into a dedicated file.
- [x] Reduce `DiscoverScreen.kt` to the route shell and overlay orchestration.
- [x] Preserve the existing Discover section order.
- [x] Preserve section-level `See all` callbacks.
- [x] Keep hero content at the top of Discover.
- [x] Keep Discover overlay flows wired to the existing callbacks.
- [ ] Split the remaining large Discover section families into dedicated files.
- [ ] Split overlay dialogs and detail views into dedicated files.
- [ ] Review the refactor for symbol collisions, import cleanup, and behavioural parity.
- [ ] Verify the screen still composes and navigates correctly after extraction.

## Notes
- The Discover refactor is still in progress.
- Remaining work includes further extraction of the larger section families and overlays into dedicated files.
- A review pass is still needed after the remaining extractions are complete.
