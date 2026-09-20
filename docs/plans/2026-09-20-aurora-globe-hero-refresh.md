# Aurora Globe Hero Refresh Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-aurora-globe-hero-refresh-design.md  
**Status:** Ready for execution

## Overview
Refresh only the Aurora hero presentation on Space Weather: replace the fixed-height Material card with a square black stage and move the initial SceneView camera closer so the globe nearly fills the available area. Preserve all existing interaction, rendering, and lifecycle behavior.

## Tasks

### Task 1: Lock the hero visual contract with failing tests
**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`, `feature/globe/src/test/java/ca/stewark/helioflux/feature/globe/EarthSceneControlWiringTest.kt`

**Test first:**
- Add `auroraHeroUsesBlackSquareStage()` that isolates the `AuroraHero` source branch and asserts it contains `.fillMaxWidth().aspectRatio(1f)`, `.background(Color.Black)`, and `.clip(RoundedCornerShape(16.dp))`, and does not contain `.height(340.dp)`.
- Add `initialCameraFramingMaximizesGlobeInHero()` that asserts `EarthScene.kt` contains `GLOBE_CAMERA_DISTANCE = 3.0f`.

**Implementation:** None in this task. Push the failing tests before production changes.

**Verify:** Android CI must fail specifically because the current hero still uses `340.dp`/Material Card styling and the current camera distance is `4.25f`.

### Task 2: Replace the Aurora card with the black square hero stage
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`

**Test first:** Use the failing `auroraHeroUsesBlackSquareStage()` regression from Task 1.

**Implementation:**
- Add the Compose `background` and `Color` imports needed for the stage.
- Replace only the `SpaceWeatherBlock.AuroraHero` Material `Card` with a `Box`.
- Use `Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black).testTag("aurora-globe-slot")`.
- Keep the inner globe at `Modifier.fillMaxSize()`.
- Keep the freshness indicator aligned `TopEnd` with the existing 10 dp padding.
- Do not modify the globe touch callback or the LazyColumn touch-ownership logic.

**Verify:** `auroraHeroUsesBlackSquareStage()` passes.

### Task 3: Increase initial globe size
**Files:** `feature/globe/src/main/java/ca/stewark/helioflux/feature/globe/EarthScene.kt`

**Test first:** Use the failing `initialCameraFramingMaximizesGlobeInHero()` regression from Task 1.

**Implementation:**
- Change only `GLOBE_CAMERA_DISTANCE` from `4.25f` to `3.0f`.
- Leave `rememberCameraManipulator`, touch callbacks, frame-loop rotation, node hierarchy, material creation, and resource lifecycle untouched.

**Verify:** `initialCameraFramingMaximizesGlobeInHero()` and all existing `EarthSceneControlWiringTest` tests pass.

### Task 4: Full verification
**Files:** No additional production files.

**Test first:** No new tests; this task validates the full regression suite.

**Implementation:** None.

**Verify:** Run the full Android CI workflow. Confirm unit tests, instrumentation compilation/tests, lint, and debug APK build are green. Then physically verify on the Pixel that the globe nearly fills the black square without visibly clipping the atmosphere and that continuous drag, pinch zoom, and idle auto-rotation remain unchanged.

## Definition of Done
- [ ] All tasks completed in order
- [ ] All tests pass
- [ ] No unplanned files modified
- [ ] Feature behaves as described in the design doc
- [ ] Pixel verification confirms visual size and preserves globe interaction
