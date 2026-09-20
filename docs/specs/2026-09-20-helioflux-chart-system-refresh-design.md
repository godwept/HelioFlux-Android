# HelioFlux Chart System Refresh Design

**Date:** 2026-09-20
**Status:** Approved

## Goal

Turn the HelioFlux Android charts from passive sparklines into readable monitoring instruments while fixing the current Space Weather timeframe/viewport bug. The shared Vico chart wrappers should provide consistent axes, gridlines, semantic colors, reference guides, touch inspection, deterministic time domains, and safe dense-data reduction across both Space Weather and Solar Activity without changing repository/network behavior or disturbing the stable Aurora globe and imagery pipelines.

## Success Criteria

- [ ] Space Weather 1h / 3h / 12h / 2d selections always display the complete selected UTC window from `now - duration` through `now`.
- [ ] Changing Space Weather timeframe immediately re-fits all shared-timeframe charts; no stale zoom/scroll viewport remains.
- [ ] Charts do not support manual pinch zoom or horizontal pan.
- [ ] Vertical screen scrolling remains natural when a gesture becomes primarily vertical.
- [ ] Line charts provide transient tap-and-drag inspection with a vertical marker, UTC timestamp, and available series values.
- [ ] X and Y axes, compact tick labels, and subtle gridlines make time and magnitude readable.
- [ ] Rendered line colors match their legends exactly.
- [ ] Bz=0 and X-Ray C/M/X levels render as reference guides rather than ordinary measured series.
- [ ] High-cadence dense traces use min/max-preserving reduction only when necessary, while preserving meaningful extrema and real timestamps.
- [ ] Kp and Hemispheric Power remain raw.
- [ ] Solar Activity X-Ray and ACE EPAM charts receive the shared presentation improvements while retaining their existing fixed 72-hour window.
- [ ] Existing chart, screen, and repository behavior outside this scope remains unchanged.
- [ ] New and existing tests pass, CI is green, and the defined physical-device checks succeed.

## Scope

### In scope

- Repair shared chart viewport behavior so Space Weather timeframe charts use an explicit `[now - timeframe, now]` X-domain.
- Disable chart zoom and horizontal pan.
- Add readable UTC X-axis labels tailored to the visible window.
- Add compact Y-axis ticks with sensible metric-appropriate range handling and padding.
- Add subtle horizontal and vertical Cartesian gridlines.
- Add transient tap-and-drag chart inspection.
- Make rendered series colors use the same semantic styling as their legends.
- Replace fake reference-series rendering with real chart reference guides.
- Add min/max-preserving render reduction for dense high-cadence line data.
- Apply shared chart presentation to Bz/Bt, density, speed, temperature, GOES magnetometer, Hemispheric Power, Solar Activity X-Ray, and ACE EPAM.
- Give Kp the same readable axes/grid/time-context language without treating it as continuous high-frequency data.
- Preserve the existing chart-card hierarchy and approximately the current chart heights unless axes require a small adjustment.

### Out of scope

- Manual chart zoom or horizontal panning.
- Moving-average smoothing, interpolation, or replacement of measured samples with synthetic trend values.
- New latest/min/max summary rows.
- Repository, networking, persistence, parser, retention, or refresh-cadence changes.
- Adding the Space Weather timeframe selector to Solar Activity.
- Aurora globe rendering, interaction, lifecycle, or Filament resource changes.
- Home screen, navigation, imagery pipelines, or unrelated Space Weather UI changes.
- Replacing Vico or performing a broad chart-library rewrite.
- Downsampling Kp.
- Downsampling Hemispheric Power unless later evidence proves it necessary.

## Design

### Root cause and architectural direction

The current Space Weather filtering logic is already correct: each mapper filters source samples by the selected `Timeframe`. The viewport bug exists at the presentation boundary because `HelioFluxLineChart` receives only the filtered points and no explicit timeframe/domain information, while the remembered Vico chart state owns its own scroll/zoom viewport. Timeframe changes therefore update model data without guaranteeing a fresh full-window viewport.

The shared chart wrappers should own chart presentation only. Screens continue to own scientific data and the requested time window. A line chart receives an explicit X-domain, series definitions, Y-axis configuration, reference guides, and value-formatting metadata. The chart has no persistent user-controlled zoom or horizontal scroll state.

### Time domains

Space Weather charts use an explicit domain of:

`[now - selectedTimeframe.durationMillis, now]`

The domain is independent of the earliest/latest sample actually present, so sparse data remains visibly sparse instead of stretching to fill the chart.

Solar Activity X-Ray and ACE EPAM retain their existing fixed 72-hour data window and use:

`[now - 72h, now]`

Solar Activity does not gain the Space Weather timeframe selector.

### Shared series model

A chart series retains:

- real timestamp/value observations;
- a human-readable label;
- semantic style/color identity;
- metric-specific value formatting when needed.

The same semantic style used by the legend is used by the actual Vico line renderer. Reference guides are separate chart metadata and never appear as measured series.

### Y-axis behavior

Normal linear charts derive their visible range from valid observations inside the supplied X-domain and add modest padding so extrema do not touch the chart edge.

Zero is not forced into every metric. It is included when scientifically or visually appropriate, such as the Bz reference context.

Constant or nearly constant values receive a nonzero padded range.

Kp uses its natural bounded scale rather than a tightly auto-scaled range.

X-Ray retains its existing fixed scientific flux range and logarithmic semantics. Its implementation may continue using the current log-transformed plotting values internally, but axis and marker formatting must expose meaningful flux values rather than raw `log10` numbers.

ACE EPAM retains scientific/exponential value formatting.

### Axes and grid

Line and bar charts share a restrained instrument-like visual language:

- left Y-axis with compact labels;
- bottom UTC X-axis;
- subtle horizontal and vertical gridlines;
- low-contrast axis/guideline styling that does not compete with the data.

Suggested X-axis formatting:

- 1h / 3h: `HH:mm`;
- 12h: `HH:mm` with fewer ticks;
- 2d / 72h: date plus time at wider intervals, such as `Sep 20 12:00`.

Exact tick placement may be delegated to Vico as long as the result remains legible and the explicit domain is preserved.

### Reference guides

Bz=0 becomes a thin neutral/dashed horizontal guide rather than an added two-point data series.

Solar Activity X-Ray C/M/X levels become proper labeled reference guides. Reference guides:

- do not appear in legends;
- do not participate in marker values;
- do not influence measured-series identity;
- remain visually secondary to actual data.

### Touch inspection

Line charts support transient tap-and-drag inspection.

On touch:

- activate one vertical marker/crosshair;
- snap inspection to the nearest real observation timestamp;
- display UTC timestamp and all series values available at that timestamp;
- use each series' semantic color in the marker presentation;
- omit unavailable values rather than interpolating them.

Horizontal movement continues inspection. If the gesture becomes primarily vertical, inspection cancels/yields so the parent screen can scroll naturally.

Releasing or cancelling the gesture removes the marker. A material domain/data change, including Space Weather timeframe changes, also clears any active marker.

### Dense-data reduction

Raw repository samples remain authoritative. Reduction is a render-only transformation implemented as a pure deterministic Kotlin function independent of Compose and Vico.

Sparse data is passed through unchanged.

When a series is dense relative to useful horizontal display resolution:

1. Divide the current visible time domain into chronological buckets.
2. For each bucket, retain the real minimum and maximum observations.
3. Emit those retained observations in timestamp order.
4. Preserve meaningful first/last boundary observations.
5. Deduplicate cases where one observation is both the minimum and maximum.
6. Never average, interpolate, synthesize, or move timestamps.

Eligible dense series:

- Space Weather Bz/Bt;
- plasma density/speed/temperature;
- GOES magnetometer;
- Solar Activity X-Ray;
- Solar Activity ACE EPAM.

Raw-only series:

- Kp;
- Hemispheric Power, unless actual future point counts demonstrate a need to revisit this.

The reduction threshold should be tied to useful render resolution/point count rather than a scientific averaging interval. The implementation should keep the policy simple and testable.

### Nulls, gaps, and invalid values

Missing/null measurements remain gaps and are not presented as measured values.

Non-finite values are ignored for range and rendering calculations.

Out-of-order input is normalized into chronological render order.

Duplicate timestamps are handled deterministically without crashing or inventing observations.

Marker inspection reports only values actually available at the selected timestamp.

### Kp behavior

Kp keeps its discrete 3-hour character and existing short-window fallback behavior. It gains explicit time-domain context, axes, and subtle gridlines through the shared bar-chart presentation, but no line interpolation and no downsampling.

### Solar Activity behavior

The Solar Activity screen continues to show its existing fixed 72-hour X-Ray and ACE EPAM data.

X-Ray retains:

- the existing scientific range;
- logarithmic behavior;
- C/M/X thresholds;
- meaningful scientific/exponential display formatting.

ACE EPAM retains its current three particle series and exponential-style value formatting.

Neither chart gains the Space Weather timeframe selector.

### Failure and edge-case behavior

- Empty/unavailable states remain screen-level behavior; the shared chart does not invent placeholder data.
- Sparse charts still occupy the full requested X-domain.
- Constant series receive visible Y padding.
- Reference guides never masquerade as data.
- Marker values are never interpolated.
- Gesture cancellation leaves parent scrolling usable.
- Dense-data reduction happens before data reaches Vico and adds no background jobs, caches, or lifecycle machinery.
- Repository fetch windows, retention, refresh behavior, and parsers remain unchanged.

## Testing Strategy

### Pure unit tests

Add focused tests for min/max-preserving reduction:

- sparse input passes through unchanged;
- dense input is reduced;
- per-bucket minima and maxima survive;
- original timestamps and values are preserved exactly;
- first/last meaningful observations survive;
- output remains chronological;
- duplicate timestamps are deterministic;
- min=max buckets do not duplicate unnecessarily;
- null/non-finite values do not create fabricated samples;
- output respects the defined reduction bound.

Add domain/range helper tests:

- 1h / 3h / 12h / 2d produce exactly `[now - duration, now]`;
- Solar Activity produces exactly a 72-hour domain;
- timeframe changes change the domain rather than merely filtering samples;
- Y-range handling covers constant, positive-only, negative-only, zero-crossing, and Bz-reference cases;
- X-Ray retains its fixed/logarithmic behavior.

### Chart configuration/presentation tests

Where practical without tightly coupling tests to Vico internals, verify:

- semantic series styles drive rendered colors;
- UTC axis formatting matches the requested window;
- reference guides remain separate from measured series;
- normal/scientific/X-Ray formatters expose meaningful values;
- zoom/pan is disabled;
- marker values come only from real observations.

### Existing screen regression tests

Expand Space Weather tests so all relevant charts receive the selected explicit timeframe/domain.

Retain Kp sparse-window fallback tests while adding domain verification.

Expand Solar Activity regression coverage so:

- X-Ray remains 72 hours with its fixed log range and C/M/X guides;
- ACE EPAM remains 72 hours;
- no Space Weather timeframe selector is introduced;
- both charts use the improved shared presentation.

### Physical-device verification

After implementation tests and CI are green:

1. Open Space Weather at the default timeframe and verify charts show the whole window rather than appearing maximally zoomed.
2. Cycle `1h -> 3h -> 12h -> 2d -> 1h`; every chart immediately re-fits.
3. Verify axes and gridlines remain readable at each window.
4. Drag horizontally on several line charts and verify transient marker inspection with UTC/value information.
5. Start a vertical swipe on a chart and verify the screen scrolls normally.
6. Confirm Bz line color matches its legend and the zero guide is visually distinct.
7. Inspect 2d Bz/GOES traces and confirm sharp excursions remain visible rather than smoothed away.
8. Check Kp and Hemispheric Power for sensible sparse/discrete behavior.
9. Open Solar Activity and verify X-Ray and ACE EPAM use the improved shared chart presentation while retaining their fixed 72-hour context.
10. Confirm X-Ray C/M/X thresholds and log-scale presentation remain correct.
11. Scroll repeatedly through both screens to catch rendering/lifecycle regressions.
12. Recheck the Aurora globe afterward to ensure chart work did not disturb its already-stable behavior.

## Open Questions

None. The interaction model, shared-scope boundary, dense-data reduction strategy, Solar Activity scope, and testing expectations were approved during brainstorming.
