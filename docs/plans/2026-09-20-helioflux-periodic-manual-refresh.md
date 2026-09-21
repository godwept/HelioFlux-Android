# HelioFlux Periodic and Manual Data Refresh Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-helioflux-periodic-manual-refresh-design.md  
**Status:** Ready for review

## Overview

Centralize HelioFlux refresh orchestration, add 15-minute and hourly WorkManager tiers, correct OVATION freshness to use forecast validity, remove duplicate alert-worker downloads, add pull-to-refresh to all three primary screens, and add targeted refresh controls to the Bz/Bt, plasma, GOES magnetometer, and Hemispheric Power charts. Preserve cached-first Room flows, existing Solar Hero behavior, and Aurora globe gesture/lifecycle behavior.

## Implementation Batches

- **Batch 1 — refresh infrastructure:** Tasks 1–6. Push once and monitor Android CI to green.
- **Batch 2 — manual/UI refresh:** Tasks 7–13. Push once and monitor Android CI to green.

Do not mix unrelated cleanup into either batch.

## Tasks

### Task 1: Split the existing coordinator into startup, fast, slow, and all refresh groups
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/data/AppRefreshCoordinator.kt`  
- `app/src/test/java/ca/stewark/helioflux/data/AppRefreshCoordinatorTest.kt`

**Test first:**
Add focused coordinator tests that inject named fake actions and prove:

```kotlin
@Test fun fastRefreshRunsOnlyFastActions()
@Test fun slowRefreshRunsOnlySlowActions()
@Test fun allRefreshRunsFastSlowAndSolarHero()
@Test fun startupRefreshRunsFastAndSlowButNotSolarHero()
@Test fun oneFailureDoesNotCancelSiblingActions()
```

Use counters/lists rather than repository mocks. Assert the fast group contains the equivalent actions for magnetic, plasma, Kp, GOES magnetometer, X-ray, ACE EPAM, OVATION, and HP; assert the slow group contains forecast, flare probabilities, flares, CMEs, and solar imagery.

**Implementation:**
Refactor `AppRefreshCoordinator` so it accepts three action lists:

```kotlin
class AppRefreshCoordinator(
    private val fastActions: List<suspend () -> Unit>,
    private val slowActions: List<suspend () -> Unit>,
    private val solarHeroAction: suspend () -> Unit,
)
```

Add suspend operations:

```kotlin
suspend fun refreshFast()
suspend fun refreshSlow()
suspend fun refreshAll()
fun refreshStartupOnce(scope: CoroutineScope)
```

Run sibling actions independently with `supervisorScope { launch { runCatching { ... } } }` or equivalent so one source failure does not cancel others.

Map repository methods exactly as approved:

Fast:
- `spaceWeather.refreshMagnetic`
- `spaceWeather.refreshPlasma`
- `spaceWeather.refreshKp`
- `spaceWeather.refreshGoesMagnetometer`
- `spaceWeather.refreshHemisphericPower`
- `aurora.refresh`
- `solarActivity.refreshXray`
- `solarActivity.refreshAceEpam`

Slow:
- `forecast.refresh`
- `solarActivity.refreshFlareProbabilities`
- `solarActivity.refreshFlares`
- `solarActivity.refreshCmes`
- `solarImagery.refreshAll`

Manual all only:
- `solarHero.refresh`

Startup must still leave Solar Hero to `HomeViewModel.refreshIfStale()`.

**Verify:**  
Run `./gradlew :app:testDebugUnitTest --tests '*AppRefreshCoordinatorTest*'`.

---

### Task 2: Add observable broad-refresh busy state
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/data/AppRefreshCoordinator.kt`  
- `app/src/test/java/ca/stewark/helioflux/data/AppRefreshCoordinatorTest.kt`

**Test first:**
Add:

```kotlin
@Test fun manualAllRefreshSetsBusyUntilEveryActionCompletes()
@Test fun duplicateManualAllRefreshIsIgnoredWhileBusy()
```

Use `CompletableDeferred` gates to hold an action in flight. Assert `isRefreshing.value` becomes true before completion and false afterward, and a second `refreshAll()` does not execute the action twice.

**Implementation:**
Expose:

```kotlin
val isRefreshing: StateFlow<Boolean>
```

Guard only user-initiated `refreshAll()` against duplicate concurrent all-refresh calls. Do not block the periodic fast/slow methods behind this UI state.

**Verify:**  
Run `./gradlew :app:testDebugUnitTest --tests '*AppRefreshCoordinatorTest*'`.

---

### Task 3: Add 15-minute and hourly periodic data-refresh workers
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/data/AppDataRefreshWorker.kt`  
- `app/src/test/java/ca/stewark/helioflux/data/AppDataRefreshWorkerTest.kt`  
- `app/build.gradle.kts`

**Test first:**
Add WorkManager test dependency to `app`:

```kotlin
testImplementation("androidx.work:work-testing:2.11.2")
```

Write tests for request builders:

```kotlin
@Test fun fastRequestUsesFifteenMinutePeriodAndConnectedNetwork()
@Test fun slowRequestUsesOneHourPeriodAndConnectedNetwork()
```

Also test worker routing with an injectable/test override:

```kotlin
@Test fun fastWorkerCallsRefreshFast()
@Test fun slowWorkerCallsRefreshSlow()
```

**Implementation:**
Create two `CoroutineWorker` classes, or one worker with an input-data tier if that remains simpler while keeping tests explicit:

- fast tier → `refreshFast()`
- slow tier → `refreshSlow()`

Both obtain the application `RepositoryProvider` and build/use the shared coordinator.

Request cadence:
- fast: 15 minutes;
- slow: 1 hour;
- both: `NetworkType.CONNECTED`.

Return `Result.retry()` only when the worker itself cannot execute its tier; individual repository failures remain isolated inside the coordinator.

**Verify:**  
Run `./gradlew :app:testDebugUnitTest --tests '*AppDataRefreshWorkerTest*'`.

---

### Task 4: Add unique periodic scheduling and wire it at application startup
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/data/AppDataRefreshScheduler.kt`  
- `app/src/main/java/ca/stewark/helioflux/HelioFluxApplication.kt`  
- `app/src/test/java/ca/stewark/helioflux/data/AppDataRefreshSchedulerTest.kt`  
- `app/src/test/java/ca/stewark/helioflux/HelioFluxApplicationTest.kt`

**Test first:**
Test stable scheduler constants/request contracts:

```kotlin
@Test fun schedulerUsesDistinctUniqueWorkNames()
@Test fun applicationInitializesDataRefreshSchedulingOnce()
```

Where practical with WorkManager testing, assert both periodic works are enqueued with `ExistingPeriodicWorkPolicy.KEEP`.

**Implementation:**
Add `AppDataRefreshScheduler.schedule()` that enqueues:

- `helioflux-live-data-refresh`
- `helioflux-slow-data-refresh`

with `KEEP`.

In `HelioFluxApplication.onCreate()`:

1. create/store one application-scoped `AppRefreshCoordinator`;
2. call `refreshStartupOnce(applicationScope)`;
3. schedule both periodic data workers;
4. preserve alert scheduling and Sun widget scheduling.

Expose the coordinator to Compose through the application instance so pull-to-refresh uses the same object/busy state.

**Verify:**  
Run:
`./gradlew :app:testDebugUnitTest --tests '*AppDataRefreshSchedulerTest*' --tests '*HelioFluxApplicationTest*'`.

---

### Task 5: Correct OVATION freshness to use forecast validity
**Files:**  
- `core/data/src/main/java/ca/stewark/helioflux/core/data/repository/AuroraRepository.kt`  
- `core/data/src/test/java/ca/stewark/helioflux/core/data/repository/AuroraRepositoryTest.kt`

**Test first:**
Add a regression test where observation time is older than the 10-minute freshness window but forecast time is current:

```kotlin
@Test fun currentForecastIsFreshEvenWhenObservationTimeIsOlder()
```

Example fixture:
- now: 30 minutes after observation;
- forecast: at/near now;
- expected freshness: `DataFreshness.Fresh`.

Also keep a delayed case where forecast time itself is older than the policy.

**Implementation:**
When a valid OVATION response is persisted, keep the snapshot entity unchanged but write `parsed.forecastTimestampMillis` into the freshness/status timestamp used by `FreshnessEvaluator`.

Do not change the 10-minute OVATION policy in this task.

**Verify:**  
Run `./gradlew :core:data:testDebugUnitTest --tests '*AuroraRepositoryTest*'`.

---

### Task 6: Remove duplicate network refreshes from the alert worker
**Files:**  
- `feature/alerts/src/main/java/ca/stewark/helioflux/feature/alerts/SpaceWeatherAlertWorker.kt`  
- `feature/alerts/src/test/java/ca/stewark/helioflux/feature/alerts/SpaceWeatherAlertWorkerTest.kt`  
- add `feature/alerts/src/test/java/ca/stewark/helioflux/feature/alerts/RepositoryAlertRepositoryTest.kt`

**Test first:**
Create repository-level tests proving `currentConditions()` only reads cached flows and does not invoke network-refresh methods. Use fake repository collaborators or the narrowest test seam required.

Cover:
- cached Kp + cached flares produce `AlertConditions`;
- failed required state still throws `TransientAlertDataException`;
- HP is no longer refreshed by the alert path.

**Implementation:**
Remove these calls from `RepositoryAlertRepository.currentConditions()`:

```kotlin
spaceWeather.refreshKp()
spaceWeather.refreshHemisphericPower()
solarActivity.refreshFlares()
```

Keep alert policy/state/notification behavior unchanged.

**Verify:**  
Run `./gradlew :feature:alerts:testDebugUnitTest`.

**Batch 1 verification and push:**  
Run `./gradlew testDebugUnitTest assembleDebugAndroidTest lintDebug assembleDebug`.  
Push Tasks 1–6 as one atomic commit and monitor Android CI until green before starting Batch 2.

---

### Task 7: Route pull-to-refresh through the shared application coordinator
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/HelioFluxApp.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/ManualRefreshRoutingTest.kt`

**Test first:**
Extract/test the minimal callback contract needed to verify all destinations receive the same broad refresh action and shared busy state:

```kotlin
@Test fun allPrimaryDestinationsUseSharedRefreshAllCallback()
```

Avoid constructing repository refresh logic in Compose tests.

**Implementation:**
In top-level `HelioFluxApp`, collect `app.refreshCoordinator.isRefreshing` and provide:

```kotlin
onRefreshAll = { scope.launch { app.refreshCoordinator.refreshAll() } }
isRefreshing = ...
```

Pass these through `DestinationContent` to Home, Space Weather, and Solar Activity.

Do not call repositories directly from Compose.

**Verify:**  
Run `./gradlew :app:testDebugUnitTest --tests '*ManualRefreshRoutingTest*'`.

---

### Task 8: Add pull-to-refresh to Home
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeRefreshTest.kt`

**Test first:**
Render `HomeScreen` with a callback counter and perform a downward pull gesture from the top. Assert the callback runs once.

Also render with `isRefreshing = true` and assert the pull-refresh indicator is present through a stable test tag such as `home-refresh-indicator`.

**Implementation:**
Extend `HomeScreen` with:

```kotlin
isRefreshing: Boolean
onRefresh: () -> Unit
```

Wrap the existing `LazyColumn` in Material 3 `PullToRefreshBox` (or the stable equivalent supplied by the current BOM). Keep all existing content/layout unchanged.

**Verify:**  
Run `./gradlew :app:assembleDebugAndroidTest`.

---

### Task 9: Add pull-to-refresh to Solar Activity
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityRefreshTest.kt`

**Test first:**
Render with a callback counter, pull down from the top of the scroll container, and assert one refresh callback. Add a stable indicator tag for the busy state.

**Implementation:**
Add `isRefreshing` and `onRefresh` parameters and wrap the existing vertically scrolling content in the same Material 3 pull-refresh container.

Do not change imagery viewer behavior or chart content.

**Verify:**  
Run `./gradlew :app:assembleDebugAndroidTest`.

---

### Task 10: Add targeted refresh state and routing to SpaceWeatherViewModel
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherViewModel.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherViewModelTest.kt`

**Test first:**
Add an enum/sealed target contract in tests and cover:

```kotlin
@Test fun magneticRefreshCallsOnlyMagneticSource()
@Test fun plasmaRefreshIsSharedByDensitySpeedAndTemperature()
@Test fun goesRefreshCallsOnlyGoesSource()
@Test fun hpRefreshCallsOnlyHemisphericPowerSource()
@Test fun duplicateTapForSameSourceIsIgnoredWhileRefreshing()
@Test fun unrelatedSourceCanRefreshWhileAnotherSourceIsBusy()
```

Use injected suspend refresh lambdas in `forTest`; do not require real repositories.

**Implementation:**
Add source-level refresh state to `SpaceWeatherUiState`, for example:

```kotlin
val refreshingSources: Set<SpaceWeatherRefreshSource> = emptySet()
```

with sources:
- Magnetic
- Plasma
- GoesMagnetometer
- HemisphericPower

Add methods on `SpaceWeatherViewModel` that guard duplicate same-source work, update the set in `try/finally`, and invoke the corresponding repository refresh.

**Verify:**  
Run `./gradlew :app:testDebugUnitTest --tests '*SpaceWeatherViewModelTest*'`.

---

### Task 11: Add a reusable compact refresh action to chart headers
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherCharts.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/HemisphericPowerChart.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/spaceweather/ChartRefreshActionTest.kt`

**Test first:**
Render a regular `SpaceWeatherLineCard` and `HemisphericPowerChart` with refresh callbacks.

Assert:
- refresh action exists with accessible description `Refresh <chart name>`;
- clicking invokes the callback;
- busy state disables repeated clicks and shows progress/spinning treatment;
- no refresh action is rendered when callback is null.

**Implementation:**
Add optional parameters to both chart-card composables:

```kotlin
onRefresh: (() -> Unit)? = null
refreshing: Boolean = false
refreshContentDescription: String = ...
```

Place a small `IconButton` aligned with the context/title header without changing chart dimensions.

Use a standard Material refresh icon already available through Material icons if present; otherwise draw/use the smallest existing project-compatible icon approach. Do not introduce a new icon library solely for this.

**Verify:**  
Run `./gradlew :app:assembleDebugAndroidTest`.

---

### Task 12: Wire targeted chart refresh actions and Space Weather pull-to-refresh
**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/HelioFluxApp.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherScreen.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/spaceweather/SpaceWeatherRefreshTest.kt`

**Test first:**
Render `SpaceWeatherScreen` with callback counters and state.

Verify:
- Bz/Bt refresh routes to magnetic callback;
- Density, Speed, and Temperature each route to the same plasma callback;
- GOES routes to GOES callback;
- HP routes to HP callback;
- Kp has no refresh action;
- pull-to-refresh triggers only the broad `onRefreshAll` callback;
- when globe touch is active, parent scrolling remains disabled exactly as before.

Use stable chart-specific tags/content descriptions rather than pixel assertions.

**Implementation:**
Extend `SpaceWeatherScreen` parameters with:
- broad `isRefreshing` + `onRefresh`;
- targeted callbacks from `SpaceWeatherViewModel`.

Pass `refreshingSources` to the chart cards.

Wrap the `LazyColumn` with Material 3 pull-to-refresh while preserving:

```kotlin
userScrollEnabled = !globeTouchActive
```

Do not modify `AuroraGlobe` or Filament/SceneView lifecycle code.

**Verify:**  
Run `./gradlew :app:assembleDebugAndroidTest`.

---

### Task 13: Run focused and full verification, then physical Pixel acceptance
**Files:** No planned production changes unless tests expose a defect in Tasks 7–12.

**Test first / verification sequence:**
Run focused suites:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :feature:alerts:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

Then full CI-equivalent verification:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

**Implementation:**
Fix only failures caused by this planned feature. Do not refactor unrelated code.

**Physical Pixel acceptance:**
Verify:
- opening the app refreshes cached datasets without blanking screens;
- Aurora freshness becomes Fresh when NOAA forecast validity is current;
- Home pull-to-refresh refreshes all app data;
- Space Weather pull-to-refresh refreshes all app data;
- Solar Activity pull-to-refresh refreshes all app data;
- globe drag/pinch still coexists with vertical scrolling and does not accidentally start refresh;
- Bz/Bt refresh works independently;
- Density/Speed/Temperature refresh the shared plasma source;
- GOES magnetometer refresh works independently;
- HP refresh works independently and remains independent of timeframe selector;
- Kp has no per-chart refresh icon;
- current chart data remains visible during refresh;
- no duplicate rapid taps create duplicate same-source requests.

**Verify:**  
Push Tasks 7–13 as one atomic commit and actively monitor Android CI until green. If CI fails, inspect the failing job, make only the necessary fix, push, and continue monitoring.

## Definition of Done

- [ ] Fast tier refreshes magnetic, plasma, Kp, GOES magnetometer, X-ray, ACE EPAM, OVATION, and HP
- [ ] Slow tier refreshes forecast, flare probabilities, flares, CMEs, and solar imagery metadata/regions/ENLIL
- [ ] Startup refresh remains cached-first and does not duplicate Solar Hero startup refresh
- [ ] Manual `refreshAll()` includes Solar Hero
- [ ] 15-minute and hourly periodic work are unique, network constrained, and KEEP-policy scheduled
- [ ] Alert worker no longer performs duplicate network refreshes
- [ ] OVATION freshness uses forecast validity
- [ ] Home, Space Weather, and Solar Activity support pull-to-refresh
- [ ] Bz/Bt, plasma, GOES magnetometer, and HP charts have targeted refresh actions
- [ ] Kp has no targeted refresh action
- [ ] Plasma chart refresh state is shared across Density, Speed, and Temperature
- [ ] Globe gesture/lifecycle behavior is unchanged
- [ ] Cached content stays visible during refresh and source failures remain isolated
- [ ] All new code has tests
- [ ] Full Android tests/lint/build pass
- [ ] Both implementation batches finish with green GitHub Actions
- [ ] No unrelated files are modified
