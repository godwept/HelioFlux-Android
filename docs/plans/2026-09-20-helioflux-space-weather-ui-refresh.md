# Space Weather UI Refresh Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-helioflux-space-weather-ui-refresh-design.md  
**Status:** Ready for review

## Overview

Reconstruct the native Android Space Weather screen around the approved PWA-inspired hierarchy while preserving its repositories, data flows, Aurora/Filament pipeline, and existing chart data semantics. The work is deliberately presentation-focused: make the Aurora globe the prominent hero, introduce Solar Wind and Geomagnetic Activity sections, replace metric cards with compact colored pills, replace independent timeframe chips with a joined segmented control, refine chart cards/legends/Kp severity presentation, improve compact/expanded composition, and change the default timeframe to 12h. Implementation must follow strict TDD and avoid modifying `godwept/HelioFlux-web`.

## Tasks

### Task 1: Make 12h the default timeframe

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherViewModelTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherViewModel.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/TimeframeSelector.kt`

**Test first:** Add a focused test that constructs `SpaceWeatherUiState()` and asserts `timeframe == Timeframe.TwelveHours`. Add a ViewModel test that creates `SpaceWeatherViewModel.forTest(...)`, yields once, and asserts the initial emitted timeframe is `TwelveHours` before any call to `selectTimeframe`.

**Implementation:** Change both default state locations from `Timeframe.ThreeHours` to `Timeframe.TwelveHours`: the `SpaceWeatherUiState.timeframe` default and `SpaceWeatherViewModel.selectedTimeframe`. Change `rememberTimeframeSelection`'s default initial value to `TwelveHours` so the helper remains consistent if used independently. Do not change `Timeframe` durations or filtering.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherViewModelTest"`. All tests pass.

---

### Task 2: Define compact metric-pill presentation

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetricsTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetrics.kt`

**Test first:** Keep the existing latest-value/null tests and add tests for a small presentation mapper that returns exactly three items in order: `Bz`, `Speed`, `Density`; verifies formatted values (`nT`, `km/s`, `p/cm³`); and verifies null values map to the existing em dash placeholder rather than zero.

**Implementation:** Replace the three weighted Material `Card` widgets with compact pill-style presentation. Introduce only a screen-local metric presentation type/helper needed to keep formatting testable. Render pills in a wrapping-capable layout so narrow widths do not squeeze values; retain the existing `solar-wind-metrics`, `metric-bz`, `metric-speed`, and `metric-density` tags. Use the approved PWA-inspired semantic colors for Bz, Speed, and Density, a subtle tinted/background treatment, rounded pill shape, and typography that gives the value more emphasis than the label. Do not change how latest values are selected.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SolarWindMetricsTest"`. All tests pass.

---

### Task 3: Replace timeframe chips with a joined segmented control

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/TimeframeSelectorTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/TimeframeSelector.kt`

**Test first:** Extend `TimeframeSelectorTest` to assert the stable four-option presentation order and labels `1h`, `3h`, `12h`, `2d`, and that `TwelveHours` is the helper's default selection contract. Keep the test independent of pixel dimensions.

**Implementation:** Replace the row of four `FilterChip` controls with one joined Material 3 segmented-control presentation using four equal-width segments. Preserve `Timeframe.entries`, `Timeframe.label`, `onSelected(Timeframe)`, `timeframe-selector`, and the per-option test tags. Apply the HelioFlux accent to the selected segment and restrained inactive surfaces; do not introduce new state beyond the selected `Timeframe` passed by the caller.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.TimeframeSelectorTest"`. All tests pass.

---

### Task 4: Add a consistent chart-card presentation model and header

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`

**Test first:** Add tests for chart metadata describing the five solar/near-Earth cards. Assert stable titles/context: IMF Bz/Bt + Magnetic Field, Density/Speed/Temperature + Solar Wind, and GOES Magnetometer + its dynamic satellite label context. Add assertions that multi-series cards expose Bz/Bt or primary/secondary legend labels without fabricating labels for missing series.

**Implementation:** Expand `SpaceWeatherLineCard` from a title-only generic card to a restrained defined chart card with a consistent header: small contextual eyebrow, measurement title/unit, optional series legend, then the chart. Keep the existing series/filter functions unchanged. Give the card a subtle border/background rather than prominent elevation, consistent internal spacing, and the existing 180dp chart region unless later layout work shows a direct need to adjust it. Pass metadata explicitly from the screen; do not move presentation labels into the ViewModel.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest"`. All tests pass.

---

### Task 5: Give line-chart series deliberate visual identities

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`

**Test first:** Add a pure mapping test that verifies the caller can provide one visual identity per data series and that reference lines remain separate from data-series identities. Preserve existing point/null mapping coverage.

**Implementation:** Extend `LineChartSeries` with only the presentation metadata needed by Vico to distinguish series consistently, then configure `HelioFluxLineChart` to render those identities rather than relying on Vico defaults. Assign PWA-inspired identities at the Space Weather call sites: Bz red/pink vs Bt light blue; Density orange; Speed green; Temperature blue; GOES primary/secondary distinct blue/red; Hemispheric Power North light blue vs South purple. Keep reference lines visually secondary and do not alter chart data.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartMappingTest" --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest"`. All tests pass.

---

### Task 6: Refine Kp presentation and severity coloring

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/KpChartTest.kt`, `app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/KpChart.kt`, `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxBarChart.kt`

**Test first:** Keep threshold/status tests and add assertions that each mapped Kp bar retains its `KpStatus` for visual styling. Add presentation tests for the Kp card header/context and the existing `No current Kp` fallback.

**Implementation:** Refine `KpChart` into the same restrained card/header language as the other charts, using the PWA wording `Planetary Kp Index (3-hour)` and a compact current-status treatment. Extend the bar-chart rendering so Kp bars use the existing `mapKpBars` status classification to select meaningful severity colors instead of a single default column appearance. Reuse the existing `kpStatus` thresholds; do not create a second threshold table.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.KpChartTest" --tests "ca.stewark.helioflux.ui.components.ChartMappingTest"`. All tests pass.

---

### Task 7: Refine Hemispheric Power card and North/South legend

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/HemisphericPowerChart.kt`

**Test first:** Extend the existing hemispheric-power test to verify two presentation series remain ordered North then South and that the empty-data contract remains `No hemispheric power data`.

**Implementation:** Restyle `HemisphericPowerChart` to match the new chart-card language, with `Hemispheric Power (GW)` as the measurement title, `OVATION Model · Today` as context, and a clear North/South legend tied to the line identities introduced in Task 5. Preserve the existing no-data text and timeframe filtering.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest"`. All tests pass.

---

### Task 8: Reconstruct the compact Space Weather hierarchy

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`

**Test first:** Add a small, pure screen-layout descriptor used by `SpaceWeatherScreen` and assert compact ordering exactly matches the approved contract: Aurora hero, Solar Wind section, metrics, timeframe, five solar/near-Earth charts, Geomagnetic Activity section, Kp, Hemispheric Power. Preserve the existing Aurora presentation/freshness tests.

**Implementation:** Rebuild `SpaceWeatherScreen` around that hierarchy. Remove the old top-level `Space Weather` title row if it competes with the hero; the hero is the opening visual. Increase the Aurora slot from its current 240dp treatment to a large/prominent hero proportion suitable for a Pixel-sized screen, with rounded clipping and freshness unobtrusively overlaid. Follow it with a strong `Solar Wind` section heading, metric pills, segmented timeframe control, the five refined solar/near-Earth cards, then a clear `Geomagnetic Activity` section heading and the Kp/Hemispheric Power cards. Preserve `space-weather-screen`, `aurora-globe-slot`, and useful compact tags. Do not change `AuroraGlobe` itself or its lifecycle/data path.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherScreenTest"`. All tests pass.

---

### Task 9: Rebuild expanded-width chart composition

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`, `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`

**Test first:** Replace/extend the current column-count test with layout-group tests that assert the hero, section headings, metrics, and timeframe remain full-width while compatible chart cards form two-column rows on expanded layouts. Assert the odd solar chart and geomagnetic pairings do not create incorrect cross-section pairings.

**Implementation:** Keep compact charts single-column. For `expanded == true`, group only compatible cards into two-column rows within their own section; keep the Aurora hero and section-level content full-width. Preserve readable chart width rather than forcing all content into columns. Remove the old single `charts` list if it makes cross-section pairing possible.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherScreenTest"`. All tests pass.

---

### Task 10: Verify retained/no-data behavior through the reconstructed screen

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`, `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetricsTest.kt`, `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`

**Test first:** Add regression cases covering loading Aurora with the rest of the screen still describable, retained magnetic/plasma data still feeding metrics/charts, null metrics staying as em dashes, missing GOES data producing an empty chart series rather than fake values, and missing hemispheric data preserving its no-data contract.

**Implementation:** Make only the minimum presentation-layer adjustments exposed by those tests. Keep `RepositoryState.Available` and `RepositoryState.Failure.retainedData` behavior unchanged; do not add new loading/error state machinery.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.*"`. All Space Weather unit tests pass.

---

### Task 11: Run the full regression suite and inspect scope

**Files:** No planned production-file changes. Fix only failures caused by Tasks 1–10, in the already listed files.

**Test first:** No new behavior is introduced in this task; this is the integration gate after all focused TDD tasks.

**Implementation:** Run the full unit-test suite and compile the app. Inspect the Git diff to confirm that only the approved Space Weather UI, shared chart rendering required by that UI, tests, and this plan/design documentation changed. Do not modify the PWA or unrelated Android screens.

**Verify:** Run `./gradlew testDebugUnitTest assembleDebug`. Both commands complete successfully. Review `git diff --stat` and `git status` for unrelated changes.

---

### Task 12: Push the completed batch and monitor CI

**Files:** No additional source changes unless CI exposes a regression directly caused by this plan.

**Test first:** The local full-suite result from Task 11 is the pre-push gate.

**Implementation:** Commit the completed Space Weather UI reconstruction in the minimum sensible number of commits, push to `main`, and monitor the Android GitHub Actions run during the same working session. Re-check approximately every 45 seconds while pending. If CI fails, inspect the failing job/log, add a focused regression test when appropriate, make the minimum fix, push, and continue monitoring until green.

**Verify:** The GitHub Actions run for the final pushed SHA completes successfully.

---

### Task 13: Physical Pixel validation

**Files:** No planned source changes unless physical-device testing reveals a specific defect; any defect starts the project `$debugprompt` workflow rather than being patched ad hoc.

**Test first:** Use the approved design as the manual acceptance checklist; no production change is made in this task.

**Implementation:** Install the green build on the Pixel and verify: the Aurora globe is the prominent opening hero; scrolling does not trigger the prior Filament lifecycle crash; Solar Wind heading and three compact pills are readable; 12h is selected initially; the segmented control switches all four timeframes; chart headers/legends are readable; series are visually distinguishable; Kp severity colors are meaningful; Geomagnetic Activity is clearly separated; no-data/stale presentation remains honest; and returning to the screen remains stable.

**Verify:** Record physical-device acceptance. If a defect is found, stop this plan and route the defect through `$debugprompt` with the observed reproduction steps.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production change was preceded by its focused test
- [ ] Space Weather defaults to 12h
- [ ] Aurora is the prominent opening hero
- [ ] Solar Wind metrics render as compact PWA-inspired pills
- [ ] Timeframes render as one joined segmented control
- [ ] Chart cards have consistent hierarchy, context, legends, and deliberate series identities
- [ ] Kp uses the existing status thresholds for severity coloring
- [ ] Geomagnetic Activity is a distinct section
- [ ] Compact and expanded layouts follow the approved hierarchy
- [ ] Existing no-data, retained-data, and Aurora lifecycle behavior is preserved
- [ ] Full tests and debug build pass
- [ ] No unplanned files modified
- [ ] PWA remains unmodified
- [ ] Final GitHub Actions run is green
- [ ] Physical Pixel validation passes
