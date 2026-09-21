# Home Landscape and Conditions Simplification Design

**Date:** 2026-09-21  
**Status:** Approved

## Goal

Simplify the Home screen's current-conditions presentation and improve landscape behavior so Home follows the same general split-screen model already proven on Space Weather.

The Home screen should:
- remove the visible `Current Conditions` section title;
- remove the geomagnetic-status sentence below the metric pills;
- show only the existing Kp, Bz, and Wind pills with comfortable vertical breathing room;
- keep portrait behavior simple and linear;
- use a fixed solar hero on the left in landscape;
- use an independently scrollable content pane on the right in landscape;
- keep the HELIOFLUX masthead fixed and always visible above both landscape panes.

## Success Criteria

- [ ] Home no longer renders the `Current Conditions` title.
- [ ] Home no longer renders any geomagnetic-status sentence beneath the pills.
- [ ] The existing Kp, Bz, and Wind pills remain visually and behaviorally unchanged.
- [ ] The pill row has 12dp vertical padding above and below.
- [ ] Portrait order remains: HELIOFLUX masthead -> solar hero -> pills -> NOAA Forecast.
- [ ] Landscape keeps HELIOFLUX fixed at the top of the screen.
- [ ] Landscape keeps the solar hero fixed in the left pane.
- [ ] Landscape places the pills and NOAA Forecast content in a vertically scrollable right pane.
- [ ] Landscape uses approximately the same 43% / 57% left-right split as Space Weather.
- [ ] NOAA Forecast continues to use the shared section-heading treatment.
- [ ] No Space Weather behavior or shared-heading styling changes are included.

## Scope

### In scope
- Simplify `CurrentConditions` to render only the existing Kp, Bz, and Wind pills.
- Remove the `Current Conditions` heading.
- Remove all geomagnetic-status text variants:
  - unavailable;
  - storm conditions;
  - below storm level.
- Add 12dp vertical padding around the pills.
- Refactor Home expanded/landscape layout so:
  - HELIOFLUX remains fixed at the top;
  - the solar hero is fixed in the left pane;
  - the right pane scrolls independently;
  - the right pane contains the pills followed by NOAA Forecast.
- Reuse the existing Space Weather expanded layout pattern where practical.
- Preserve existing Home pull-to-refresh behavior in a way that remains natural with the split layout.

### Out of scope
- Changing pill colors, dimensions, labels, values, spacing between pills, shadows, borders, or click navigation.
- Changing the solar hero animation/loading/gesture pipeline.
- Changing NOAA Forecast card styling or behavior.
- Changing `HelioFluxSectionHeading` styling.
- Changing Space Weather.
- Changing repositories, ViewModels, networking, or data models.
- Redesigning the HELIOFLUX masthead.

## Current State

The current Home screen uses one `LazyColumn` for both portrait and expanded layouts.

In compact mode:
1. HELIOFLUX masthead
2. Solar hero
3. Current Conditions heading
4. Kp/Bz/Wind pills
5. Geomagnetic-status sentence
6. NOAA Forecast

In expanded mode:
- HELIOFLUX masthead remains above the main content.
- Solar hero and Current Conditions share one horizontal row.
- NOAA Forecast appears below that row and therefore spans the full content width.
- The entire Home content scrolls together.

This differs from Space Weather's expanded mode, where the visual hero remains fixed in a left pane and the data/content side scrolls independently.

## Design

### Current conditions simplification

Keep the existing `CurrentConditions` composable as an implementation boundary to avoid unnecessary structural changes.

Its visible content becomes only:

```text
Kp | Bz | Wind
```

The metric row receives:
- 12dp padding above;
- 12dp padding below.

Remove:
- `HelioFluxSectionHeading("Current Conditions", ...)`;
- the 8dp spacer used before the status sentence;
- the geomagnetic-status `Text`;
- any imports used only by the removed heading/status text.

Do not modify the existing `Metric` composable.

### Portrait Home

Portrait remains a single vertically scrolling flow:

1. HELIOFLUX masthead
2. Solar hero
3. Kp/Bz/Wind pills with 12dp top and bottom padding
4. NOAA Forecast heading
5. Forecast cards

The Home-wide spacing outside the simplified CurrentConditions container should remain consistent with the existing compact layout unless the 12dp internal pill padding would create obvious duplicated spacing. Avoid stacking redundant vertical padding.

### Landscape Home

Landscape/expanded Home uses a fixed masthead plus a split content area below it.

Structure:

```text
HELIOFLUX masthead (fixed, full width)

| fixed left pane       | scrollable right pane |
| solar hero            | Kp / Bz / Wind        |
|                       | NOAA Forecast          |
|                       | forecast cards         |
```

The masthead must remain outside the scrolling right pane so it stays visible at all times.

The content area below the masthead uses approximately the same proportions as Space Weather:
- left pane: 43%;
- right pane: 57%.

#### Left pane
- contains the existing solar hero;
- fills the available height below the masthead;
- does not scroll vertically;
- preserves existing hero behavior and transforms;
- does not introduce new hero state or animation logic.

#### Right pane
- fills the available height below the masthead;
- scrolls vertically independently of the left pane;
- begins with the Kp/Bz/Wind pills;
- shows NOAA Forecast beneath the pills;
- keeps forecast cards and expansion behavior unchanged;
- uses the existing Home horizontal padding and appropriate bottom content padding.

### Pull-to-refresh

In landscape, follow the existing Space Weather pattern rather than making the fixed visual pane scroll.

The right content pane should own the scrollable pull-to-refresh surface. The masthead and solar hero remain stationary during refresh gestures.

Portrait may retain the existing whole-screen pull-to-refresh structure if that remains simpler and behaviorally unchanged.

Do not create separate refresh state or alter repository refresh behavior.

## State and data

No new persistent or screen state is required.

The change is layout-only:
- existing Home conditions data still feeds the pills;
- existing forecast state still feeds NOAA Forecast;
- existing solar hero frame state remains unchanged;
- existing destination callbacks remain unchanged.

Removing the geomagnetic-status sentence does not remove any underlying Kp data or alert logic; it only removes that secondary text presentation from Home.

## Edge Cases

- Missing Kp/Bz/Wind data should continue to render the current em-dash fallback in the corresponding pill.
- Pills remain clickable even when a value is unavailable.
- Expanded layout must not accidentally create nested competing vertical scroll containers.
- The fixed masthead must not overlap the split content below it.
- The hero must remain fully interactive and must not receive scroll gestures intended for the right pane.
- Long NOAA Forecast content must remain fully reachable in the right pane.
- Landscape rotation should not alter the hero's existing playback/loading pipeline.
- The portrait path should not inherit expanded-only fixed-pane behavior.

## Testing Strategy

Use focused TDD.

### CurrentConditions coverage
- Verify `Current Conditions` is no longer present in the source/presentation.
- Verify all geomagnetic-status strings are absent.
- Verify the Kp, Bz, and Wind metrics remain.
- Verify the CurrentConditions container applies 12dp vertical padding.
- Preserve existing metric click/navigation coverage.

### Home layout coverage
- Verify compact Home still contains:
  - masthead;
  - solar hero;
  - current-condition pills;
  - NOAA Forecast.
- Verify expanded Home:
  - places the masthead outside the split content;
  - uses a fixed left hero pane;
  - uses a vertically scrollable right content pane;
  - places CurrentConditions before NOAA Forecast in the right pane;
  - uses the same approximate 43% / 57% split as Space Weather.
- Verify the fixed hero pane does not live inside the right-side scroll container.
- Verify NOAA Forecast still uses `HelioFluxSectionHeading`.

### Regression verification
Run the full unit suite, lint, and debug APK build after the focused tests pass.

Physical-device acceptance on Pixel:
- portrait shows only the pills between hero and NOAA Forecast;
- pills have comfortable 12dp vertical breathing room;
- landscape masthead remains visible at all times;
- landscape solar hero remains fixed on the left;
- right side scrolls independently through pills and forecast content;
- hero animation/gestures remain unchanged;
- pull-to-refresh remains usable;
- no Home content is clipped or unreachable.

## Open Questions

None. All design decisions required for implementation are approved.
