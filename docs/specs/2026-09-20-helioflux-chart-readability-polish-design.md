# HelioFlux Chart Readability Polish Design

**Date:** 2026-09-20  
**Status:** Approved

## Goal and success criteria

Polish the existing Android chart presentation for physical Pixel readability without changing chart data behavior.

Success means:

- Space Weather chart plots use a taller 240dp presentation.
- UTC X-axis and Y-axis labels remain readable without dominating the plotted data.
- Gridlines provide useful reference while remaining visually quieter than data series.
- Data series remain visually stronger than reference lines, and reference lines remain stronger than ordinary gridlines.
- Bz zero and other reference guides read as secondary chart guides rather than ordinary plotted series.
- Longer timeframes, especially 12h and 2d, avoid crowded labels.
- Press/drag inspection remains useful while keeping its marker compact enough not to unnecessarily obscure the plot.
- Existing timeframe, domain, reduction, color, and interaction behavior is preserved.
- Aurora globe and Filament rendering/lifecycle code is untouched.

## Scope

### In scope

- Increase Space Weather chart plot height from 180dp to approximately 240dp.
- Refine shared axis spacing and label presentation where needed.
- Reduce gridline visual weight or density where it competes with the plotted series.
- Preserve clear hierarchy between data series, reference lines, and gridlines.
- Improve plot padding so labels and data do not feel pressed against card edges.
- Refine the existing press/drag marker presentation if its footprint is excessive.
- Allow shared presentation improvements in `HelioFluxLineChart` to benefit Solar Activity X-Ray and ACE EPAM where appropriate.

Space Weather controls chart height at the calling screen. Solar Activity retains its existing 240dp/260dp sizing unless later physical-device testing identifies a separate need.

### Out of scope

- Data fetching or retention changes.
- Timeframe semantics.
- Reduction algorithm changes.
- Manual chart zoom or pan.
- New chart interactions.
- Chart-library replacement.
- Broad chart-card redesign.
- Aurora globe or Filament changes.

For a 2d selection, a partially empty 48-hour domain is acceptable when Room has not accumulated the full history. The chart will not add an explanatory availability message or stretch/synthesize data to disguise the gap.

## Data and state

No new application state is required.

The existing `Timeframe` selection remains the source of the visible X-domain, and charts continue receiving the same repository/ViewModel data as today.

The existing pipeline remains:

repository data -> timeframe filtering/domain -> existing reduction strategy -> Vico model -> fixed timeframe viewport

Dense continuous series continue using min/max-preserving reduction. Kp and Hemispheric Power continue using their existing raw treatment.

Y scaling, reference-line values, semantic colors, and marker values retain their current calculations. This pass changes presentation only.

The X-domain may legitimately begin earlier than the oldest locally stored sample. In that case, the chart renders the unavailable portion as empty space without changing the requested domain or synthesizing samples.

## Interfaces and presentation API

Keep the existing shared `HelioFluxLineChart` API essentially unchanged. Its current series, domain, reference-line, formatting, and interaction inputs already describe the renderer's needs.

Presentation refinements belong inside the existing shared chart implementation rather than introducing a generalized chart-style configuration layer.

The visual hierarchy should be:

1. Data series
2. Reference lines
3. Gridlines

The existing press/drag interaction remains unchanged. The marker continues to show the UTC timestamp and series values, with presentation adjusted only as needed to remain compact and readable.

## Errors and edge cases

No new error-handling path is required because fetching, persistence, and data processing are unchanged.

Charts must continue to handle:

- sparse data,
- gaps and disconnected segments,
- data beginning later than the selected timeframe,
- missing series values without interpolation,
- very large or small values using the existing formatting,
- reference-line behavior without introducing unrelated scaling changes.

On narrow Pixel-sized screens, legibility takes priority over maximizing tick count. Fewer well-spaced labels/gridlines are preferable to crowded labels.

No fallback or lifecycle behavior will touch the Aurora globe.

## Testing

Implementation follows strict TDD.

Focused regression coverage should verify presentation decisions that can be tested reliably, including:

- Space Weather charts request the new 240dp height.
- Existing timeframe/domain behavior remains unchanged.
- Sparse data does not alter the requested domain.
- Shared chart reference-line and inspection configuration remains established.

Purely visual properties such as exact grid weight, spacing, and marker compactness should not be protected with brittle pixel-perfect assertions. Existing chart tests plus targeted structural assertions should protect behavior.

The full Android test suite must pass in CI.

Final acceptance is physical-device testing on the Pixel at 1h, 3h, 12h, and 2d.

The polish pass must not change filtering, reduction, domain fitting, series semantics, or interaction behavior.
