# Chart Series Line Width Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal and Success Criteria

Make all plotted data-series lines on Solar Activity and Space Weather charts thinner by setting the shared chart renderer to **1 dp**.

This applies to:

- Solar Activity X-Ray
- Solar Activity Particle Environment / ACE EPAM
- Space Weather line charts, including solar wind / Bz / Bt
- Hemispheric Power
- Any other chart that uses the shared `HelioFluxLineChart` renderer

It does not change:

- gridlines
- axes or ticks
- Bz zero/reference lines
- X-Ray C/M/X reference guides
- marker/guideline styling

Success means the charts look cleaner and less visually heavy while preserving their existing colors, data reduction, interaction, domains, animation behavior, and chart layout.

## Scope

### In scope

- Set the shared plotted data-series stroke thickness to **1 dp**.
- Apply the change consistently to every chart using `HelioFluxLineChart`.
- Preserve each series' existing semantic color.
- Preserve current chart heights, spacing, legends, axes, markers, inspection behavior, domains, reduction, and animation behavior.

### Out of scope

- Changing reference-line thickness.
- Changing gridline thickness.
- Changing axis/tick styling.
- Changing chart colors.
- Changing chart heights or layout.
- Changing touch/drag inspection.
- Changing data resolution or reduction.
- Changing chart-specific calculations.
- Adding per-chart stroke-width overrides.
- Adding a user setting for chart line width.

## Data and State

No new application state is required.

There is no new:

- ViewModel state
- repository state
- persistence
- user preference
- per-chart configuration

The shared presentation layer should define one fixed series stroke width of **1 dp** and use it whenever a plotted data-series line is constructed.

## Interfaces and Implementation Boundary

The change remains entirely inside the shared chart renderer.

`HelioFluxLineChart` already constructs each plotted data-series line through Vico's `LineCartesianLayer.rememberLine`. The project uses Vico **3.1.0**, whose `LineCartesianLayer.LineStroke.Continuous` supports a `thickness: Dp` argument.

The intended implementation is conceptually:

```kotlin
internal val ChartSeriesStrokeWidth = 1.dp

LineCartesianLayer.rememberLine(
    fill = ...,
    stroke = LineCartesianLayer.LineStroke.Continuous(
        thickness = ChartSeriesStrokeWidth,
    ),
)
```

No new parameter should be added to:

- `XrayChart`
- `AceEpamChart`
- `SpaceWeatherCharts`
- `HemisphericPowerChart`

All current and future charts using `HelioFluxLineChart` should inherit the same 1 dp plotted-series width automatically.

## Error Handling and Edge Cases

This is presentation-only, so no runtime error-handling path is added.

Safeguards:

- Use the exact Vico 3.1.0 stroke API supported by the installed dependency.
- Keep the width fixed at **1 dp**.
- Do not introduce per-series exceptions unless a genuine rendering defect is discovered during implementation.
- Reference lines and gridlines remain unchanged even if their relative visual weight becomes stronger.
- Empty/missing chart-data behavior remains unchanged.
- The Particle Environment chart's `animateInitial = false` scroll-jank fix remains intact.
- Do not add fallback widths, device-specific branching, or runtime configuration.

## Testing Strategy

Add or update focused regression coverage around the shared chart renderer.

Tests should verify:

- the shared plotted-series stroke width is **1 dp**;
- the shared renderer applies the custom continuous stroke to plotted series;
- Solar Activity and Space Weather charts continue to use `HelioFluxLineChart`;
- semantic series colors remain unchanged;
- reference-line styling remains unchanged;
- gridline/axis styling remains unchanged;
- the Particle Environment chart still uses `animateInitial = false`;
- no chart-specific data-series width overrides are introduced.

Verification should include the focused chart tests followed by the normal Android CI sequence:

- unit tests
- instrumentation-test compilation
- connected instrumentation tests when a device is available
- lint
- debug APK build

Physical Pixel verification should confirm:

- plotted lines are visibly thinner across Solar Activity and Space Weather;
- all series remain readable;
- reference lines/gridlines retain their existing visual weight;
- scrolling and chart inspection behavior are unchanged.

## Open Questions

None.
