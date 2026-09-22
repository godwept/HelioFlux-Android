# HMI Active-Region Label Collision Avoidance Implementation Plan

**Date:** 2026-09-22  
**Design doc:** `docs/specs/2026-09-22-hmi-active-region-label-collision-avoidance-design.md`  
**Status:** Ready for review

## Overview

Add deterministic screen-space collision avoidance to the shared HMI `ActiveRegionOverlay`. A small pure layout helper will keep each NOAA label anchored to its true helioprojective location, move crowded labels only as far as necessary within the approved 48 dp cap, maintain a 10 dp minimum gap when possible, keep labels inside the magnetogram stage, and request a subtle leader line once displacement exceeds 16 dp. The existing HEK data path, `mapActiveRegion()` coordinate mapping, HMIBC imagery, normal/fullscreen reuse of `ActiveRegionOverlay`, and zoom/pan behavior remain unchanged.

## Tasks

### Task 1: Add the pure label-layout contract and preserve non-colliding placement

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayout.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayoutTest.kt`

**Test first:**

Create `ActiveRegionLabelLayoutTest.kt` and add a test for two comfortably separated labels. Use pixel values directly so the helper remains density-independent.

The test should construct two inputs with stable keys, true anchor points, and measured label sizes, call the resolver with:

- stage: 400 × 400 px;
- minimum gap: 10 px;
- maximum displacement: 48 px;
- leader threshold: 16 px.

Assert:

- both labels are returned;
- each resolved label center remains at its true anchor when there is no collision;
- neither placement requires a leader line.

Also keep the existing `SolarActivityComponentsTest.magnetogramCoordinatesMatchPwaMapping` unchanged; do not move coordinate mapping into the new helper.

Run the focused test and confirm it fails because the helper does not exist.

**Implementation:**

Create `ActiveRegionLabelLayout.kt` with small internal data classes:

```kotlin
internal data class ActiveRegionLabelInput(
    val key: String,
    val anchor: Offset,
    val size: IntSize,
)

internal data class ActiveRegionLabelPlacement(
    val key: String,
    val anchor: Offset,
    val topLeft: Offset,
    val size: IntSize,
    val leaderEnd: Offset?,
)
```

Add:

```kotlin
internal fun resolveActiveRegionLabels(
    labels: List<ActiveRegionLabelInput>,
    stageSize: IntSize,
    minGapPx: Float,
    maxDisplacementPx: Float,
    leaderThresholdPx: Float,
): List<ActiveRegionLabelPlacement>
```

For this first task:

- sort inputs by `key` before placement so output is deterministic;
- define the zero-displacement position as the label rectangle centered on the true anchor;
- return that centered position when it is in bounds and does not conflict with an already placed label;
- leave collision search and leader-line geometry for the following tasks.

Do not import HEK/domain types into the pure helper.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*ActiveRegionLabelLayoutTest"
```

The non-colliding placement test passes.

---

### Task 2: Enforce the 10 px minimum gap with deterministic nearby candidates

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayout.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayoutTest.kt`

**Test first:**

Add two tests:

1. `closeLabelsMaintainMinimumGap`
   - place two 36 × 16 px labels whose anchors are close enough that centered rectangles overlap;
   - assert their final rectangles are separated by at least `minGapPx` on one axis and do not overlap after applying the required gap.

2. `placementIsDeterministicForSameLabels`
   - call the resolver once with inputs in A/B/C order and once in C/A/B order;
   - compare placements by `key`;
   - assert identical top-left coordinates and leader decisions.

Run the focused test and confirm the collision test fails before implementation.

**Implementation:**

Extend the helper with deterministic candidate generation.

Candidate rules:

1. try zero displacement first;
2. for increasing radial offsets, test directions in this exact order:
   - above;
   - below;
   - left;
   - right;
   - upper-left;
   - upper-right;
   - lower-left;
   - lower-right;
3. increase radial offset in `minGapPx` steps, with a final ring exactly at `maxDisplacementPx` when the step does not land on it;
4. compute candidate label top-left from the candidate label center:
   `candidateCenter - labelSize / 2`;
5. select the first in-bounds candidate that has no conflict with previously placed labels.

Implement label conflicts by treating the approved minimum gap as padding around the rectangles. Keep this math pure and independent of Compose density.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*ActiveRegionLabelLayoutTest"
```

Both new tests pass.

---

### Task 3: Keep labels in bounds and cap displacement at 48 px

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayout.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayoutTest.kt`

**Test first:**

Add three focused tests:

1. `edgeAnchorKeepsWholeLabelInsideStage`
   - put an anchor close to the top-left edge;
   - assert final bounds have `left >= 0`, `top >= 0`, `right <= stage.width`, and `bottom <= stage.height`.

2. `placementNeverExceedsMaximumDisplacement`
   - create a crowded cluster;
   - compute distance between each true anchor and resolved label center;
   - assert it is no greater than `maxDisplacementPx` (allow only a tiny float tolerance).

3. `unavoidableClusterUsesLeastOverlapCandidateWithoutDroppingLabels`
   - use a deliberately tight stage/cluster where full separation is impossible inside the cap;
   - assert every input still has a placement;
   - assert each placement remains in bounds;
   - assert repeated calls return the same fallback placement.

Run the focused tests and confirm the fallback/bounds cases fail before implementation.

**Implementation:**

Extend the resolver so:

- candidates whose complete label bounds fall outside the stage are rejected;
- all candidate centers remain within the approved maximum displacement from the true anchor;
- while generating candidates, retain their total overlap score against previously placed labels using the same minimum-gap-expanded rectangles;
- if no zero-overlap candidate exists within the cap, choose the in-bounds candidate with the lowest total overlap area;
- break equal scores by:
  1. smaller displacement;
  2. earlier direction/ring order.

Do not hide or drop labels.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*ActiveRegionLabelLayoutTest"
```

All bounds, cap, and fallback tests pass.

---

### Task 4: Add the 16 px leader-line decision and edge endpoint

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayout.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayoutTest.kt`

**Test first:**

Add tests that explicitly cover the threshold:

1. a resolved displacement of exactly 16 px produces `leaderEnd == null`;
2. a resolved displacement greater than 16 px produces a non-null `leaderEnd`;
3. when a leader is present:
   - the line starts from `placement.anchor` by contract;
   - `leaderEnd` lies on the resolved label rectangle boundary rather than at its center.

Run the tests and confirm the leader-line cases fail before implementation.

**Implementation:**

After selecting each final candidate:

- compute actual displacement using distance from true anchor to resolved label center;
- set `leaderEnd = null` when displacement is `<= leaderThresholdPx`;
- when displacement is greater, compute the point where a ray from the anchor toward the label center first meets the label rectangle boundary;
- store that edge point as `leaderEnd`.

Keep leader-line geometry in the pure helper so Canvas rendering remains trivial and testable.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*ActiveRegionLabelLayoutTest"
```

All leader-line threshold and endpoint tests pass.

---

### Task 5: Measure real label text and use the resolver in ActiveRegionOverlay

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/MagnetogramCard.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`

**Test first:**

Do not change the existing `magnetogramCoordinatesMatchPwaMapping` assertions.

Add a small source-level/unit contract if necessary to verify the approved dp values remain centralized rather than duplicated:

- minimum gap: 10 dp;
- maximum displacement: 48 dp;
- leader threshold: 16 dp.

Prefer exposing internal constants from `MagnetogramCard.kt` only if needed by the test; do not create a settings/config abstraction.

Run the focused Solar Activity unit tests before changing the overlay.

**Implementation:**

Update only `ActiveRegionOverlay` in `MagnetogramCard.kt`.

Use:

- `rememberTextMeasurer()` to measure the actual rendered region-number string;
- the existing local/default `Text` style for both measurement and rendering so the measured bounds match the visible label;
- `LocalDensity.current` to convert the approved 10/48/16 dp values to pixels;
- the existing `mapActiveRegion()` result to derive the true anchor in overlay-local pixels.

For a non-zero overlay size:

1. sort/identify each region by stable key `region.id`;
2. measure `region.number ?: region.id`;
3. build `ActiveRegionLabelInput` values;
4. call `resolveActiveRegionLabels(...)`;
5. position each `Text` with the returned top-left.

If the overlay size is still zero, do not run collision resolution.

Do not modify `mapActiveRegion()`, HEK data, HMI image loading, or the parent `SolarImageryCard`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*SolarActivityComponentsTest" --tests "*ActiveRegionLabelLayoutTest"
```

The mapping test remains unchanged and all layout tests stay green.

---

### Task 6: Render subtle leader lines behind displaced labels

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/MagnetogramCard.kt`

**Test first:**

The pure leader-line decision and endpoint are already covered in Task 4. Before rendering, rerun that focused test to establish the green contract.

No screenshot/golden framework should be introduced solely for a one-line Canvas rendering detail.

**Implementation:**

Inside the shared `ActiveRegionOverlay`:

- add a full-size `Canvas` behind the label `Text` nodes;
- for placements whose `leaderEnd` is non-null, call `drawLine`:
  - start = exact `placement.anchor`;
  - end = `placement.leaderEnd`;
  - use the existing muted/light theme treatment (for example `SpaceMuted` with reduced alpha);
  - use approximately 1 dp stroke width;
- render the text after the Canvas so labels remain visually dominant.

Do not draw lines for labels displaced at or below the 16 dp threshold.

Because both normal and fullscreen HMI already call the same `ActiveRegionOverlay`, do not add a second fullscreen implementation.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "*ActiveRegionLabelLayoutTest"
```

The pure rendering contract remains green.

---

### Task 7: Prove labels still render through the shared normal/fullscreen overlay

**Files:**  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Extend the existing populated imagery fixture to include at least two nearby active regions with distinct labels, for example `4532` and `4533`.

Add/extend Compose assertions so:

1. on the normal HMI card, both region-number texts exist;
2. after opening HMI fullscreen, the existing `fullscreen-active-region-overlay` exists and both labels still exist;
3. closing fullscreen still returns to the HMI stage.

Do not assert exact device-pixel coordinates in the instrumentation test; the pure resolver tests own placement geometry.

Run the instrumentation test target and confirm any missing label integration assertion fails before finalizing the overlay wiring.

**Implementation:**

Make only the minimum testability/wiring correction exposed by the failing Compose test. Ideally no additional production change is required because `ActiveRegionOverlay` is already shared.

Do not modify `FullscreenImageryViewer.kt` or `SolarActivityScreen.kt` unless the failing test demonstrates the shared overlay path is broken.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

If a connected device is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The HMI UI test passes in both normal and fullscreen presentation.

---

### Task 8: Run focused and full local verification

**Files:** No planned production changes.

**Test first:**

No new test is created solely for this gate. If verification exposes a defect, add the narrowest failing regression before changing production code.

**Verify:**

Run the focused suite:

```bash
./gradlew :app:testDebugUnitTest   --tests "*ActiveRegionLabelLayoutTest"   --tests "*SolarActivityComponentsTest"
```

Then the repository-required Android gates:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Inspect scope:

```bash
git status --short
git diff --stat
```

Expected implementation files are limited to:

```text
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayout.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/MagnetogramCard.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/ActiveRegionLabelLayoutTest.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt
app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt
```

`FullscreenImageryViewer.kt`, `SolarActivityScreen.kt`, data repositories, network endpoints, HEK parsing, HMIBC/LaTiS code, and unrelated UI files should remain unchanged unless a test demonstrates a genuine requirement.

---

### Task 9: Commit one TDD batch, push once, and monitor Android CI

**Files:** No additional planned source changes.

**Test first:**

Preserve TDD history without triggering extra GitHub Actions runs:

1. create the failing test changes first;
2. commit those tests locally/on an unattached Git object if needed;
3. put the production implementation on top;
4. fast-forward/push `main` only once when the full batch is green.

Suggested commits:

```text
test: require HMI active-region label separation
fix: avoid HMI active-region label collisions
```

**Implementation / verification:**

Push the completed batch once to `main`.

Actively monitor the resulting Android CI run during the same response, re-checking roughly every 45 seconds while pending.

If CI fails:

1. inspect the failing step/log;
2. reproduce with the narrowest relevant test;
3. add/fix regression coverage first;
4. make the minimum correction;
5. push the correction;
6. continue monitoring until green.

**Verify:**

Android CI completes successfully for the final implementation SHA.

---

### Task 10: Perform the physical Pixel acceptance pass

**Files:** No planned source changes. Any new defect starts the project's bug workflow before production changes.

**Verify on the physical Pixel:**

1. Open Solar Activity with the HMI magnetogram selected.
2. Find a current frame with two or more closely spaced NOAA active regions.
3. Confirm labels no longer overlap/crowd when enough room exists.
4. Confirm every label still remains visually close to its corresponding sunspot.
5. Confirm a noticeably displaced label has a subtle leader line back to the exact active-region anchor.
6. Confirm labels near the solar-disk/stage edge stay fully visible.
7. Open HMI fullscreen and confirm the same spacing/leader-line behavior.
8. Pinch/pan fullscreen and confirm image plus labels/leader lines still move together as one transformed stage.
9. Confirm labels do not visibly jump while the screen is idle or during ordinary recomposition.
10. Confirm HMIBC loading, HEK region numbers, LASCO/ENLIL, and other Solar Activity behavior remain unchanged.

If a physical-device issue appears, route it through `$debugprompt` with a focused reproduction rather than broadening this task.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production change was preceded by focused failing coverage
- [ ] Existing `mapActiveRegion()` coordinate mapping is unchanged
- [ ] True active-region anchors are never moved
- [ ] Non-colliding labels stay at their anchor-centered default position
- [ ] Crowded labels attempt deterministic nearby placements in the approved direction order
- [ ] Resolved labels maintain approximately 10 dp minimum spacing when possible
- [ ] Labels stay inside the visible stage
- [ ] Label-center displacement never exceeds approximately 48 dp
- [ ] Impossible clusters use a deterministic least-overlap fallback rather than dropping labels
- [ ] Leader lines appear only when displacement exceeds approximately 16 dp
- [ ] Leader lines originate at the true anchor and terminate at the label edge
- [ ] Leader lines are visually subordinate to label text
- [ ] Normal and fullscreen HMI use the same `ActiveRegionOverlay`
- [ ] Fullscreen zoom/pan behavior is unchanged
- [ ] HEK acquisition/parsing is unchanged
- [ ] HMIBC/LaTiS image acquisition is unchanged
- [ ] No unrelated Solar Activity or data-source behavior is modified
- [ ] Focused unit tests pass
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:compileDebugAndroidTestKotlin` passes
- [ ] `./gradlew :app:lintDebug :app:assembleDebug` passes
- [ ] No unplanned files are modified
- [ ] Final Android CI is green
- [ ] Physical Pixel acceptance passes
