# Home Solar Background Zoom Implementation Plan

**Date:** 2026-09-24
**Design doc:** `docs/specs/2026-09-24-home-solar-background-zoom-design.md`
**Status:** Ready for review

## Overview

Turn the Home solar hero into a fixed background only after the user zooms it. Keep one animation instance, put the existing Home content in front, route gestures only from exposed animation, and persist the chosen zoom and position across launches. Restore the square hero at 1×. Tasks are ordered for test-first work and should each be kept to a 2–5 minute change; split a task further if it grows.

## Tasks

### Task 1: Define and test the solar view state

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeSolarViewState.kt` (new), `app/src/test/java/ca/stewark/helioflux/ui/home/HomeSolarViewStateTest.kt` (new)

**Test first:** Add unit cases for default `(scale=1, x=0, y=0)`, clamping zoom to 1×–4×, converting pan pixels to fractions of the current viewport, reconstructing pan pixels on a different viewport, constraining a restored position so some animation remains visible, and resetting both position components at 1×. Add a transition test that a tiny unfinished pinch returns to 1×, while a settled zoom beyond a small threshold enters background mode. Run the focused test and confirm it fails for the missing state type.

**Implementation:** Add an immutable state containing `scale`, `panFractionX`, and `panFractionY`. Put zoom/pan, viewport conversion, bounds, reset, and settle behavior in small pure functions. Use the current 1×–4× zoom limit and a named small entry threshold. Keep the threshold only for settling a new pinch; any persisted scale above 1× must restore background mode. Reject non-finite scale and position values during normalization.

**Verify:** `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeSolarViewStateTest"` passes.

---

### Task 2: Persist the chosen view

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeSolarViewStore.kt` (new), `app/src/test/java/ca/stewark/helioflux/ui/home/HomeSolarViewStoreTest.kt` (new)

**Test first:** With Robolectric and an isolated preferences name, test round-trip save/load through a newly created store instance, default state when no keys exist, sanitizing corrupt or out-of-range stored floats, and saving a reset view as 1× with zero position. Confirm the new tests fail before adding the store.

**Implementation:** Define the smallest `read()`/`save(state)` store interface and a `SharedPreferences` implementation using the application context. Persist three float values: scale and the two viewport-relative position components. Normalize on read and write. Use `apply()` for writes; no database schema or new library is needed.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeSolarViewStoreTest"`.

---

### Task 3: Give Home one view state and save settled changes

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add a Compose test using an in-memory `HomeSolarViewStore` fake: seed a zoomed view, compose Home, and assert the background-mode tag is present; reset through the provided state callback and assert the fake eventually contains 1× and zero position. Add a fresh-composition case that reads the previously saved view. Run on a connected device/emulator if available; otherwise compile the new instrumentation test before implementation.

**Implementation:** Let `HomeScreen` accept an optional injected store for tests and use the Android store by default. Read once for the initial Home view state. Own subsequent state changes above both compact and expanded layouts. Debounce writes until the transform gesture settles; avoid writing for every frame or pointer move. Save reset promptly. Keep the existing `HomeScreen` call from `HelioFluxApp.kt` source-compatible.

**Verify:** Run `./gradlew :app:compileDebugAndroidTestKotlin` and the new `HomeDashboardTest` cases when a device is available.

---

### Task 4: Make SolarHero use the Home-owned transform

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`, `app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add a UI test that pinching the square reports a changed Home view state and double tap reports the reset state. Add a unit assertion that a reset view cannot retain a pan offset. Confirm the focused new tests fail for the missing callbacks.

**Implementation:** Replace SolarHero's private `rememberSaveable` scale/offset with the supplied `HomeSolarViewState` and change callback. Keep the existing frame preload, blend cadence, loading indicator, and cached-image message. Report gesture completion separately from per-pointer transform changes so Home can settle and save once. Preserve the existing square stage and its test tags at 1×.

**Verify:** Run `HomeSolarViewStateTest` and `SolarHeroPlaybackTest`; compile and run the new Home UI test where possible.

---

### Task 5: Switch the compact Home layout to a fixed background

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add compact UI assertions for one square hero at 1×, one full-home background hero when restored zoom is above 1×, the masthead/conditions/forecast still present, and the compact content moving into the former hero space. Update the older source-structure test only where its literal placement assumptions conflict with the approved behavior. Confirm the new UI case fails on the old layout.

**Implementation:** Use a root `Box` for the Home content area. In background mode, place SolarHero behind the compact foreground LazyColumn and omit its square item; in normal mode, retain the square item. Keep the animation as one movable Compose content instance across placements so preload and playback do not run twice or restart during the switch. Keep pull-to-refresh and the 16 dp horizontal content padding. Put a stable test tag on the background stage.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeScreenSourceTest"` and the compact `HomeDashboardTest` cases on a device.

---

### Task 6: Apply the same mode to expanded Home

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/test/java/ca/stewark/helioflux/ui/home/HomeScreenSourceTest.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add expanded UI cases asserting that normal mode retains the existing 43% hero / 57% content split, background mode has a single full-home hero behind the masthead and right pane, and the right pane still scrolls to NOAA Forecast. Run the new case before changing the expanded layout.

**Implementation:** Place the same movable SolarHero content at the root background in zoomed mode and in the left hero pane at 1×. Retain the expanded masthead, right-side content width, pull-to-refresh, and current navigation rail boundary. The background fills only the Home content area.

**Verify:** Run the focused `HomeScreenSourceTest` and expanded `HomeDashboardTest` cases.

---

### Task 7: Render the animation across the available background

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add UI assertions that the background stage occupies the Home viewport, the square stage remains square at 1×, and a saved zoomed view still shows the masthead plus loading indicator while frames are unavailable. Include the cached-imagery message case. Run the cases before the rendering change.

**Implementation:** Give SolarHero a background presentation that fills its parent instead of enforcing `aspectRatio(1f)`. Crop the square frames to cover the viewport, apply the saved scale and viewport-relative pan, and clip within the Home area. Keep the title free of a panel and the existing opaque forecast card surfaces. Show a dark background while imagery is loading; retain the current loading and cache notices without blocking the foreground.

**Verify:** Run the new Home UI cases and visually compare compact and expanded staging on an emulator/device.

---

### Task 8: Define exposed-background hit testing

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeGestureRegions.kt` (new), `app/src/test/java/ca/stewark/helioflux/ui/home/HomeGestureRegionsTest.kt` (new)

**Test first:** Add pure tests with viewport-relative rectangles: a point inside a visible title, condition badge, forecast heading, or card is protected; a point in a gap or outside those rectangles is exposed; a removed lazy card no longer protects its old position. Confirm the tests fail for the missing region helper.

**Implementation:** Add a small registry keyed by visible foreground element ID, with `update(id, Rect)`, `remove(id)`, and `isExposed(Offset)`. Keep coordinates in the Home root's coordinate space. Do not treat the full LazyColumn viewport as protected: its transparent gaps must remain usable for solar gestures.

**Verify:** Run `./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.HomeGestureRegionsTest"`.

---

### Task 9: Register visible foreground bounds

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt`, `app/src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add a Home UI case that exercises the registry after scrolling: a visible card and badge remain protected, while the exposed gap next to them is reported as exposed; a card that has scrolled away is removed. Confirm the case fails before the callbacks exist.

**Implementation:** Register the masthead's visible text area in `HomeScreen.kt`; pass an optional bounds callback to `CurrentConditions`/`Metric` and `ForecastCards`/`ForecastCard` so each visible badge and card registers its actual bounds. Pass a modifier to `HelioFluxSectionHeading` to register the NOAA heading. Use `onGloballyPositioned` and remove lazy-item entries on disposal. Convert coordinates to the Home root before passing them to `HomeGestureRegions`. Keep callbacks optional so other callers and current behavior are unchanged.

**Verify:** Compile the Android UI tests, run the new case on a device, and run the existing forecast and metric interaction cases.

---

### Task 10: Route background gestures from exposed regions

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add UI cases for dragging and pinching from exposed animation changing the saved view, tapping a forecast card still expanding it, tapping a condition badge still navigating, and dragging on foreground content still scrolling it. Add a double-tap-on-exposed-animation case that restores the square. Confirm the gesture cases fail before routing changes.

**Implementation:** Handle background-mode pointer input on the Home root, which is an ancestor of the scroll content. Accept a gesture only if it starts in an exposed region reported by `HomeGestureRegions`; consume only accepted movement, then forward zoom/pan or reset to the one Home-owned state. Do not put a full-screen sibling overlay above the controls. Keep SolarHero's direct stage gestures in square mode and disable its direct transform handler in background mode so a gesture cannot apply twice.

**Verify:** Run the gesture `HomeDashboardTest` cases on a device. Manually check both phone and expanded layouts.

---

### Task 11: Check transition and restore edge cases

**Files:** `app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt`, `app/src/main/java/ca/stewark/helioflux/ui/home/HomeSolarViewState.kt`, `app/src/test/java/ca/stewark/helioflux/ui/home/HomeSolarViewStateTest.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt`

**Test first:** Add cases for a pinch that stops below the entry threshold, a pinch that enters background and returns to 1×, a restored view after a viewport size change, and repeated mode switches leaving exactly one hero. Ensure the expected cases fail where behavior is still missing.

**Implementation:** Finish only the transition behavior those tests expose: settle near-1× gestures to 1×, constrain position after size changes, and make the mode switch after the active gesture completes so moving the hero does not interrupt that pinch. Keep the normal square and background states visually consistent through the switch.

**Verify:** Run the focused unit and UI cases; inspect a phone-sized and wide emulator/device for jumpiness and readable foreground text.

---

### Task 12: Final regression and visual verification

**Files:** No planned production changes.

**Test first:** No new behavior is introduced in this verification task. If a regression appears, add its failing test before fixing it in the relevant earlier task.

**Implementation:** Review the diff against the design. Confirm only the planned Home files, tests, and documentation changed. Check that the hero is never duplicated, the forecast card color remains unchanged, and no other destination or navigation surface was altered.

**Verify:** Run `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug`. Run `./gradlew :app:connectedDebugAndroidTest` if a device is available. Manually verify a fresh launch with saved zoom, 1× reset, compact/wide layouts, scrolling, card and badge actions, imagery loading, cached imagery, and animation quality at a high zoom.

## Definition of Done

- [ ] Tasks completed in order with each behavior test written before its implementation
- [ ] Solar animation appears once and remains fixed behind Home content only when zoomed
- [ ] Zoom and position survive a fresh launch and adapt to viewport changes
- [ ] Reset restores the square hero and clears saved position
- [ ] Foreground actions and scrolling still work
- [ ] Loading and cached imagery remain usable
- [ ] Focused tests and final Gradle checks pass; connected tests pass when a device is available
- [ ] No unrelated files modified
