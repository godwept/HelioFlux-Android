# Solar Activity Event Layout Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal and Success Criteria

Reorder the Solar Activity content so the chart-driven science sections stay together and the recent event lists follow them.

The content order should be:

1. X-Ray Activity — probabilities + X-Ray chart
2. Particle Environment — ACE EPAM chart
3. Recent Flares — full-width card
4. Recent CMEs — full-width card

The two recent-event cards should keep their current styling and content, but move from a side-by-side horizontal layout to a single-column vertical stack.

Success means the charts read as one continuous science section at the top, followed by spacious, readable flare and CME lists.

## Scope

### In scope

- Move Particle Environment directly below X-Ray Activity.
- Move the recent-event content below both charts.
- Replace the current side-by-side flare/CME `Row` with a vertical stack.
- Make Recent Flares a full-width card.
- Make Recent CMEs a full-width card.
- Preserve existing card styling.
- Preserve existing event-row styling.
- Preserve the CME Details action.
- Preserve empty/loading/failure states.
- Preserve event ordering.
- Apply the same logical ordering in compact and expanded layouts through the existing shared science-item structure.

### Out of scope

- Redesigning the flare or CME cards.
- Changing event data sources or refresh behavior.
- Changing chart sizes or chart styling.
- Changing X-Ray or Particle Environment data presentation.
- Changing navigation or bottom-bar behavior.
- Adding collapsible sections, tabs, or new interaction patterns.

## Data and State

No new data or state is required.

The existing `SolarActivityUiState` already provides:

- `xray`
- `probabilities`
- `epam`
- `flares`
- `cmes`

This change is presentation-only.

- `ParticleSection(state.epam, chartDomain)` moves ahead of the recent-event cards.
- The existing flare/CME repository states continue to flow into `FlareList` and `CmeList` unchanged.
- No ViewModel changes.
- No repository/database changes.
- No new remembered Compose state.
- No changes to refresh/freshness logic.

## Interfaces and Composition Boundary

Keep the change inside `SolarActivityScreen.kt`.

The shared `solarActivityScienceItems(...)` composition should own the overall order:

1. `XrayActivitySection`
2. Particle Environment heading
3. `ParticleSection`
4. recent-event section

The existing `SolarEventCards(...)` composition should render vertically instead of horizontally:

- full-width Recent Flares card
- full-width Recent CMEs card

`FlareList` and `CmeList` remain unchanged and continue receiving the same states and callbacks.

Rename `SolarEventCards` only if the existing name becomes materially misleading during implementation; otherwise leave it unchanged.

No public API changes and no new parameters are required.

## Error Handling and Edge Cases

Existing state handling remains unchanged:

- `Loading` keeps the current loading text.
- `Empty` keeps `No recent flares` / `No recent CMEs`.
- `Failure` with cached data keeps the cached-data message and rows.
- `Failure` without cached data keeps the unavailable message.
- CME Details buttons continue to work unchanged.
- Long event lists grow naturally inside the existing `LazyColumn`.
- Do not add fixed heights.
- Do not add nested vertical scrolling.
- Preserve Particle Environment's existing `animateInitial = false` behavior.

The implementation rule is: change placement and stacking only, not state behavior.

## Testing Strategy

Update focused Solar Activity UI/composition coverage to verify:

- Particle Environment appears before Recent Flares and Recent CMEs.
- Recent Flares and Recent CMEs both render.
- The event cards are stacked vertically rather than sharing a horizontal `Row`.
- Each event card uses the full available content width.
- Existing flare rows still render.
- Existing CME rows still render.
- Existing empty/loading/failure text still renders.
- Existing CME Details behavior remains available.
- Particle Environment continues to use the current EPAM chart path.
- The existing no-reentry-animation behavior remains intact.
- Compact and expanded layouts continue to use the same shared `solarActivityScienceItems(...)` ordering.

Run focused Solar Activity tests followed by the normal Android CI suite.

Physical Pixel verification should confirm the visible order:

**X-Ray Activity → Particle Environment → Recent Flares → Recent CMEs**

with both recent-event cards stacked and easy to scan.

## Open Questions

None.
