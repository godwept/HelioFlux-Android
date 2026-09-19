# HelioFlux Native Android Conversion Progress

**Date:** 2026-09-19  
**Plan:** `docs/plans/2026-09-19-helioflux-android-native-conversion.md`

## Current status

Implementation is complete through the final planned production changes. Tasks 126–133 have been implemented, Task 134 is verified, Task 136 is wired into CI, and Task 137 is recorded below. Task 135 and the connected-test verification portions of Tasks 128–132 remain pending because the self-hosted GitHub Actions runner has no connected Android device or emulator.

## Phase 13 — App-Level Offline/Refresh Behavior

- [x] **Task 126 — App startup refresh coordinator.** `AppRefreshCoordinator` launches repository refreshes from application scope while screens remain cached-state observers. Repeated coordinator calls are guarded.
- [x] **Task 127 — Source-isolated refresh failure handling.** Refresh actions run independently; a failing source does not cancel successful NOAA/chart refreshes. Unit coverage passes.
- [ ] **Task 128 — Cached-first cold start verification.** Instrumentation coverage seeds Room and verifies cached Kp renders without a network refresh. The test compiles successfully; execution is pending a connected device/emulator.
- [ ] **Task 129 — Rotation/window resize verification.** Instrumentation coverage verifies destination state survives compact-to-expanded resize; app-level refresh is separately guarded by unit tests. Connected execution is pending.

## Phase 14 — Final Functional/Accessibility Verification

- [ ] **Task 130 — Navigation integration test.** Home → Space Weather → Solar Activity → Home coverage is present and compiles. Connected execution is pending.
- [ ] **Task 131 — Partial-data integration test.** Mixed fresh/cached/empty/failure section coverage is present and compiles. Connected execution is pending.
- [ ] **Task 132 — Freshness accessibility test.** Collapsed freshness semantics include freshness plus last-updated context. Instrumentation coverage compiles; connected execution is pending.
- [x] **Task 133 — No-location-permission manifest test.** Unit test passes and the app manifest requests no coarse, fine, or background location permission.
- [x] **Task 134 — Full unit-test suite.** `./gradlew testDebugUnitTest` passed in Android CI run 35476702191 before the connected-device gate.
- [ ] **Task 135 — Full connected UI/instrumentation suite.** `assembleDebugAndroidTest` passes. `connectedDebugAndroidTest` cannot run on the self-hosted runner because ADB reports **No connected devices**.
- [ ] **Task 136 — Release-candidate verification.** `lintDebug assembleDebug` is configured in CI after the optional connected-device check; final result is pending the next CI run.
- [x] **Task 137 — Feature-parity checklist.** Recorded below with incomplete validation explicitly left open.

## Feature-parity / success-criteria checklist

- [x] **Native primary screens:** Home, Space Weather, and Solar Activity are Compose destinations; Solar Activity is now wired into app-level navigation. No WebView is used as the application UI.
- [x] **Reference data sources represented:** NOAA solar wind/Kp/GOES/forecast, Helioviewer, DONKI, HEK, LASCO, HMI, ACE EPAM, ENLIL, and OVATION have native repository/parser paths covered by unit tests.
- [x] **HelioFlux visual direction:** Material 3 theme, dark space styling, solar imagery, scientific cards/charts, and adaptive navigation are implemented.
- [ ] **Portrait/landscape and width adaptation:** compact bottom navigation and expanded navigation rail plus adaptive screen layouts are implemented; final connected resize/orientation execution remains pending.
- [ ] **Offline cached recent data and freshness:** Room-backed rolling cache, cached-first repository behavior, freshness states, and accessibility context are implemented; final cold-start instrumentation execution remains pending.
- [x] **Background notifications without location:** WorkManager alert flow and conservative Kp/M/X event rules are implemented and unit-tested; manifest policy confirms no location permission.
- [x] **Three widgets:** Space Weather Status, Aurora Conditions, and static-fallback Sun Hero are implemented with deep links. Sun refresh is scheduled at approximately 30 minutes. Optional launcher animation remains intentionally disabled because physical-launcher validation was unavailable.
- [x] **Native Aurora Globe:** dedicated `feature:globe` implementation uses native SceneView/Filament architecture and is isolated from networking/repositories.
- [x] **Ported data-processing tests:** parser/domain/repository unit coverage passes.
- [x] **Failure isolation:** repository/coordinator tests verify source-specific failures retain cached data and do not cancel unrelated refreshes.
- [x] **PWA remains untouched:** all changes are confined to `godwept/HelioFlux-Android`.

## Open verification item

To finish the remaining unchecked tasks, connect an Android device to the self-hosted runner (with ADB authorization) or provide an emulator on that runner, then run:

```
./gradlew connectedDebugAndroidTest
```

The suite already compiles with `./gradlew assembleDebugAndroidTest`.
