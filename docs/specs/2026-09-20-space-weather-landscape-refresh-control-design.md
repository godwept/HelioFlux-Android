# Space Weather Landscape Layout and Refresh Control Polish Design

**Date:** 2026-09-20  
**Status:** Approved

## Goal

Fix the Space Weather experience on wide/landscape devices so the Aurora globe no longer monopolizes the viewport or prevents access to the rest of the screen, and make the per-chart refresh action feel visually centered and intentional.

## Landscape / Expanded Layout

When the app is in the existing expanded-width mode:

- Keep the navigation rail unchanged.
- Split Space Weather content into two panes.
- Left pane: Aurora globe hero only, fixed in place and not part of the scrolling chart list.
- Right pane: Solar Wind heading, metric pills, timeframe selector, all solar-wind charts, GOES magnetometer, geomagnetic heading, Kp, and Hemispheric Power.
- The right pane is one vertically scrollable column. Do not place two charts side-by-side inside the narrower right pane.
- Pull-to-refresh applies to the right scroll pane and keeps the existing global refresh behavior.
- Globe drag/pinch remains isolated in the left pane and must not disable or cancel scrolling in the right pane.
- Preserve the existing globe visualization, gesture implementation, Filament lifecycle, and freshness overlay.

Use an approximately 43/57 left/right split so the square globe remains large enough to be useful while still leaving a readable chart column on the Pixel landscape viewport.

Portrait/compact layout remains unchanged: the globe stays at the top of the Space Weather list, followed by the existing content.

## Refresh Control Polish

Keep the existing 48dp touch target, but change the visual treatment:

- visible circular surface grows from 36dp to 40dp;
- replace the text-based `↻` glyph with a Canvas-drawn refresh symbol so centering is geometric rather than font-baseline dependent;
- use a roughly 28dp refresh symbol;
- keep a neutral Material surface fill and subtle outline;
- keep the progress indicator centered while refreshing;
- preserve accessibility descriptions and duplicate-tap suppression.

The control remains neutral rather than adopting the chart series color.

## Testing

Add/adjust tests that verify:

- expanded Space Weather excludes the globe from the right-side scrolling block list;
- all non-globe blocks remain present and ordered in the right pane;
- portrait hierarchy remains unchanged;
- the source keeps globe-touch scroll suppression only for compact mode;
- refresh control dimensions are 48dp touch / 40dp visible / 28dp icon;
- refresh callback and busy/disabled behavior remain intact.

Run the full Android CI-equivalent suite and verify the result on the physical Pixel in landscape and portrait.

## Non-Goals

- No globe rendering/lifecycle changes.
- No chart scale/data/timeframe changes.
- No navigation changes.
- No new icon dependency.
- No Kp refresh action.
