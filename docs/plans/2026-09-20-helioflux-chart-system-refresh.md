# HelioFlux Chart System Refresh Implementation Plan

**Date:** 2026-09-20  
**Design doc:** `docs/specs/2026-09-20-helioflux-chart-system-refresh-design.md`  
**Status:** Ready for review

## Overview

Upgrade the existing shared Vico chart wrappers so Space Weather and Solar Activity charts behave like readable monitoring instruments instead of bare sparklines. The implementation will add explicit time domains, no-scroll/no-zoom chart state, UTC axes, Y-axis ranges with padding, subtle gridlines, semantic series colors, real reference guides, transient press-and-drag inspection, and min/max-preserving render reduction for dense line data. Space Weather keeps its shared 1h/3h/12h/2d selector and always re-fits to the selected window; Solar Activity keeps its existing fixed 72-hour X-Ray and ACE EPAM windows. Repository/network/retention logic, Aurora globe code, imagery code, Home, and navigation remain untouched.

## Implementation batches

To minimize GitHub Actions usage while keeping changes atomic:

1. **Batch 1 — Shared chart core:** Tasks 1–11. Run focused tests locally after each task, then run the full app unit suite once, commit/push once, and monitor CI to green.
2. **Batch 2 — Space Weather integration:** Tasks 12–13. Run focused + full app unit tests, commit/push once, and monitor CI to green.
3. **Batch 3 — Solar Activity integration:** Task 14. Run focused + full app unit tests, commit/push once, and monitor CI to green.
4. **Final verification:** Task 15. Do not create a new commit unless verification exposes a defect that requires a focused fix.

Do not begin the next batch while the previous pushed batch has failing CI.

## Tasks

### Task 1: Add shared chart domain and presentation primitives

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun chartDomainUsesExactRequestedWindow() {
    val now = 10_000_000L
    assertEquals(
        ChartDomain(
            minX = (now - 3L * 60L * 60L * 1_000L).toDouble(),
            maxX = now.toDouble(),
        ),
        chartDomain(now, 3L * 60L * 60L * 1_000L),
    )
}

@Test
fun defaultInteractionPolicyDisablesChartNavigation() {
    assertFalse(DefaultChartInteractionPolicy.scrollEnabled)
    assertFalse(DefaultChartInteractionPolicy.zoomEnabled)
    assertFalse(DefaultChartInteractionPolicy.consumeMoveEvents)
}
```

**Implementation:**

Create the shared presentation file with:

```kotlin
data class ChartDomain(
    val minX: Double,
    val maxX: Double,
)

fun chartDomain(nowMillis: Long, durationMillis: Long): ChartDomain =
    ChartDomain(
        minX = (nowMillis - durationMillis).toDouble(),
        maxX = nowMillis.toDouble(),
    )

data class ChartInteractionPolicy(
    val scrollEnabled: Boolean,
    val zoomEnabled: Boolean,
    val consumeMoveEvents: Boolean,
)

val DefaultChartInteractionPolicy = ChartInteractionPolicy(
    scrollEnabled = false,
    zoomEnabled = false,
    consumeMoveEvents = false,
)
```

Keep these types presentation-only; do not add repository dependencies.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 2: Add UTC X-axis and value formatting helpers

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun utcAxisFormattingChangesWithWindowLength() {
    val instant = Instant.parse("2026-09-20T12:34:00Z").toEpochMilli().toDouble()

    assertEquals(
        "12:34",
        formatUtcAxisLabel(
            instant,
            ChartDomain(instant - 3.hours.inWholeMilliseconds, instant),
        ),
    )

    assertEquals(
        "Sep 20 12:34",
        formatUtcAxisLabel(
            instant,
            ChartDomain(instant - 72.hours.inWholeMilliseconds, instant),
        ),
    )
}

@Test
fun xrayFormatterConvertsLogValueBackToFlux() {
    assertEquals("1e-06", formatChartValue(-6.0, ChartValueFormat.XrayFlux))
}
```

**Implementation:**

Add:

```kotlin
enum class ChartValueFormat {
    Compact,
    Scientific,
    XrayFlux,
}
```

Implement:

- `formatUtcAxisLabel(xMillis, domain)`
  - domain length <= 12 hours: `HH:mm` in UTC;
  - domain length > 12 hours: `MMM d HH:mm` in UTC.
- `chartXStepMillis(domain)`
  - <= 1h: 15 minutes;
  - <= 3h: 30 minutes;
  - <= 12h: 2 hours;
  - <= 48h: 6 hours;
  - > 48h: 12 hours.
- `formatChartValue(value, format)`
  - `Compact`: concise decimal formatting suitable for normal Space Weather values;
  - `Scientific`: scientific notation;
  - `XrayFlux`: treat the plotting value as `log10(flux)`, convert with `10.0.pow(value)`, then format in scientific notation.

Use `java.time.Instant`, `ZoneOffset.UTC`, and immutable `DateTimeFormatter` instances.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 3: Add deterministic padded Y-domain calculation

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun autoYDomainPadsVisibleValuesWithoutForcingZero() {
    val domain = ChartDomain(0.0, 10.0)
    val out = paddedYDomain(
        values = listOf(
            ChartPoint(1.0, 100.0),
            ChartPoint(2.0, 120.0),
        ),
        domain = domain,
        requiredValues = emptyList(),
    )!!

    assertTrue(out.minY > 0.0)
    assertTrue(out.minY < 100.0)
    assertTrue(out.maxY > 120.0)
}

@Test
fun requiredReferenceValueIsIncludedInYDomain() {
    val out = paddedYDomain(
        values = listOf(ChartPoint(1.0, 5.0), ChartPoint(2.0, 8.0)),
        domain = ChartDomain(0.0, 10.0),
        requiredValues = listOf(0.0),
    )!!

    assertTrue(out.minY <= 0.0)
}

@Test
fun constantSeriesGetsNonZeroRange() {
    val out = paddedYDomain(
        values = listOf(ChartPoint(1.0, 4.0), ChartPoint(2.0, 4.0)),
        domain = ChartDomain(0.0, 10.0),
        requiredValues = emptyList(),
    )!!

    assertTrue(out.maxY > out.minY)
}
```

**Implementation:**

Add:

```kotlin
data class ChartYDomain(
    val minY: Double,
    val maxY: Double,
)

const val ChartYRangePaddingFraction = 0.08
```

Implement `paddedYDomain(...)` with these exact rules:

1. Only include finite non-null values whose X is inside the supplied `ChartDomain`.
2. Include finite `requiredValues` such as Bz zero.
3. If no valid values exist, return `null`.
4. If `minY != maxY`, pad both ends by 8% of the range.
5. If `minY == maxY`, use `max(abs(value) * 0.08, 1.0)` as symmetric padding.
6. Do not force zero unless zero is explicitly supplied in `requiredValues`.

Fixed metric domains, such as X-Ray and Kp, will bypass this helper later.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 4: Add min/max-preserving dense-series reduction

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun sparseSeriesPassesThroughUnchanged() {
    val points = listOf(
        ChartPoint(1.0, 4.0),
        ChartPoint(2.0, 2.0),
        ChartPoint(3.0, 8.0),
    )

    assertEquals(
        points,
        reduceMinMax(points, ChartDomain(0.0, 10.0), maxRenderPoints = 10),
    )
}

@Test
fun denseReductionPreservesBucketExtremaAndRealTimestamps() {
    val points = (0..99).map { x ->
        ChartPoint(x.toDouble(), if (x == 45) -20.0 else x.toDouble())
    }

    val reduced = reduceMinMax(
        points,
        ChartDomain(0.0, 99.0),
        maxRenderPoints = 20,
    )

    assertTrue(reduced.size <= 20)
    assertTrue(reduced.any { it.x == 45.0 && it.y == -20.0 })
    assertTrue(reduced.all { candidate -> candidate in points })
    assertEquals(reduced.sortedBy { it.x }, reduced)
}
```

**Implementation:**

Add:

```kotlin
const val DefaultMaxRenderPoints = 512
```

Implement `reduceMinMax(points, domain, maxRenderPoints)` as a pure deterministic function:

1. Sort input chronologically by X.
2. Restrict rendering to the supplied domain.
3. Treat null/non-finite Y values as gap markers; do not turn them into values.
4. If valid point count is already below the render budget, return normalized points unchanged.
5. Reserve capacity for the first and last valid observations.
6. Divide the full X-domain into chronological buckets using the remaining valid-point budget.
7. For each bucket, retain only the real minimum and maximum observations, ordered by timestamp.
8. Deduplicate when min and max identify the same source observation.
9. Always preserve the first and last valid observations.
10. Never average, interpolate, synthesize, or move a timestamp.
11. The number of non-null rendered observations must not exceed `maxRenderPoints`.

Do not add coroutines, caches, or mutable global state.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 5: Preserve null gaps and invalid-value boundaries during reduction

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun reductionPreservesAVisibleGapBetweenRuns() {
    val points = listOf(
        ChartPoint(1.0, 1.0),
        ChartPoint(2.0, 2.0),
        ChartPoint(3.0, null),
        ChartPoint(4.0, 3.0),
        ChartPoint(5.0, 4.0),
    )

    val reduced = reduceMinMax(
        points,
        ChartDomain(0.0, 10.0),
        maxRenderPoints = 4,
    )

    val segments = nonNullSegments(reduced)
    assertEquals(2, segments.size)
    assertEquals(listOf(1.0, 2.0), segments[0].map { it.x })
    assertEquals(listOf(4.0, 5.0), segments[1].map { it.x })
}

@Test
fun nonFiniteValuesAreTreatedAsGaps() {
    val segments = nonNullSegments(
        listOf(
            ChartPoint(1.0, 1.0),
            ChartPoint(2.0, Double.NaN),
            ChartPoint(3.0, 3.0),
        ),
    )

    assertEquals(2, segments.size)
}
```

**Implementation:**

Add `nonNullSegments(points)` and make the reducer preserve one gap boundary between consecutive valid runs.

Rules:

- `null`, `NaN`, and infinite Y values are gap boundaries.
- Collapse consecutive gap markers to one boundary.
- Gap markers do not count against the valid-observation render budget.
- Rendering code must later submit each returned non-null segment to Vico as a separate line series so Vico cannot draw across a missing-data interval.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 6: Extend logical series and reference metadata

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`

**Test first:**
```kotlin
@Test
fun lineSeriesCarriesMarkerLabelFormatAndReductionPolicy() {
    val series = LineChartSeries(
        label = "Bz",
        points = listOf(ChartPoint(1.0, -3.0)),
        style = ChartSeriesStyle.Bz,
        valueFormat = ChartValueFormat.Compact,
        allowReduction = true,
    )

    assertEquals("Bz", series.label)
    assertEquals(ChartSeriesStyle.Bz, series.style)
    assertEquals(ChartValueFormat.Compact, series.valueFormat)
    assertTrue(series.allowReduction)
}

@Test
fun referenceLineCarriesLabelAndVisualRole() {
    val line = ChartReferenceLine(
        y = -6.0,
        label = "C",
        style = ChartReferenceStyle.Caution,
    )

    assertEquals("C", line.label)
    assertEquals(ChartReferenceStyle.Caution, line.style)
}
```

**Implementation:**

Update the shared models to:

```kotlin
enum class ChartSeriesStyle {
    Default,
    Bz,
    Primary,
    Secondary,
    Alert,
    Purple,
    Success,
    Warning,
}

data class LineChartSeries(
    val label: String,
    val points: List<ChartPoint>,
    val style: ChartSeriesStyle = ChartSeriesStyle.Default,
    val valueFormat: ChartValueFormat = ChartValueFormat.Compact,
    val allowReduction: Boolean = true,
)

enum class ChartReferenceStyle {
    Neutral,
    Caution,
    Warning,
    Alert,
}

data class ChartReferenceLine(
    val y: Double,
    val label: String? = null,
    val style: ChartReferenceStyle = ChartReferenceStyle.Neutral,
)
```

Update `seriesColor`:

- Default / Secondary -> `DataCyan`
- Bz / Alert -> `AlertRed`
- Primary -> `DataBlue`
- Purple -> existing purple
- Success -> `FreshGreen`
- Warning -> `WarningAmber`

Add `referenceColor`:

- Neutral -> `SpaceMuted`
- Caution -> `WarningAmber`
- Warning -> `SolarOrange`
- Alert -> `AlertRed`

Keep colors centralized in these mapping helpers so legends, line rendering, markers, and guides share the same source.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartMappingTest"
```
All tests pass.

---

### Task 7: Build logical render-series preparation

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun renderPreparationReducesEligibleSeriesAndKeepsRawSeriesRaw() {
    val dense = (0..999).map { ChartPoint(it.toDouble(), it.toDouble()) }

    val eligible = prepareLineSeriesForRender(
        LineChartSeries("Dense", dense, allowReduction = true),
        ChartDomain(0.0, 999.0),
        maxRenderPoints = 100,
    )
    val raw = prepareLineSeriesForRender(
        LineChartSeries("Raw", dense, allowReduction = false),
        ChartDomain(0.0, 999.0),
        maxRenderPoints = 100,
    )

    assertTrue(eligible.points.count { it.y != null } <= 100)
    assertEquals(1000, raw.points.count { it.y != null })
}
```

**Implementation:**

Add `prepareLineSeriesForRender(series, domain, maxRenderPoints)`:

- clip/sort points to domain;
- apply `reduceMinMax` only when `allowReduction == true`;
- preserve label/style/value-format metadata unchanged;
- return a logical reduced series that can still be used by marker inspection;
- let the Vico adapter split that logical series into `nonNullSegments`.

Do not reduce Hemispheric Power; its callers will set `allowReduction = false`.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass.

---

### Task 8: Add explicit Vico range and navigation policy

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`

**Test first:**
```kotlin
@Test
fun chartInteractionContractIsNonScrollableAndNonZoomable() {
    assertFalse(DefaultChartInteractionPolicy.scrollEnabled)
    assertFalse(DefaultChartInteractionPolicy.zoomEnabled)
    assertFalse(DefaultChartInteractionPolicy.consumeMoveEvents)
}
```

**Implementation:**

Change `HelioFluxLineChart` signature to require:

```kotlin
@Composable
fun HelioFluxLineChart(
    series: List<LineChartSeries>,
    domain: ChartDomain,
    modifier: Modifier = Modifier,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    fixedYDomain: ChartYDomain? = null,
    yAxisFormat: ChartValueFormat = ChartValueFormat.Compact,
)
```

Inside the composable:

1. Prepare logical render series with `prepareLineSeriesForRender(..., DefaultMaxRenderPoints)`.
2. Compute Y-domain:
   - use `fixedYDomain` when provided;
   - otherwise call `paddedYDomain` using all logical series points plus reference-line Y values as required values.
3. Create one stable `CartesianLayerRangeProvider` instance whose:
   - `getMinX` returns `domain.minX`;
   - `getMaxX` returns `domain.maxX`;
   - `getMinY`/`getMaxY` return the resolved Y-domain when present.
4. Pass:
   - `rememberVicoScrollState(scrollEnabled = false)`;
   - `rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content)`.

Do not persist or restore chart scroll/zoom position. Domain changes are the viewport reset.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartMappingTest" --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
```
All tests pass and `:app:compileDebugKotlin` succeeds.

---

### Task 9: Add axes and subtle gridlines to the shared line chart

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun xStepMatchesApprovedWindowBands() {
    val hour = 60L * 60L * 1_000L

    assertEquals(15L * 60L * 1_000L, chartXStepMillis(ChartDomain(0.0, hour.toDouble())))
    assertEquals(2L * hour, chartXStepMillis(ChartDomain(0.0, (12L * hour).toDouble())))
    assertEquals(12L * hour, chartXStepMillis(ChartDomain(0.0, (72L * hour).toDouble())))
}
```

**Implementation:**

Configure `rememberCartesianChart` with:

- `VerticalAxis.rememberStart`
  - Y-value formatter delegates to `formatChartValue(value, yAxisFormat)`;
  - axis label/tick styling uses `MaterialTheme.colorScheme.onSurfaceVariant`;
  - horizontal guidelines use an outline/on-surface color with low alpha.
- `HorizontalAxis.rememberBottom`
  - formatter delegates to `formatUtcAxisLabel(x, domain)`;
  - aligned item placer uses `chartXStepMillis(domain)` through chart `getXStep`;
  - vertical guidelines use the same restrained low-alpha component.
- No fading edges because the chart is not scrollable.

Keep axis styling subordinate to the data lines.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
./gradlew :app:compileDebugKotlin
```
Both succeed.

---

### Task 10: Render semantic line colors and preserve data gaps

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`

**Test first:**
```kotlin
@Test
fun semanticStylesMapToTheExpectedThemeColors() {
    assertEquals(AlertRed, seriesColor(ChartSeriesStyle.Bz))
    assertEquals(DataBlue, seriesColor(ChartSeriesStyle.Primary))
    assertEquals(DataCyan, seriesColor(ChartSeriesStyle.Secondary))
    assertEquals(FreshGreen, seriesColor(ChartSeriesStyle.Success))
    assertEquals(WarningAmber, seriesColor(ChartSeriesStyle.Warning))
}
```

**Implementation:**

Replace Vico's default line provider with an explicit `LineCartesianLayer.LineProvider.series(...)`.

For each logical render series:

1. Split reduced points with `nonNullSegments`.
2. Add each non-empty segment as a separate Vico line series.
3. Create one Vico `Line` for each flattened segment using the parent logical series' `seriesColor(style)`.
4. Feed the flattened segment list to `lineSeries { ... }` in the same order as the flattened Vico line definitions.

This keeps one logical legend/marker series while preventing Vico from connecting across missing values.

Do not add point markers or smoothing.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartMappingTest" --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
./gradlew :app:compileDebugKotlin
```
Both succeed.

---

### Task 11: Add real reference guides and transient marker inspection

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/components/ChartPresentation.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartPresentationTest.kt`

**Test first:**
```kotlin
@Test
fun markerRowsUseOnlyRealPlottedValuesAtSelectedTimestamp() {
    val series = listOf(
        LineChartSeries(
            "Bz",
            listOf(ChartPoint(10.0, -4.0), ChartPoint(20.0, -2.0)),
            ChartSeriesStyle.Bz,
        ),
        LineChartSeries(
            "Bt",
            listOf(ChartPoint(10.0, 6.0), ChartPoint(20.0, null)),
            ChartSeriesStyle.Secondary,
        ),
    )

    val rows = markerRowsAtX(series, 20.0)

    assertEquals(listOf("Bz"), rows.map { it.label })
    assertEquals(-2.0, rows.single().value, 0.0)
}

@Test
fun referenceGuidesDoNotBecomeMarkerRows() {
    val rows = markerRowsAtX(
        listOf(LineChartSeries("Bz", listOf(ChartPoint(1.0, -3.0)))),
        1.0,
    )
    assertEquals(1, rows.size)
}
```

**Implementation:**

Add pure marker helpers:

```kotlin
data class ChartMarkerRow(
    val label: String,
    val value: Double,
    val style: ChartSeriesStyle,
    val valueFormat: ChartValueFormat,
)
```

Implement `markerRowsAtX(renderSeries, selectedX)` so it:

- matches only finite non-null observations at exactly `selectedX`;
- never interpolates;
- never includes reference lines;
- preserves logical series order.

In `HelioFluxLineChart`:

1. Convert each `ChartReferenceLine` into Vico `HorizontalLine` decoration instead of adding fake data.
2. Use a thin dashed/low-alpha line component.
3. Add labels only when `ChartReferenceLine.label != null`.
4. Build a `rememberDefaultCartesianMarker` with:
   - a vertical guideline;
   - a custom value formatter that extracts the selected X from the first Vico line target, formats its UTC timestamp, then looks up all display rows with `markerRowsAtX`;
   - series-colored value text/indicators where supported by the existing Vico marker API.
5. Use `CartesianMarkerController.rememberShowOnPress(consumeMoveEvents = false)`.

**Vico 3.1.0 implementation note:** Do not rely on the Vico target list to contain every logical series in a non-scrollable multi-series chart. Use the Vico target only to obtain the selected X; derive the displayed rows from HelioFlux's own prepared logical series at that X.

The default show-on-press controller gives the approved transient behavior: marker appears on press/move and disappears on release. `consumeMoveEvents = false` is required so vertical page scrolling can take ownership of vertical movement.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.components.ChartPresentationTest"
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```
All succeed.

**Batch 1 checkpoint:** Commit/push Tasks 1–11 as one shared-chart batch. Monitor the resulting Android CI run until green. If CI fails, inspect the failure, make the minimum focused fix, push, and continue monitoring before Task 12.

---

### Task 12: Wire explicit domains, labels, formats, and reduction policy into Space Weather line charts

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`

**Test first:**
```kotlin
@Test
fun spaceWeatherDomainUsesSelectedTimeframeInsteadOfSampleBounds() {
    val now = 10_000_000L
    assertEquals(
        ChartDomain(
            minX = (now - Timeframe.TwoDays.durationMillis).toDouble(),
            maxX = now.toDouble(),
        ),
        spaceWeatherChartDomain(Timeframe.TwoDays, now),
    )
}

@Test
fun spaceWeatherSeriesCarryMarkerLabelsAndSemanticStyles() {
    val bzBt = bzBtSeries(
        listOf(SolarWindMag(now - 1, null, null, -2.0, 4.0)),
        Timeframe.OneHour,
        now,
    )

    assertEquals(listOf("Bz", "Bt"), bzBt.map { it.label })
    assertEquals(
        listOf(ChartSeriesStyle.Bz, ChartSeriesStyle.Secondary),
        bzBt.map { it.style },
    )
}
```

**Implementation:**

Add:

```kotlin
fun spaceWeatherChartDomain(timeframe: Timeframe, now: Long): ChartDomain =
    chartDomain(now, timeframe.durationMillis)
```

Update line-series factories:

- Bz -> label `Bz`, style `Bz`, compact format, reduction enabled.
- Bt -> label `Bt`, style `Secondary`, compact format, reduction enabled.
- Density -> label `Density`, compact format, reduction enabled.
- Speed -> label `Speed`, compact format, reduction enabled.
- Temperature -> label `Temperature`, compact format, reduction enabled.
- GOES primary/secondary -> dynamic instrument labels when available, styles `Primary` / `Alert`, compact format, reduction enabled.

Change `plasmaSeries` to accept a required marker label.

Change `SpaceWeatherLineCard` to accept a required `ChartDomain` and pass it to `HelioFluxLineChart`.

In `SpaceWeatherScreen` compute the domain once from `state.timeframe` and `nowMillis`, then pass the same domain to Bz/Bt, Density, Speed, Temperature, GOES, Kp, and Hemispheric Power.

Change Bz reference metadata to:

```kotlin
ChartReferenceLine(
    y = 0.0,
    style = ChartReferenceStyle.Neutral,
)
```

Do not change the existing sample filtering functions; the explicit domain is a presentation contract layered on top of the already-correct filtering.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest" --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherScreenTest"
```
All tests pass.

---

### Task 13: Bring Hemispheric Power and Kp into the shared chart presentation

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/HemisphericPowerChart.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/spaceweather/KpChart.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxBarChart.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/spaceweather/KpChartTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/components/ChartMappingTest.kt`

**Test first:**
```kotlin
@Test
fun hemisphericPowerRemainsRawAndCarriesLabels() {
    val series = hemisphericPowerSeries(
        listOf(HemisphericPowerSample(now - 1, 40.0, 30.0)),
        Timeframe.OneHour,
        now,
    )

    assertEquals(listOf("North", "South"), series.map { it.label })
    assertTrue(series.none { it.allowReduction })
}

@Test
fun kpUsesFixedNaturalScaleAndSelectedDomain() {
    assertEquals(ChartYDomain(0.0, 9.0), KpYDomain)
}
```

**Implementation:**

Hemispheric Power:

- set logical labels `North` and `South`;
- keep styles `Secondary` and `Purple`;
- set `allowReduction = false`;
- add a required `domain: ChartDomain` parameter to `HemisphericPowerChart`;
- pass the domain into `HelioFluxLineChart`.

Kp:

1. Add `val KpYDomain = ChartYDomain(0.0, 9.0)`.
2. Change `KpChart` to accept the same selected `ChartDomain`.
3. Change `HelioFluxBarChart` signature to require `domain` and optionally `fixedYDomain`, using Kp's fixed 0–9 range.
4. Add the same start/bottom axes, UTC labels, subtle gridlines, explicit non-scroll/no-zoom states, and custom range provider as the line wrapper.
5. Keep the existing `kpPresentation` short-window fallback exactly as-is.
6. Do not downsample or interpolate Kp.
7. Do not add line-chart marker behavior to Kp in this task.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.spaceweather.SpaceWeatherChartsTest" --tests "ca.stewark.helioflux.ui.spaceweather.KpChartTest" --tests "ca.stewark.helioflux.ui.components.ChartMappingTest"
./gradlew :app:testDebugUnitTest
```
All tests pass.

**Batch 2 checkpoint:** Commit/push Tasks 12–13 as one Space Weather batch. Monitor Android CI until green before Task 14.

---

### Task 14: Apply the shared chart system to Solar Activity without adding a timeframe selector

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityViewModel.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/XrayChart.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/AceEpamChart.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt`

**Test first:**
```kotlin
@Test
fun solarActivityChartWindowRemainsExactly72Hours() {
    val now = 10_000_000L
    assertEquals(
        ChartDomain(
            minX = (now - SolarActivityWindowMillis).toDouble(),
            maxX = now.toDouble(),
        ),
        solarActivityChartDomain(now),
    )
    assertEquals(72L * 60L * 60L * 1_000L, SolarActivityWindowMillis)
}

@Test
fun xraySeriesUseApprovedLabelsStylesAndReferenceLevels() {
    val references = xrayReferenceLines()
    assertEquals(listOf("C", "M", "X"), references.map { it.label })
    assertEquals(
        listOf(
            ChartReferenceStyle.Caution,
            ChartReferenceStyle.Warning,
            ChartReferenceStyle.Alert,
        ),
        references.map { it.style },
    )
}
```

**Implementation:**

1. Replace the private ViewModel `WINDOW` constant with one shared top-level/internal:
   ```kotlin
   internal const val SolarActivityWindowMillis = 72L * 60L * 60L * 1_000L
   ```
   Reuse it in the existing X-Ray and ACE repository calls so fetch behavior is unchanged.
2. Add:
   ```kotlin
   fun solarActivityChartDomain(now: Long): ChartDomain =
       chartDomain(now, SolarActivityWindowMillis)
   ```
3. Add optional `nowMillis: Long = System.currentTimeMillis()` to `SolarActivityScreen`, compute one 72h domain, and pass it to both charts.
4. `XrayChart`:
   - require `domain`;
   - use fixed Y-domain `ChartYDomain(-9.0, -2.0)` because plotting remains log10;
   - use Y-axis/marker format `ChartValueFormat.XrayFlux`;
   - use logical labels/styles:
     - `GOES-18 Short` -> `Secondary`;
     - `GOES-18 Long` -> `Success`;
     - `GOES-19 Short` -> `Warning`;
     - `GOES-19 Long` -> `Alert`;
   - reduction enabled for all four;
   - replace fake reference-series behavior with:
     - C: `log10(1e-6)`, label `C`, style `Caution`;
     - M: `log10(1e-5)`, label `M`, style `Warning`;
     - X: `log10(1e-4)`, label `X`, style `Alert`.
5. `AceEpamChart`:
   - require `domain`;
   - labels/styles:
     - `Electron 38-53 keV` -> `Secondary`;
     - `Proton 47-68 keV` -> `Warning`;
     - `Proton 115-195 keV` -> `Alert`;
   - use `ChartValueFormat.Scientific`;
   - reduction enabled.
6. Do not add `TimeframeSelector` or any new Solar Activity timeframe state.

**Verify:**  
Run:
```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test"
./gradlew :app:testDebugUnitTest
```
All tests pass.

**Batch 3 checkpoint:** Commit/push Task 14 as one Solar Activity batch. Monitor Android CI until green.

---

### Task 15: Run final regression and physical-device verification

**Files:** No planned production-file changes. Only make a focused fix if verification exposes a real defect.

**Test first:**  
No new test should be added merely to satisfy this task. Any defect discovered here must first receive a focused failing regression test before its fix.

**Implementation / verification:**

Run the full local verification suite:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Confirm no unrelated files changed:

```bash
git status --short
git diff --stat
```

After the final pushed implementation commit, monitor Android CI to green.

Then perform the approved Pixel checks:

1. Open Space Weather at the default timeframe; every chart shows the full selected window rather than a max-zoomed viewport.
2. Cycle `1h -> 3h -> 12h -> 2d -> 1h`; all Space Weather charts immediately re-fit.
3. Verify X/Y axes and subtle grids remain legible at each window.
4. Press and drag horizontally on several line charts; the marker tracks real plotted timestamps and reports UTC plus available values.
5. Start a vertical swipe on a chart; the parent page scrolls normally.
6. Confirm Bz is red and its zero guide is visually secondary.
7. Inspect 2d Bz/GOES traces; sharp extrema remain visible and are not smoothed away.
8. Confirm Hemispheric Power remains raw and Kp remains discrete with sensible short-window fallback.
9. Open Solar Activity; X-Ray and ACE EPAM use the same improved chart language while remaining fixed at 72 hours.
10. Confirm X-Ray is still logarithmic, displays meaningful scientific flux labels, and shows labeled C/M/X guides.
11. Repeatedly scroll through both screens to catch chart/rendering regressions.
12. Recheck the Aurora globe after chart testing to confirm its already-stable interaction/render/lifecycle behavior is unchanged.

If any physical-device issue appears, follow the project `$debugprompt` workflow before changing production code.

**Verify:**  
All unit tests and `assembleDebug` pass, Android CI is green, no unrelated files are modified, and the physical-device checklist succeeds.

## Definition of Done

- [ ] Tasks 1–15 completed in order
- [ ] Every implementation task followed TDD: failing test first, minimum implementation second
- [ ] Shared Space Weather domains are exactly `[now - timeframe.durationMillis, now]`
- [ ] Solar Activity X-Ray/ACE domains remain exactly 72 hours
- [ ] No manual chart zoom or horizontal pan remains
- [ ] Vertical screen scrolling works through chart regions
- [ ] UTC X-axis, Y-axis, and subtle gridlines are present
- [ ] Rendered line colors match semantic legend colors
- [ ] Null/missing measurements remain visible gaps
- [ ] Bz zero and X-Ray C/M/X use proper reference guides
- [ ] Dense eligible series use min/max-preserving render reduction without synthetic values
- [ ] Kp and Hemispheric Power remain raw
- [ ] X-Ray logarithmic semantics remain intact
- [ ] Solar Activity does not gain a timeframe selector
- [ ] Aurora globe, repositories, networking, persistence, imagery, Home, and navigation are unchanged
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:assembleDebug` passes
- [ ] Every pushed batch has a green Android CI run
- [ ] No unrelated files are modified
- [ ] Physical-device verification passes
