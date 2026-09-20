# Home UI Refresh Design

**Date:** 2026-09-20
**Status:** Approved

## Goal

Refresh the native Android Home screen so it has a distinctive HelioFlux identity rather than reading as a generic Material 3 screen, while preserving the stabilized native behavior. The Sun is the visual focal point: it should appear to float directly on a true-black screen, with compact scientific conditions and NOAA forecast content arranged around it. The existing PWA is the visual/behavioral reference, interpreted through Android-native adaptive layouts and interaction patterns.

## Success Criteria

- [ ] The Home screen background is true black and visually matches the black surrounding the solar imagery.
- [ ] The solar hero has no visible Card/container, elevation, border, or contrasting surface; the Sun appears to float on the screen.
- [ ] Hero loading is continuous: spinner on black -> first/poster frame plus spinner -> animation starts and spinner disappears.
- [ ] Once a poster frame is visible, preload state changes never intentionally return the hero to a blank frame.
- [ ] Existing hero zoom, pan, double-tap reset, 200 ms blended playback, cached imagery, and failure behavior remain functional.
- [ ] Home has a clear HELIOFLUX -> Sun -> current conditions -> NOAA forecast hierarchy.
- [ ] Current conditions are compact, quickly scannable, and preserve their existing navigation targets.
- [ ] NOAA forecasts are more compact and visually differentiated while preserving the app's actual forecast data and expand/collapse behavior.
- [ ] Compact/portrait and expanded/landscape layouts remain adaptive and usable.
- [ ] No unrelated screen, repository, notification, widget, or data-source behavior changes.

## Scope

**In scope:**
- True-black Home background.
- PWA-inspired HELIOFLUX masthead with restrained solar-orange emphasis.
- Borderless/floating solar hero presentation.
- Hero loading-state correction.
- Compact Current Conditions presentation with meaningful accent colors.
- Compact PWA-inspired NOAA forecast presentation.
- Portrait/compact and expanded/landscape Home layouts.
- Focused tests for hero loading-state decisions and Home behavior affected by the redesign.

**Out of scope:**
- Changes to repositories, refresh cadence, APIs, or data sources.
- Changes to the hero's 200 ms playback cadence or blending algorithm.
- Changes to hero pinch zoom, pan, or double-tap reset.
- Redesigning Space Weather or Solar Activity in this pass.
- Redesigning shared bottom navigation/navigation rail in this pass.
- Notification or widget changes.
- Inventing forecast data or presentation semantics not supplied by the existing Android model.
- Adding a custom font dependency solely for this pass.

## Design

### Visual Direction

Use a hybrid direction: preserve Android-native Material 3 interaction, accessibility, adaptive layout, and navigation patterns while carrying over the strongest HelioFlux/PWA visual identity.

The screen background is pure black. Solar orange is the primary brand/solar accent. Cyan, green, red, and other semantic accents are reserved for meaningful measurements and status distinctions. Secondary text is subdued so the Sun and live measurements dominate.

The approved UI mockup is a visual reference for hierarchy, spacing, black background, floating Sun, compact instrumentation, and overall atmosphere. It is not a new data specification. Any mockup-only forecast values or labels that do not exist in the Android data model are not to be implemented.

### Home Composition

Compact/portrait order:
1. Centered HELIOFLUX masthead.
2. Borderless solar hero on the same true-black background.
3. Compact Current Conditions instrumentation.
4. NOAA forecast section with horizontally browsable compact cards.

The layout should be tighter than the current generic Material card stack while retaining adequate Android touch targets and readable spacing.

Expanded layouts retain native adaptation. The solar hero and current conditions may remain side-by-side where the existing expanded structure benefits from the available width, while preserving the same visual language as portrait. Forecast content should make productive use of wider space rather than merely stretching phone-width cards.

### Typography and Hierarchy

HELIOFLUX is centered, uppercase, strongly weighted, and letter-spaced with restrained solar-orange treatment inspired by the PWA masthead. Do not add a new font dependency unless later design work establishes a real need.

Section headings and scientific labels should be compact. Measurement values should carry more visual weight than their labels. Metadata such as issue times remains visually secondary.

### Solar Hero State Model

The hero has three visual states:

1. **Initial loading** — pure-black stage with a centered loading spinner.
2. **Poster loading** — as soon as the first usable solar frame is available, keep that frame continuously visible and overlay the spinner while the remaining playback frames are prepared.
3. **Playing** — once the usable playback set is ready, begin the existing blended animation from visible imagery and remove the spinner.

The poster is a stable visual layer. Updating the collection of preloaded/playable frames must not temporarily remove the only visible image or intentionally return the stage to blank.

Failed individual frame preloads may be excluded from playback rather than blocking the entire hero. If only one usable frame ultimately exists, it remains visible as static imagery rather than repeatedly entering a loading state. Existing retained/cached imagery behavior remains intact.

The hero itself supplies no Card surface, elevation, border, or contrasting container. Its black image stage visually merges into the Home background. Existing gesture handling remains attached to the hero area.

### Current Conditions

Replace the generic large Material-card appearance with compact scientific instrumentation while preserving the existing Kp, Bz, solar-wind, and flare data and their tap-through destinations.

Use semantic accents deliberately: solar orange for Kp/solar emphasis, cyan for magnetic-field information, and appropriate green/red/alert accents where the existing measurement meaning supports them. Do not create new scientific classifications solely for styling.

The geomagnetic status message remains available but visually secondary to the live measurements.

### NOAA Forecast

Preserve the Android app's existing NOAA forecast sections, summary, forecast text, issue time, and expand/collapse interaction.

On compact screens, use horizontally browsable cards inspired by the PWA rather than a long vertical stack. Cards should have restrained dark treatment against the black screen, compact typography, category accent where it maps cleanly to existing section keys, clear summary/forecast hierarchy, subdued issue time, and an obvious but quiet expand/collapse affordance.

Expanded layouts may show more forecast content simultaneously when space permits. Do not invent the mockup's example three-day risk forecast or any other data not present in the existing model.

### Error Handling and Edge Cases

- Repository loading with no frames shows the spinner on black.
- A first/poster frame remains visible while additional frames preload.
- Preload failures do not create a blank transition.
- If enough frames are usable for animation, playback begins with the existing cadence/blending behavior.
- If only one usable frame is available, display it statically.
- Repository failure with retained/cached imagery continues to show retained imagery and the existing cached/failure indication.
- Missing condition values continue to render the existing unavailable placeholder rather than fabricated values.
- Empty forecast data remains a valid state and must not crash or distort the Home layout.
- Compact and expanded layouts must not clip the hero, conditions, or forecast interaction areas.

## Testing Strategy

Use strict TDD during implementation.

Hero tests should target state decisions rather than fragile screenshot timing:
- poster selection while preload is incomplete;
- spinner visibility until playback readiness;
- playback readiness only when the usable set is ready;
- poster retention through the loading-to-playing transition;
- single-usable-frame behavior;
- existing blend-progress behavior remains covered.

Home UI tests should verify stable semantics and structure where practical:
- Home/masthead/hero/conditions/forecast elements remain reachable;
- condition metric taps still navigate to their existing destinations;
- compact and expanded composition markers remain correct;
- forecast expand/collapse behavior remains intact.

Avoid tests coupled to arbitrary pixel values, exact shadows, or screenshot colors unless a visual regression framework is intentionally introduced later. Run the existing unit/UI suites affected by the changed files and the normal Android CI verification.

Physical Pixel testing follows implementation and green CI, with special attention to the hero's real network-loading transition, black-background continuity, portrait/landscape composition, scrolling, and forecast interaction.

## Open Questions

None for this Home pass. Shared navigation icon/chrome redesign and the Space Weather/Solar Activity visual refresh are intentionally deferred to subsequent UI passes.
