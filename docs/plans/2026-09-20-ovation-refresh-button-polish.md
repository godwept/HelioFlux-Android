# OVATION Refresh Reliability and Chart Refresh Button Polish Plan

**Date:** 2026-09-20  
**Design:** docs/specs/2026-09-20-ovation-refresh-button-polish-design.md  
**Status:** Ready for implementation

## Task 1: Reproduce and lock down refresh overlap

**Files**
- app/src/test/java/ca/stewark/helioflux/data/AppRefreshCoordinatorTest.kt
- app/src/main/java/ca/stewark/helioflux/data/AppRefreshCoordinator.kt

**TDD**
1. Add a test that launches two `refreshFast()` calls with the first blocked behind a gate.
2. Assert the second fast action does not start until the first finishes.
3. Refactor the coordinator so fast and slow groups each have their own coroutine `Mutex`.
4. Make startup and manual all-refresh call the serialized group methods rather than directly concatenating action lists.
5. Keep source failures isolated with the existing supervised per-action execution.

## Task 2: Stop periodic work duplicating startup refresh

**Files**
- app/src/test/java/ca/stewark/helioflux/data/AppDataRefreshWorkerTest.kt
- app/src/main/java/ca/stewark/helioflux/data/AppDataRefreshWorker.kt

**TDD**
1. Extend the fast request test to require a 15-minute initial delay.
2. Extend the slow request test to require a 1-hour initial delay.
3. Add matching `setInitialDelay` calls to both request builders.
4. Preserve the existing network constraint and periodic cadence.

## Task 3: Polish chart refresh action dimensions and treatment

**Files**
- app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherChartsTest.kt
- app/src/androidTest/java/ca/stewark/helioflux/ui/spaceweather/ChartRefreshActionTest.kt
- app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt

**TDD**
1. Add constants tests requiring a 48dp touch target and 36dp visible circle.
2. Extend Compose coverage to assert the refresh action remains enabled/clickable when idle and disabled while busy.
3. Implement the circular neutral Material surface and larger glyph/spinner.
4. Do not add an icon dependency or modify chart layout/data.

## Task 4: Verification

Run:
```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

Push as one focused implementation commit, monitor Android CI to green, then physically verify on Pixel:

- after launch, OVATION should not be downgraded to Cached by a duplicate broad refresh;
- pull-to-refresh still refreshes all data;
- genuine OVATION failure still shows Cached;
- chart refresh controls are visibly larger and feel integrated with the cards;
- chart refresh callbacks and busy-state suppression still work.
