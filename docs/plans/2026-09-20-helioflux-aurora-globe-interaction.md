# HelioFlux Aurora Globe Interaction Implementation Plan

**Date:** 2026-09-20
**Design:** docs/specs/2026-09-20-helioflux-aurora-globe-interaction-design.md

## Task 1 — Lock gesture intent with tests

Files:
- Create feature/globe/src/main/java/ca/stewark/helioflux/feature/globe/GlobeGestureIntent.kt
- Create feature/globe/src/test/java/ca/stewark/helioflux/feature/globe/GlobeGestureIntentTest.kt

TDD:
1. Add failing tests for undecided movement below touch slop, horizontal globe ownership, vertical parent-scroll ownership, and multi-touch globe ownership.
2. Implement the smallest pure gesture-intent classifier that makes those tests pass.
3. Do not touch rendering or Filament resources.

## Task 2 — Integrate arbitration at the globe boundary

Files:
- Modify feature/globe/src/main/java/ca/stewark/helioflux/feature/globe/EarthScene.kt
- Update focused tests only if integration exposes a stable test seam.

TDD:
1. Route one-finger SceneView movement through the classifier before mutating GlobeInteractionState.
2. Begin/pause globe interaction only when the gesture becomes globe-owned.
3. Let vertical one-finger gestures remain unclaimed for parent LazyColumn scrolling.
4. Preserve two-finger scaling, yaw/pitch behavior, camera-distance clamps, idle animation loop, and delayed resume.
5. Keep SceneView construction, Earth/Aurora/atmosphere creation, materials, textures, and lifecycle code unchanged.

## Task 3 — Regression verification and delivery

Run the globe unit tests and relevant app/globe test suites. Verify no unrelated files changed. Push the smallest sensible implementation batch. Monitor Android CI until green; if CI fails, inspect and make only the minimum focused correction.

Then hand back for physical Pixel validation:
- idle slow rotation
- horizontal and diagonal drag
- pinch/spread zoom
- vertical page scroll beginning over the globe
- auto-rotation resume after interaction
- navigate away/back and scroll repeatedly without a Filament crash
