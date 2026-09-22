# Chart Inspection Tooltip Placement Implementation Plan

**Date:** 2026-09-22  
**Design doc:** docs/specs/2026-09-22-chart-inspection-tooltip-placement-design.md  
**Status:** Ready for review

## Overview

Replace Vico's stock `AroundPoint` marker-label positioning with a small shared HelioFlux marker specialization that preserves the existing formatter, guideline, press/drag behavior, and chart presentation while moving the transient tooltip approximately 48 dp away from the selected point. Placement will prefer above, flip below near the top, remain inside chart bounds, and prioritize a readable in-bounds label when the ideal clearance cannot fit. The public `HelioFluxLineChart` API and all chart consumers remain unchanged.

The implementation follows Vico's supported extension point: subclass `DefaultCartesianMarker` and override its over-layer drawing/label positioning rather than introducing Compose interaction state or reserving marker space above the chart.

## Tasks

### Task 1: Add deterministic tooltip-placement tests

**Files:** `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**

Add focused unit tests for an internal, platform-independent placement helper. Use simple Float geometry so the behavior can be verified without Compose/UI instrumentation.

Cover these cases:

```kotlin
@Test
fun markerLabelPrefersFortyEightDpAboveSelectedPoint() {
    val placement = markerLabelPlacement(
        targetX = 150f,
        pointTopY = 160f,
        pointBottomY = 160f,
        labelWidth = 100f,
        labelHeight = 40f,
        boundsLeft = 0f,
        boundsTop = 0f,
        boundsRight = 300f,
        boundsBottom = 240f,
        clearance = 48f,
    )

    assertEquals(MarkerLabelSide.Above, placement.side)
    assertEquals(72f, placement.top, 0f)
}

@Test
fun markerLabelFlipsBelowWhenPreferredClearanceDoesNotFitAbove() {
    val placement = markerLabelPlacement(
        targetX = 150f,
        pointTopY = 60f,
        pointBottomY = 60f,
        labelWidth = 100f,
        labelHeight = 40f,
        boundsLeft = 0f,
        boundsTop = 0f,
        boundsRight = 300f,
        boundsBottom = 240f,
        clearance = 48f,
    )

    assertEquals(MarkerLabelSide.Below, placement.side)
    assertEquals(108f, placement.top, 0f)
}
```

Add equivalent assertions that:

- a below-placement near the bottom is vertically clamped inside `boundsBottom`
- a point near the left edge produces `left >= boundsLeft`
- a point near the right edge produces `right <= boundsRight`
- a label too tall to maintain 48 px clearance on either side remains vertically in bounds and uses reduced clearance rather than overflowing

The tests should describe the approved behavior, not Vico internals. They must fail before Task 2 because the placement types/helper do not exist yet.

**Implementation:** No production implementation in this task.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. Confirm the new placement tests fail for the expected missing helper/types.

---

### Task 2: Implement the pure placement calculation

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`, `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:** Use the failing tests from Task 1 unchanged.

**Implementation:**

Add the smallest internal geometry model required by the tests:

```kotlin
internal enum class MarkerLabelSide {
    Above,
    Below,
}

internal data class MarkerLabelPlacement(
    val left: Float,
    val top: Float,
    val side: MarkerLabelSide,
)
```

Add `internal fun markerLabelPlacement(...): MarkerLabelPlacement` with these exact rules:

1. Center the label horizontally on `targetX`, then clamp its left edge to `boundsLeft..(boundsRight - labelWidth)`. If the label is wider than the bounds, use `boundsLeft`.
2. Calculate ideal-above top as `pointTopY - clearance - labelHeight`.
3. If ideal-above is at or below `boundsTop`, choose `Above` and use that top.
4. Otherwise choose `Below`; calculate ideal-below top as `pointBottomY + clearance`.
5. Clamp the chosen vertical top into `boundsTop..(boundsBottom - labelHeight)`. If the label is taller than the bounds, use `boundsTop`.
6. Do not add mutable state, chart-specific data, or Vico types to this helper.

This makes the calculation deterministic and separately testable while encoding the design's readability-over-clearance fallback.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. All placement tests pass, and existing chart presentation tests remain green.

---

### Task 3: Wire custom Vico marker drawing to the placement helper

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`, `app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**

Replace the obsolete source-contract assertion that requires:

```kotlin
labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint
```

with assertions that the shared renderer:

- defines/uses a single `48.dp` marker-clearance constant
- constructs the HelioFlux custom `DefaultCartesianMarker` specialization
- calls `markerLabelPlacement` from marker drawing
- still wires `valueFormatter = markerFormatter`
- still wires `lineCount = chartMarkerLineCount(preparedSeries.size)`
- still wires the marker guideline
- still uses `CartesianMarkerController.rememberShowOnPress`
- still passes `DefaultChartInteractionPolicy.consumeMoveEvents`

Keep the existing formatter/content tests unchanged.

**Implementation:**

In `HelioFluxLineChart.kt`:

1. Add one internal Compose constant/value representing the approved `48.dp` clearance. Do not expose it as a `HelioFluxLineChart` parameter.
2. Add a private `HelioFluxCartesianMarker` subclass of Vico `DefaultCartesianMarker`.
3. Override `drawOverLayers(context, targets)`, following Vico's `DefaultCartesianMarker` drawing sequence:
   - draw the existing guideline
   - draw any existing indicators using the inherited marker behavior/components
   - format the label with the existing `ValueFormatter`
   - calculate the average target X
   - calculate the selected point's top/bottom Y from the supplied marker targets; for line targets use their point canvas-Y values
   - get the label bounds from the existing `TextComponent`
   - convert the 48 dp clearance to pixels through Vico's drawing context
   - call `markerLabelPlacement` using `context.layerBounds`
   - update `MarkerCorneredShape` tick X/side so the label still points toward the selected location
   - draw the same label component at the calculated position, using the placement side to choose the appropriate vertical anchor/tick direction
4. For an empty or unsupported target collection, fall back safely to `super.drawOverLayers(context, targets)` rather than throwing.
5. Add a private `@Composable rememberHelioFluxCartesianMarker(...)` helper that mirrors only the arguments currently needed by this chart: label, value formatter, and guideline. Remember the marker from those dependencies plus the fixed clearance.
6. Replace `rememberDefaultCartesianMarker(... labelPosition = AroundPoint ...)` with the new remember helper. Preserve the existing `markerFormatter`, text component styling/line count, and guideline unchanged.
7. Do not modify `CartesianMarkerController`, chart host interaction policy, domains, axes, gridlines, reference lines, series rendering, or chart consumers.

Vico's documented extension pattern for custom positioning is to subclass `DefaultCartesianMarker` and override `drawOverLayers`; use the exact Vico 3.1.0 core types/signatures available to the project when adding imports.

**Verify:** Run `./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.components.ChartPresentationTest`. All focused tests pass.

---

### Task 4: Run the full local regression suite

**Files:** No additional files expected.

**Test first:** No new behavior is introduced here; this task validates the completed TDD batch.

**Implementation:** Do not change production code unless a regression directly caused by Tasks 2–3 is exposed. If a regression is found, add/adjust the focused test first, then make only the minimum correction.

**Verify:** Run the repository's full Android test/build checks used by CI. At minimum:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

All commands must pass. Confirm `git diff --name-only` contains only the planned chart implementation/test files plus this approved plan/design documentation already committed.

---

### Task 5: Commit, push, and monitor Android CI

**Files:** Only the implementation/test files from Tasks 1–3.

**Test first:** Confirm the complete local verification from Task 4 is green before committing.

**Implementation:**

Create one focused implementation commit for the shared chart-tooltip change and push it to `main` (one batch is appropriate because the design affects one shared chart subsystem).

Do not include unrelated working-tree changes.

**Verify:** Monitor the resulting GitHub Actions Android CI run during the same work session, re-checking roughly every 45 seconds until it completes. If CI fails, inspect the failure, add a regression test when appropriate, make the minimum fix, push, and continue monitoring until green or until user input is required.

---

### Task 6: Physical Pixel acceptance check

**Files:** No code changes expected.

**Test first:** Automated coverage and CI must already be green before physical acceptance testing.

**Implementation:** Install/run the green build on the physical Pixel and inspect at least one Space Weather chart and one Solar Activity chart.

Confirm while pressing, holding, and dragging:

- the tooltip remains associated with the selected point/guideline
- it is normally about 48 dp above the point/finger
- near the top it flips below
- near chart edges it remains readable and inside the chart
- no permanent blank marker band appears
- timestamp, series values, semantic colors, and guideline are unchanged
- press/drag inspection remains responsive
- chart zoom/pan remains disabled

**Verify:** Physical-device behavior matches the design on both screens. If a device-only placement problem appears, return to TDD: reproduce the geometry in `ChartPresentationTest.kt` first, then make the minimum shared-renderer correction.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Placement helper tests cover above, flip-below, bottom, left, right, and oversized-label cases
- [ ] Shared marker uses the approved 48 dp preferred clearance
- [ ] Tooltip stays readable/in bounds when ideal clearance is impossible
- [ ] Existing marker content, formatter, guideline, and press/drag behavior are preserved
- [ ] No permanent top marker band is reintroduced
- [ ] Public `HelioFluxLineChart` API is unchanged
- [ ] Space Weather and Solar Activity inherit the same shared behavior
- [ ] Full local checks pass
- [ ] No unplanned files modified
- [ ] GitHub Actions Android CI is green
- [ ] Physical Pixel acceptance check passes
