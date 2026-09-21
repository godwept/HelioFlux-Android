# Solar Activity Chart Spacing and Flare Probability Placement Design

**Date:** 2026-09-21  
**Status:** Approved

## Goal

Refine the Solar Activity science sections after physical Pixel testing by removing the large blank bands above the X-Ray Activity and Particle Environment charts and by moving the C/M/X flare probability pills into the X-Ray Activity section where they provide clearer context.

## Success Criteria

- [ ] X-Ray Activity no longer has a large blank band between its heading/content and the visible chart.
- [ ] Particle Environment no longer has a large blank band between its heading and the visible chart.
- [ ] C/M/X flare probability pills are removed from the top of the Solar Activity screen.
- [ ] C/M/X pills appear directly below the X-Ray Activity heading and above the X-Ray chart.
- [ ] Press/drag chart inspection remains available.
- [ ] The inspection tooltip floats near the selected data point instead of reserving vertical space above the plot.
- [ ] Existing chart domains, axes, reference lines, series colors, reduction, and data behavior remain unchanged.
- [ ] Existing imagery, event cards, fullscreen behavior, ENLIL, refresh behavior, repositories, and ViewModels remain unchanged.

## Scope

**In scope:**
- Move the existing flare-probability pill row into the X-Ray Activity section.
- Remove the duplicate/top-level probability row from compact and expanded Solar Activity feeds.
- Change the shared line-chart inspection marker from a top-reserved placement to a floating placement near the selected point.
- Preserve the current compact visual spacing between section title, pills, and chart.
- Preserve Particle Environment's existing section structure while reclaiming the chart's reserved top space.
- Add focused regression coverage for probability placement and chart marker placement.

**Out of scope:**
- Chart domain changes.
- Chart height changes.
- Axis, gridline, legend, series-color, reduction, reference-line, or timeframe changes.
- Changes to X-Ray or ACE EPAM data models or repositories.
- Changes to flare probability calculation/data.
- Changes to imagery, fullscreen, ENLIL, flare/CME cards, navigation, or refresh.
- PWA changes.

## Design

### Root cause

Physical-device testing shows that the remaining large gaps are not primarily created by section-heading padding.

The shared `HelioFluxLineChart` configures a multi-line Vico inspection marker using the default top placement. The marker is sized according to the number of series so the X-Ray chart reserves a larger top area than the Particle Environment chart. This reserved marker area visually appears as empty space before the plot.

The fix should address this at the shared chart presentation level rather than applying negative margins or per-screen offsets.

### Chart inspection marker

Change the shared line-chart marker placement from Vico's default top placement to a floating placement around the selected chart point.

Expected behavior:

- No large reserved tooltip band above the chart when idle.
- Press/drag inspection still shows the same timestamp and series-value content.
- The tooltip appears near the selected data point.
- The guideline and marker interaction policy remain unchanged.
- All charts using `HelioFluxLineChart` receive the same improved marker behavior.

Do not alter the marker's content formatting, series lookup, colors, line count, or gesture handling.

### X-Ray Activity composition

The X-Ray section should read, top to bottom:

```text
X-Ray Activity
[C probability] [M probability] [X probability]
[X-Ray chart]
```

The existing `FlareProbabilityBadges` component should be reused unchanged.

The probability pills should be rendered inside the X-Ray section immediately after the heading and before the X-Ray state/chart content.

Use compact local spacing so the pills visually belong to the X-Ray section and the chart begins closely beneath them.

### Top-level probability strip

Remove the top-level `FlareProbabilityBadges(state.probabilities)` item from both:

- compact Solar Activity feed;
- expanded Solar Activity right-side science feed.

The screen's status-bar inset handling remains in place because it still protects whatever content becomes the first visible item.

### Particle Environment composition

Keep the existing Particle Environment heading and ACE EPAM section structure.

Do not introduce Particle-specific spacing hacks. The shared marker-placement change should reclaim the reserved top band and allow the existing heading spacing to read correctly.

If physical verification later reveals a separate Compose-layout gap after the marker fix, treat that as a distinct defect and debug it separately rather than pre-emptively adding offsets.

## State and Interfaces

No new state is required.

`FlareProbabilityBadges` continues to receive the existing `RepositoryState<FlareProbabilities>`.

No ViewModel, repository, model, parser, networking, persistence, or navigation interfaces change.

The shared chart API should remain unchanged. Marker placement is an internal presentation detail of `HelioFluxLineChart`.

## Error Handling and Edge Cases

- If flare probabilities are loading, empty, failed, cached, or available, the existing badge component continues to display its current state behavior inside the X-Ray section.
- If X-Ray samples are unavailable, the X-Ray heading and probability pills remain visible above the existing X-Ray state message.
- If Particle Environment data is unavailable, the existing state message remains unchanged.
- Floating inspection labels may reposition around the touched point according to Vico's placement behavior; this is acceptable and preferred to permanently reserving top chart space.
- No negative offsets or clipping should be introduced to force charts upward.

## Testing Strategy

Use strict TDD for implementation.

Automated coverage should verify:

- the Solar Activity screen no longer renders flare probability badges as standalone top-level compact/expanded items;
- the X-Ray Activity section receives probability state and renders `FlareProbabilityBadges` between its heading and chart/state content;
- the shared chart configures the inspection marker for floating around-point placement;
- existing marker formatter, line-count, guideline, and interaction-policy contracts remain unchanged;
- existing X-Ray 72-hour domain and reference-line tests remain green;
- existing ACE EPAM chart behavior remains green;
- existing Solar Activity imagery, event-card, partial-data, and refresh tests remain green.

After implementation, run the full Android unit suite, instrumentation-test compilation, lint, and debug APK build, then monitor Android CI to green.

## Physical Pixel Acceptance

Verify on the Pixel:

- X-Ray Activity title, C/M/X pills, and chart form one compact visual group.
- The large blank band above the X-Ray plot is gone.
- The large blank band above the Particle Environment plot is gone.
- Press/drag inspection still works on both charts.
- Tooltip content is readable when floating near the selected data point.
- Probability pills no longer appear at the top of the Solar Activity screen.
- No regressions appear in imagery, flare/CME cards, fullscreen behavior, ENLIL, or refresh.

## Open Questions

None. All design decisions required for planning are approved.
