# Section Heading Standardization and Geomagnetic GOES Layout Design

**Date:** 2026-09-21
**Status:** Approved

## Goal
Create a single reusable HelioFlux section-heading treatment for the Home and Space Weather screens that improves visual hierarchy and consistency without competing with the HELIOFLUX masthead, chart titles, or metric data. As part of the same focused Space Weather refinement, move the GOES Magnetometer chart into the Geomagnetic Activity section and make it use a fixed two-day window so the Solar Wind timeframe selector applies only to Solar Wind content.

## Success Criteria
- [ ] `Current Conditions`, `NOAA Forecast`, `Solar Wind`, and `Geomagnetic Activity` use the same reusable section-heading component.
- [ ] The heading uses the normal app UI font, approximately 20sp SemiBold, title case, white/on-background text, slight positive tracking, and a restrained Solar Orange vertical accent.
- [ ] Section-heading spacing is standardized across Home and Space Weather.
- [ ] The heading treatment adds no glow, background card, heavy divider, or uppercase transformation.
- [ ] Space Weather ordering places GOES Magnetometer after Hemispheric Power inside Geomagnetic Activity.
- [ ] GOES Magnetometer no longer changes when the Solar Wind `1h / 3h / 12h / 2d` selector changes.
- [ ] GOES Magnetometer uses a fixed two-day data window and chart domain, matching the Kp chart's time horizon.
- [ ] Existing GOES refresh, series, legend, loading, chart interaction, and data behavior remain intact apart from the timeframe decoupling.

## Scope
**In scope:**
- Introduce one reusable HelioFlux section-heading component for Home and Space Weather.
- Apply it to `Current Conditions`, `NOAA Forecast`, `Solar Wind`, and `Geomagnetic Activity`.
- Use normal app typography at 20sp SemiBold with title-case text, slight positive letter spacing, white/on-background text, and a small Solar Orange vertical accent.
- Standardize immediate heading spacing so sections have a consistent visual rhythm.
- Move GOES Magnetometer beneath Hemispheric Power in the Geomagnetic Activity section.
- Give GOES Magnetometer a fixed two-day filter and fixed two-day chart domain independent of `state.timeframe`.
- Preserve the existing GOES refresh source, chart metadata, series mapping, legend labels, loading state, and chart interactions.

**Out of scope:**
- Redesigning metric pills, chart cards, axes, legends, or chart rendering.
- Changing Kp or Hemispheric Power data behavior.
- Changing Solar Wind timeframe behavior for Bz/Bt, density, speed, or temperature.
- Applying the shared heading to every remaining app screen in this pass.
- Changing the HELIOFLUX masthead or its Orbitron treatment.
- Rewriting the chart or repository/data pipeline.

## Design

### Shared section heading
The reusable section heading uses the app's normal UI font rather than Orbitron. The text is 20sp, SemiBold, title case, and uses the normal `onBackground` color. Letter spacing is slightly positive, approximately 0.2-0.3sp, to add subtle polish without turning the heading into a technical caption.

A narrow Solar Orange vertical accent sits immediately to the left of the title. The accent is approximately 3dp wide and 18-20dp tall with mildly rounded ends. Accent and title are vertically centered. The heading has no glow, shadow, background card, horizontal divider, or uppercase transformation.

The heading component owns its immediate spacing. Use approximately 20dp before a new section and approximately 8dp between the heading and its first content item, while avoiding redundant top padding when the heading is already the first item in its container.

### Home application
Apply the shared heading treatment to:
- `Current Conditions`
- `NOAA Forecast`

Do not alter the Home metric cards, forecast cards, solar hero, or HELIOFLUX masthead. The shared heading should provide stronger hierarchy than the current plain `titleMedium` labels while remaining visually subordinate to the masthead.

### Space Weather application
Apply the same shared heading treatment to:
- `Solar Wind`
- `Geomagnetic Activity`

The Solar Wind section retains its current metrics, timeframe selector, and four timeframe-controlled charts in this order:
1. Solar Wind heading
2. Solar Wind metrics
3. `1h / 3h / 12h / 2d` timeframe selector
4. Bz / Bt
5. Density
6. Speed
7. Temperature

The Geomagnetic Activity section becomes:
1. Geomagnetic Activity heading
2. Kp
3. Hemispheric Power
4. GOES Magnetometer

GOES does not receive an extra subsection heading because the chart card already identifies itself.

### GOES timeframe behavior
The Solar Wind timeframe selector belongs only to the Solar Wind section. It continues to control Bz/Bt, density, speed, and temperature exactly as it does now.

GOES Magnetometer is decoupled from `state.timeframe`. Its series are filtered to the most recent two days and its X-axis domain is fixed to the same two-day interval. This matches the Kp chart's time horizon and makes the Geomagnetic Activity section conceptually consistent without changing Hemispheric Power behavior.

The GOES chart's existing refresh action, refresh source, chart metadata, series styling, legend labels, loading state, and chart interactions remain unchanged.

### State and data boundaries
No new persistent state is required. The selected Solar Wind timeframe remains existing screen state and continues to affect only the Solar Wind charts. GOES derives a fixed two-day presentation from its existing data using the current time, without introducing a second user-selectable timeframe.

No repository or network-layer changes are required by this design.

### Error handling and edge cases
- If GOES has less than two days of available samples, render the available samples within the fixed two-day domain using existing chart behavior.
- If GOES has no data, preserve the existing empty/loading behavior rather than introducing new UI.
- Moving GOES must not change its refresh routing.
- Compact and expanded Space Weather layouts must preserve the same logical block ordering, with the Aurora globe placement remaining unchanged.
- Heading spacing must avoid doubled top padding when a heading is the first visible item in a container.

## Testing Strategy
Use focused TDD regression coverage.

- Verify Home renders `Current Conditions` and `NOAA Forecast` through the shared heading treatment.
- Verify Space Weather renders `Solar Wind` and `Geomagnetic Activity` through the same reusable heading treatment.
- Verify the heading's typography and Solar Orange accent contract at the component level where practical.
- Verify Space Weather block ordering is `Solar Wind -> metrics/timeframe -> Bz/Bt -> Density -> Speed -> Temperature -> Geomagnetic Activity -> Kp -> Hemispheric Power -> GOES`.
- Verify changing the Solar Wind timeframe still changes Bz/Bt, Density, Speed, and Temperature presentation.
- Verify changing the Solar Wind timeframe does not change GOES presentation.
- Verify GOES filters to a fixed two-day range and uses a fixed two-day chart domain.
- Verify Kp and Hemispheric Power behavior remains unchanged.
- Verify GOES refresh remains wired to the existing GOES refresh source.
- On a physical device, confirm the shared heading treatment is consistent and restrained in portrait and landscape, GOES reads as part of Geomagnetic Activity, and changing the Solar Wind timeframe leaves GOES unchanged.

## Open Questions
None. All design decisions required for implementation are approved.
