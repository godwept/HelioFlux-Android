# Metric Pill Text Centering Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-metric-pill-text-centering-design.md  
**Status:** Ready for review

## Overview
Center the complete label/value group inside the existing Home current-condition pills and Space Weather solar-wind pills. Keep all current dimensions, styling, spacing, formatting, and behavior unchanged.

## Tasks

### Task 1: Add failing alignment regressions
**Files:** `app/src/test/java/ca/stewark/helioflux/ui/home/CurrentConditionsSourceTest.kt`, `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetricsTest.kt`

**Test first:**
- Add a Home source regression that reads `CurrentConditions.kt` and asserts the pill content row includes `fillMaxWidth()` and `Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)`.
- Extend `SolarWindMetricsTest` with the same assertions against the `MetricPill` source.
- Keep existing tests unchanged.

**Implementation:** None in this task. Push the failing tests before production changes.

**Verify:** Android CI should fail because both current pill rows use only padding plus `Arrangement.spacedBy(5.dp)`.

### Task 2: Center Home pill content
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt`

**Test first:** Use the failing Home alignment regression from Task 1.

**Implementation:**
- Add `Alignment` import from `androidx.compose.ui`.
- Change only the internal pill `Row` modifier to `Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)`.
- Change only its horizontal arrangement to `Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)`.
- Do not alter pill styling, sizing, tags, formatting, or click behavior.

**Verify:** Home alignment regression passes and existing Home dashboard tests remain green.

### Task 3: Center Space Weather pill content
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetrics.kt`

**Test first:** Use the failing Space Weather alignment regression from Task 1.

**Implementation:**
- Add `Alignment` import from `androidx.compose.ui`.
- Change only the internal `MetricPill` row modifier to `Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=6.dp)`.
- Change only its horizontal arrangement to `Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)`.
- Do not alter presentation mapping, colors, dimensions, tags, formatting, or layout outside the pill.

**Verify:** Space Weather alignment regression and existing solar-wind metric tests pass.

### Task 4: Full verification
**Files:** No additional production files.

**Test first:** No new tests; this task validates the full regression suite.

**Implementation:** None.

**Verify:** Run the complete Android CI workflow and confirm unit tests, instrumentation compilation/tests, lint, and debug APK build are green.

## Definition of Done
- [ ] All tasks completed in order
- [ ] All tests pass
- [ ] No unplanned files modified
- [ ] Both Home and Space Weather metric pill label/value groups are centered
- [ ] All existing styling and behavior remain unchanged
