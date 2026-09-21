# Space Weather Black Canvas Implementation Plan

**Date:** 2026-09-20  
**Design:** `docs/specs/2026-09-20-helioflux-space-weather-ui-refresh-design.md`

## Goal

Make the entire Space Weather screen canvas pure black in compact and expanded layouts so the Aurora globe's existing black rendering surface blends into the page and the Earth appears to float in space. Preserve all existing component styling, chart behavior, globe rendering/gestures, refresh behavior, and data flow.

## Task 1: Add regression coverage for the screen background contract

**Files:**
- `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`

Add a focused source/presentation regression assertion establishing that the Space Weather root presentation applies `Color.Black` to both compact and expanded screen canvases. Keep the test structural and narrow; do not add pixel/screenshot testing or assertions about unrelated card colors.

Run the focused Space Weather tests and confirm the new assertion fails before production code changes.

## Task 2: Apply the black canvas in both layouts

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt`

Make the smallest Compose change that guarantees a pure-black background across the full Space Weather content area:

- Compact layout: ensure the full `PullToRefreshBox` / list canvas is backed by `Color.Black`.
- Expanded layout: ensure the full two-pane `Row`, including the globe side and scrolling content side, is backed by `Color.Black`.
- Keep the existing globe slot black; do not alter `AuroraGlobe`, its clipping, Filament lifecycle, gesture handling, or data pipeline.
- Do not recolor cards, chart surfaces, metric pills, timeframe controls, text, refresh indicators, or bottom navigation.
- Do not change spacing, sizing, hierarchy, or responsive weights.

Run the focused Space Weather test suite, then the relevant unit-test suite. Confirm all tests pass.

## Task 3: Commit, push, and validate CI

Commit only the spec/plan, focused test, and Space Weather implementation changes. Push to `main` with no unrelated modifications.

Actively monitor the resulting Android CI run until completion. If CI fails, inspect the failure, make only the required fix, push, and continue monitoring until green.

After CI is green, physically verify on the Pixel:

- the complete Space Weather tab is black from top through the bottom-navigation boundary;
- the globe slot boundary is no longer visually distinguishable from the page background;
- the Earth reads as floating on the screen;
- cards/charts remain readable and retain their existing styling;
- portrait scrolling and pull-to-refresh still work;
- landscape/expanded layout is black on both panes;
- globe rotation, drag, pinch zoom, and resumed auto-rotation remain unchanged.

No additional visual redesign is part of this task.
