# Chart Series Line Width Implementation Plan

**Date:** 2026-09-22  
**Design doc:** `docs/specs/2026-09-22-chart-series-line-width-design.md`  
**Status:** Ready for review

## Goal

Set every plotted data-series line rendered by `HelioFluxLineChart` to **1 dp** while leaving reference lines, gridlines, axes, markers, chart behavior, and chart-specific calculations unchanged.

This is intentionally a shared-renderer-only change so Solar Activity and Space Weather inherit the same styling automatically.

## Baseline

Current `main`:

`173295cb9c24f8ac66bf3cf8a4362b705473a3c2`

Latest Android CI:

- Android CI #295
- Run ID `35768123763`
- GREEN

The docs-only design commit did not trigger a new Android CI run.

## Tasks

### Task 1: Add focused regression coverage for the shared 1 dp series width

**Files:**
- `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**

Extend `ChartPresentationTest.kt` with a direct constant assertion:

```kotlin
@Test
fun chartSeriesStrokeWidthIsOneDp() {
    assertEquals(1.dp, ChartSeriesStrokeWidth)
}
```

Add the required import for `androidx.compose.ui.unit.dp`.

Also add a source-policy regression that reads:

`src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`

and asserts the shared renderer explicitly applies the continuous Vico stroke using the shared constant:

```kotlin
@Test
fun sharedRendererAppliesConfiguredSeriesStrokeWidth() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt")
            .readText()

    assertTrue(source.contains("LineStroke.Continuous("))
    assertTrue(source.contains("thickness = ChartSeriesStrokeWidth"))
}
```

Keep the existing chart interaction, reference-line, marker, formatting, reduction, and color tests unchanged.

Run the focused test before production changes and confirm it fails because `ChartSeriesStrokeWidth` and the custom stroke are not yet defined.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```

Expected before implementation: failing new assertions.

---

### Task 2: Set the shared plotted-series stroke to 1 dp

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Implementation:**

In `HelioFluxLineChart.kt`:

1. Define one shared presentation constant near the other chart constants:

```kotlin
internal val ChartSeriesStrokeWidth = 1.dp
```

2. In the existing `renderSegments.map` block, keep the current semantic fill/color logic unchanged.
3. Add Vico 3.1.0's supported continuous stroke to each plotted series:

```kotlin
LineCartesianLayer.rememberLine(
    fill =
        LineCartesianLayer.LineFill.single(
            Fill(seriesColor(segment.series.style)),
        ),
    stroke =
        LineCartesianLayer.LineStroke.Continuous(
            thickness = ChartSeriesStrokeWidth,
        ),
)
```

Do not change:

- `seriesColor`;
- `ChartReferenceLine` rendering;
- axis guideline components;
- marker guideline;
- chart domain/range behavior;
- min/max reduction;
- scroll/zoom policy;
- `animateInitial`;
- chart heights;
- any chart-specific files.

No changes should be needed in:

- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/XrayChart.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/AceEpamChart.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/HemisphericPowerChart.kt`

because all already render through `HelioFluxLineChart`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```

The new 1 dp stroke tests and all existing shared-chart tests pass.

---

### Task 3: Run chart regression coverage and full local verification

**Files:** No additional files expected.

**Test first:**

Run focused chart coverage:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.AceEpamChartTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.*ChartTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.*"
```

If a focused test fails, make only the minimum correction consistent with the approved design.

**Full verification:**

Run the same normal Android verification sequence:

```bash
./gradlew projects
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

If an Android device is connected:

```bash
./gradlew connectedDebugAndroidTest
```

Inspect the final diff and confirm:

- only `HelioFluxLineChart.kt` and `ChartPresentationTest.kt` changed for implementation;
- `ChartSeriesStrokeWidth` is exactly `1.dp`;
- only plotted data-series lines use the new width;
- reference lines are unchanged;
- gridlines/axes are unchanged;
- semantic series colors are unchanged;
- `AceEpamChart` still has `animateInitial = false`;
- no chart-specific stroke overrides were added;
- no dependency versions changed.

---

### Task 4: Commit once, push, and monitor Android CI

**Files:** Only the two implementation/test files from Tasks 1-3.

**Implementation:**

To minimize GitHub Actions usage:

1. perform TDD work on an isolated branch;
2. keep test-first commits there if useful;
3. squash into one focused implementation commit on `main`;
4. push once.

Suggested commit message:

`style: thin shared chart series lines`

Monitor the triggered Android CI run during the implementation response.

If CI fails:

1. inspect the failing step and logs;
2. reproduce or identify the issue;
3. make the minimum correction;
4. push the correction;
5. continue monitoring until green.

**Verify:**

Android CI completes with `conclusion: success`.

Then physically verify on the Pixel:

- Solar Activity X-Ray lines are visibly thinner;
- Particle Environment lines are visibly thinner;
- Space Weather Bz/Bt and plasma chart lines are visibly thinner;
- Hemispheric Power lines are visibly thinner;
- reference/grid lines retain their current appearance;
- colors remain unchanged;
- chart inspection still works;
- Particle Environment scrolling remains smooth and does not replay its reveal animation.

## Definition of Done

- [ ] Shared plotted-series width is exactly 1 dp.
- [ ] All charts using `HelioFluxLineChart` inherit the width automatically.
- [ ] No per-chart width overrides are added.
- [ ] Reference-line styling is unchanged.
- [ ] Gridline/axis styling is unchanged.
- [ ] Semantic series colors are unchanged.
- [ ] Marker/inspection behavior is unchanged.
- [ ] Data/domain/reduction behavior is unchanged.
- [ ] Particle Environment `animateInitial = false` behavior remains intact.
- [ ] New regression tests pass.
- [ ] Existing chart tests pass.
- [ ] Full Android verification passes.
- [ ] No unrelated files are modified.
- [ ] Android CI is green.
- [ ] Pixel verification confirms the thinner lines remain readable.
