# Space Weather Landscape Layout and Refresh Control Polish Plan

**Date:** 2026-09-20  
**Design:** docs/specs/2026-09-20-space-weather-landscape-refresh-control-design.md  
**Status:** Approved for implementation

## Task 1: Lock down expanded-layout behavior with tests

**Files**
- app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreenTest.kt

Add tests first that require:

1. Compact layout still contains the existing full ordered block list including `AuroraHero`.
2. Expanded layout's scrollable block list excludes `AuroraHero`.
3. Expanded right pane keeps the remaining blocks in the same order and one block per row.
4. Source-level structure contains a dedicated expanded two-pane branch and keeps compact `userScrollEnabled = !globeTouchActive` handling.

## Task 2: Implement the expanded two-pane Space Weather layout

**Files**
- app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt

Implementation:

- keep the existing compact `PullToRefreshBox + LazyColumn`;
- for `expanded == true`, render a `Row`:
  - left weighted pane around 43% with fixed square Aurora hero;
  - right weighted pane around 57% with `PullToRefreshBox + LazyColumn`;
- use the right-side block list from Task 1;
- keep charts single-column in the right pane;
- do not pass globe touch state into the right-side scrolling pane;
- reuse existing `SpaceWeatherBlockContent` rather than duplicating chart logic;
- preserve the same global and per-source refresh callbacks.

## Task 3: Lock down refresh-control sizing

**Files**
- app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt
- app/src/androidTest/java/ca/stewark/helioflux/ui/spaceweather/ChartRefreshActionTest.kt

Tests first:

- touch target = 48dp;
- visible circle = 40dp;
- drawn icon size = 28dp;
- idle action remains clickable;
- busy action remains disabled.

## Task 4: Replace the text glyph with a centered drawn icon

**Files**
- app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt

Implementation:

- add a small private `RefreshGlyph` Canvas composable;
- draw a circular arc with a rounded stroke and arrowhead;
- center it inside the 40dp circular surface;
- keep 48dp `IconButton` semantics/touch target;
- use a centered progress indicator while busy;
- remove the font-based `↻` glyph.

## Task 5: Verification

Run the full CI-equivalent sequence:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

Push this work as one focused TDD batch and monitor Android CI until green.

Physical Pixel acceptance:

- landscape Space Weather shows fixed globe on the left;
- right pane scrolls independently;
- globe interaction never blocks right-pane scrolling;
- pull-to-refresh works from the right pane;
- portrait layout is unchanged;
- refresh controls look centered and proportionate;
- per-chart refresh and busy states still work.
