# HelioFlux Chart Readability Polish Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-helioflux-chart-readability-polish-design.md  
**Status:** Ready for review

## Overview

Polish the existing shared Android chart presentation without changing chart data semantics. Space Weather line charts will grow from 180dp to 240dp, the 2-day X-axis cadence will be reduced from 6-hour to 12-hour major positions, shared gridlines will become quieter, and the press/drag marker text will become slightly more compact. Existing timeframe domains, UTC formatting, Y-domain behavior, reduction, reference-line strength, semantic series colors, disabled zoom/pan, and marker interaction remain unchanged. No Aurora globe or Filament files are modified.

## Tasks

### Task 1: Lock the 2-day axis cadence with a failing unit test

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**

Update `xStepMatchesApprovedWindowBands()` so the 48-hour expectation is 12 hours rather than 6 hours:

```kotlin
@Test
fun xStepMatchesApprovedWindowBands() {
    assertEquals(15L * 60L * 1_000L, chartXStepMillis(ChartDomain(0.0, hour.toDouble())))
    assertEquals(30L * 60L * 1_000L, chartXStepMillis(ChartDomain(0.0, (3L * hour).toDouble())))
    assertEquals(2L * hour, chartXStepMillis(ChartDomain(0.0, (12L * hour).toDouble())))
    assertEquals(12L * hour, chartXStepMillis(ChartDomain(0.0, (48L * hour).toDouble())))
    assertEquals(12L * hour, chartXStepMillis(ChartDomain(0.0, (72L * hour).toDouble())))
}
```

Do not change the existing domain or UTC-label tests.

**Implementation:** None in this task. This task deliberately establishes the red TDD state.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. Confirm `xStepMatchesApprovedWindowBands` fails because the current 48-hour result is 6 hours.

---

### Task 2: Implement the approved 2-day X-axis cadence

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`, `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:** Use the failing test from Task 1; add no additional behavior.

**Implementation:**

Change only the 48-hour branch in `chartXStepMillis(domain)` so a 48-hour domain returns `12L * HourMillis` instead of `6L * HourMillis`. Keep the 1h, 3h, 12h, and >48h behavior unchanged.

This intentionally reduces the 2-day major X positions from roughly nine to roughly five and leaves `formatUtcAxisLabel` unchanged.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. All tests in that class pass.

---

### Task 3: Expose the approved Space Weather chart height as a testable presentation constant

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`, `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`

**Test first:**

Add a unit test that fixes the approved chart height at 240dp:

```kotlin
@Test
fun spaceWeatherChartHeightMatchesApprovedPixelPresentation() {
    assertEquals(240.dp, SpaceWeatherChartHeight)
}
```

Add the required `androidx.compose.ui.unit.dp` import to the test.

**Implementation:**

In `SpaceWeatherCharts.kt`, define a single presentation constant near the chart metadata:

```kotlin
val SpaceWeatherChartHeight = 240.dp
```

Do not introduce a new chart-style/configuration abstraction.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest`. Confirm the new test passes after the constant is added.

---

### Task 4: Apply the 240dp height to Space Weather line charts

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`, `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`

**Test first:** The presentation constant test from Task 3 protects the approved dimension. Do not add a brittle Compose screenshot/pixel test.

**Implementation:**

In `SpaceWeatherLineCard`, replace:

```kotlin
modifier = Modifier.fillMaxWidth().height(180.dp)
```

with:

```kotlin
modifier = Modifier.fillMaxWidth().height(SpaceWeatherChartHeight)
```

Do not change card padding, title/legend hierarchy, chart data, reference lines, or Solar Activity chart heights.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest`. All tests pass. Inspect the diff and confirm the only Space Weather production presentation change is the line-chart height.

---

### Task 5: Make shared gridlines quieter and the inspection marker more compact

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`

**Test first:**

No pixel-perfect unit test is added for alpha or font size; the approved design explicitly avoids brittle visual assertions. Before editing production code, run the existing shared chart tests to establish the behavioral baseline:

```bash
./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest
```

They must be green after Task 2.

**Implementation:**

Make exactly these shared presentation changes in `HelioFluxLineChart`:

1. Change the ordinary axis guideline alpha from `0.18f` to `0.12f`.
2. Change the marker label font size from `11.sp` to `10.sp`.
3. Leave reference-line alpha at `0.55f`.
4. Leave the marker guideline alpha at `0.5f`.
5. Leave series colors, marker contents, UTC timestamp/value formatting, marker controller, zoom/scroll policy, range provider, reduction, and domain handling unchanged.

This preserves the approved visual hierarchy: data series first, reference lines second, gridlines third.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. All tests pass. Inspect the diff to confirm there are no unrelated shared-chart changes.

---

### Task 6: Run focused chart regression tests as one atomic implementation batch

**Files:** No new files.

**Test first:** No new test is required; this task verifies all tests written in Tasks 1–5 together.

**Implementation:** None.

**Verify:** Run:

```bash
./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest --tests ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest
```

Confirm both test classes pass. Also confirm the existing tests still prove exact timeframe domains, sparse/gapped-series behavior, reduction behavior, reference-value Y-domain inclusion, UTC axis formatting, disabled chart navigation, and marker rows using only real plotted values.

---

### Task 7: Run the full local verification suite and audit scope

**Files:** No new files.

**Test first:** No new test; this is the pre-push verification gate.

**Implementation:** None.

**Verify:**

Run the repository's normal Android verification command used by CI. If the workflow invokes multiple Gradle tasks, run the same relevant tasks locally before pushing.

Then inspect `git diff --stat` and `git diff`. The implementation batch must be limited to:

- `app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`

Confirm no Aurora globe, SceneView, Filament, repository, ViewModel, persistence, or networking files changed.

---

### Task 8: Push once and actively monitor Android CI

**Files:** No new files.

**Test first:** All local verification from Tasks 6–7 must already be green.

**Implementation:**

Commit the complete chart-readability implementation as one atomic code/test batch to minimize GitHub Actions usage, then push it.

Do not split the height, cadence, and shared presentation tweaks into separate pushed commits because they are one approved chart-presentation subsystem change.

**Verify:**

Monitor the resulting Android CI run during the same work session. Re-check approximately every 45 seconds while it is pending. If CI fails, inspect the failing job/logs, make the minimum correction, rerun the relevant local test(s), push the correction, and continue monitoring until CI is green or a problem genuinely requires user input.

---

### Task 9: Physical Pixel acceptance check

**Files:** No code changes unless physical testing reveals a separate defect that is routed through the appropriate workflow.

**Test first:** CI must be green before device acceptance.

**Implementation:** None.

**Verify:**

On the physical Pixel, inspect Space Weather at `1h`, `3h`, `12h`, and `2d` and confirm:

- the 240dp plots provide noticeably more vertical breathing room;
- axis labels remain readable;
- 2d labels/gridlines are less crowded;
- data series visually dominate ordinary gridlines;
- Bz zero/reference guides remain visible but secondary to data;
- press/drag inspection remains readable and less obtrusive;
- changing timeframe still refits to the selected explicit domain;
- a partially empty 2d domain remains empty when local history is unavailable, without warnings or synthetic/stretch data;
- zoom/pan remains disabled.

Spot-check Solar Activity X-Ray and ACE EPAM to ensure the shared grid/marker refinements remain visually appropriate and their existing 260dp/240dp heights are unchanged.

No Aurora globe/Filament retest is required beyond confirming this batch did not modify those files.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New/changed behavior is covered by focused tests where it is meaningfully testable.
- [ ] Focused chart tests pass.
- [ ] Full repository Android verification passes.
- [ ] Only planned chart files are modified.
- [ ] Space Weather line charts use 240dp plot height.
- [ ] 2d uses a 12-hour X-axis step while existing timeframe/domain semantics remain unchanged.
- [ ] Shared gridline alpha is 0.12 and reference-line alpha remains 0.55.
- [ ] Inspection marker text is 10sp with unchanged content and interaction.
- [ ] Filtering, reduction, domain fitting, series semantics, and zoom/pan behavior are unchanged.
- [ ] Aurora globe and Filament files are untouched.
- [ ] One atomic implementation batch is pushed and Android CI is monitored to green.
- [ ] Physical Pixel acceptance check passes at 1h / 3h / 12h / 2d.
