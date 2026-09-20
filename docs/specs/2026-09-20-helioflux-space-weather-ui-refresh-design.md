# HelioFlux Space Weather UI Refresh Design

**Date:** 2026-09-20  
**Status:** Approved

## Goal and success criteria

Reconstruct the native Android Space Weather screen so it feels like a deliberate, information-dense space-weather dashboard rather than a collection of generic Material components. Use `godwept/HelioFlux-web` as the primary visual and behavioral reference while allowing a native, responsive Android interpretation.

The intended information hierarchy is:

**Aurora hero → Solar Wind → live metric pills → timeframe control → solar-wind/GOES charts → Geomagnetic Activity → Kp/Hemispheric Power.**

On Pixel-sized screens, the Aurora globe is the dominant opening visual, comparable in prominence to the PWA. Scrolling progressively exposes increasingly detailed data rather than giving every component equal visual weight.

Preserve the working Space Weather functionality and data pipeline. The one approved behavioral parity change is that **12h becomes the default timeframe**.

Success means noticeably stronger visual hierarchy, spacing, typography, chart presentation, responsive composition, and PWA parity while retaining the existing working data behavior and globe lifecycle.

## Scope

### In scope

- Reconstruct the Space Weather Compose hierarchy and responsive layout.
- Make the Aurora globe a large, prominent opening hero.
- Add clear Solar Wind and Geomagnetic Activity sections.
- Replace the three current metric cards with compact PWA-inspired colored pills for Bz, Speed, and Density.
- Replace four independent timeframe filter chips with a joined native segmented control.
- Change the initial timeframe from 3h to 12h.
- Redesign chart containers, headers, typography, spacing, series identification, and reference-line presentation.
- Refine chart presentation where needed, including meaningful Kp severity coloring and clearer multi-series identification.
- Improve compact and expanded layouts so larger screens make sensible use of two columns.

### Out of scope

- API, repository, refresh, caching, or data-model changes.
- Navigation or bottom-navigation changes.
- Home screen changes.
- Aurora globe rendering/data-pipeline changes.
- New space-weather measurements, settings, interactions, animations, or unrelated Android-only features.
- PWA modifications.

## Data and state

Keep the existing `SpaceWeatherViewModel` and repository/data flows as the source of truth. Magnetic-field, plasma, Kp, GOES, hemispheric-power, Aurora, freshness, loading, and retained-data behavior remain unchanged.

The approved state change is that `Timeframe` initializes to **12 hours** instead of 3 hours. The segmented control continues to filter existing datasets using the established timeframe/filtering logic; no second UI-specific filtering system will be introduced.

Current Bz, speed, and density values feed the compact metric pills. Full datasets feed their corresponding charts. Aurora state feeds the hero. Kp and hemispheric-power state remain grouped under Geomagnetic Activity.

Responsive layout is derived from available screen width and is not persistent application state. Colors, chart labels, section metadata, and series legends remain presentation concerns rather than ViewModel concerns.

Existing unavailable, stale, and retained-data semantics are preserved and visually adapted to the new design.

## Component interfaces and screen structure

Use screen-local presentation components rather than introducing a broad new UI framework.

### Compact/mobile hierarchy

1. **Aurora hero**
   - Large globe presentation.
   - Existing freshness treatment integrated unobtrusively into the hero.

2. **Solar Wind**
   - Strong section heading.
   - Compact wrapping row of Bz, Speed, and Density pills.
   - PWA-inspired series colors.
   - Values visually emphasized over labels.

3. **Timeframe**
   - One joined native segmented control for **1h · 3h · 12h · 2d**.
   - 12h selected initially.

4. **Solar/near-Earth charts**
   - Refined defined cards with restrained border/background treatment.
   - IMF Bz/Bt.
   - Solar Wind Density.
   - Solar Wind Speed.
   - Solar Wind Temperature.
   - GOES Magnetometer.
   - Consistent chart header structure: contextual eyebrow, measurement title/unit, optional series legend, then visualization.

5. **Geomagnetic Activity**
   - Clear section break.
   - Planetary Kp card with meaningful severity colors.
   - Hemispheric Power card with clear North/South identification.

### Expanded widths

Keep the Aurora hero and major section headings full-width. Arrange compatible chart cards in a two-column grid without forcing awkward pairings merely to fill space.

Existing components such as `SolarWindMetrics`, `TimeframeSelector`, `KpChart`, `HemisphericPowerChart`, and `SpaceWeatherLineCard` may be substantially rewritten or replaced where that yields a cleaner structure. Keep public inputs small and derived from existing state. Remove obsolete UI structures once replacements are covered by tests rather than retaining parallel old/new paths.

## Error and edge-case behavior

Preserve existing no-data and fallback semantics. Missing datasets should remain represented honestly inside the new design without collapsing layout or substituting misleading zero values.

Freshness remains visible but secondary. It should not compete with the globe, current measurements, or chart titles.

Existing chart/data logic remains responsible for short series, gaps, missing secondary series, and extreme values. New cards and legends must tolerate those cases without fabricated values or placeholder series.

On narrow phones, charts remain single-column and metric pills may wrap gracefully. On expanded widths, compatible cards may use two columns while the hero, section headings, metrics, and timeframe control retain coherent full-width placement. Do not shrink charts merely to force additional columns.

The redesign must not interfere with the stabilized Aurora/Filament lifecycle. Scrolling, leaving the screen, returning, and configuration/state restoration must remain safe.

## Testing strategy

Use strict TDD during implementation.

Before production changes, add or update focused tests covering the new presentation contract:

- Aurora hero precedes Solar Wind content.
- Bz, Speed, and Density are exposed as the three summary metrics.
- Timeframe selector contains all four options.
- Default timeframe is 12h.
- Solar Wind and Geomagnetic Activity are distinct sections.
- Expected charts remain present.

Component tests cover redesigned metric pills and segmented timeframe control, including selection callbacks and selected-state semantics.

Chart tests preserve existing data/filtering coverage and add only stable presentation assertions where useful, such as expected chart title/context and Kp severity presentation. Avoid brittle pixel-coordinate or exact-spacing tests.

Existing ViewModel, filtering, chart-data, globe, lifecycle, and screen tests remain regression coverage. When an old test asserts obsolete component structure, replace it with the equivalent behavioral assertion rather than weakening coverage.

Implement in small TDD batches, grouping closely related Space Weather components when they touch the same files. After the complete reconstruction is green locally, push a minimal number of commits and actively monitor GitHub Actions. Final validation is a physical Pixel check focused on scrolling, globe lifecycle, chart readability, segmented-control interaction, and overall hierarchy.

The PWA is reference-only and must not be modified.
