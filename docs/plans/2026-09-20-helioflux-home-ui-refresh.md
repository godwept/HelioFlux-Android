# Home UI Refresh Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-helioflux-home-ui-refresh-design.md  
**Status:** Ready for review

## Overview

Refresh only the native Android Home dashboard to match the approved hybrid HelioFlux direction: a true-black screen, borderless/floating Sun, continuous hero loading feedback, branded HELIOFLUX masthead, compact scientific current-condition instrumentation, and compact NOAA forecast browsing. Preserve all stabilized repositories, data models, navigation destinations, hero gestures, 200 ms blend cadence, caching/failure behavior, and non-Home screens. Work in strict TDD order and keep commits/CI batches focused.

## Tasks

### Task 1: Define hero loading-state decisions
**Files:** app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt

**Test first:** Extend SolarHeroPlaybackTest with pure tests that establish four phases: InitialLoading when no frame exists, PosterLoading while frames exist but preload is incomplete, Playing when multiple usable frames are ready, and StaticPoster when only one usable frame exists. Keep the existing solarBlendProgress regression coverage.

**Implementation:** Add the smallest internal state representation needed by the tests, such as SolarHeroPhase plus solarHeroPhase(...). Do not change composable rendering yet.

**Verify:** Run ./gradlew testDebugUnitTest --tests "ca.stewark.helioflux.ui.home.SolarHeroPlaybackTest".

### Task 2: Keep the hero poster stable through preload
**Files:** app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt

**Test first:** Add coverage proving the visible poster is always selected from the repository frame list and does not depend on transient partial preload membership. Retain coverage that selectPlayableSolarFrames filters playback to successfully preloaded URLs.

**Implementation:** Refactor SolarHero so frames.first() (including retained repository data) is the stable poster while preload runs. Only switch to the existing current/next blended playback layers once the phase is Playing. Reset playback index/blend when repository frame identity changes, not merely because preload membership grows. Preserve the 200 ms blend loop.

**Verify:** Run the focused SolarHeroPlaybackTest command.

### Task 3: Render continuous loading feedback
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt

**Test first:** Add a HomeDashboardTest case that renders the existing RepositoryState.Loading contract and asserts solar-hero plus a new solar-hero-loading tag exist.

**Implementation:** Render a centered CircularProgressIndicator tagged solar-hero-loading during InitialLoading and PosterLoading. Keep the poster underneath during PosterLoading. Remove the spinner only in Playing or StaticPoster. Preserve the cached/failure message and all gestures/preload request behavior.

**Verify:** Run ./gradlew assembleDebugAndroidTest and the focused hero unit test.

### Task 4: Remove the hero Card and establish the black stage
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt

**Test first:** Add structural coverage asserting solar-hero and a new solar-hero-stage tag both exist.

**Implementation:** Replace the outer Material Card with a Box tagged solar-hero containing a square black stage tagged solar-hero-stage. Keep transformable/double-tap handling on the stage. Add no elevation, border, contrasting surface, or decorative container.

**Verify:** Run ./gradlew assembleDebugAndroidTest plus the focused hero unit test.

### Task 5: Establish the Home black canvas and masthead
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt

**Test first:** Add coverage asserting home-screen, home-masthead with text HELIOFLUX, and home-compact exist. Retain the existing home-expanded coverage.

**Implementation:** Apply a true-black background to the full Home content area. Replace HelioFlux with centered HELIOFLUX, tagged home-masthead, using strong weight, uppercase lettering, deliberate letter spacing, and the existing solar-orange theme color. Tighten vertical rhythm without reducing touch targets. Preserve compact order and expanded hero/conditions side-by-side structure. Add no font dependency.

**Verify:** Run ./gradlew assembleDebugAndroidTest.

### Task 6: Convert Current Conditions to compact instrumentation
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt

**Test first:** Extend HomeDashboardTest to assert metric-kp, metric-bz, metric-wind, and metric-flare all remain present, and verify the existing Kp tap still routes to SpaceWeather. Retain coverage for unavailable placeholders and rendered values.

**Implementation:** Remove the generic outer Card. Keep a compact Column tagged current-conditions with a smaller section heading and two-by-two metric layout. Retain each clickable metric and test tag. Use restrained dark instrumentation surfaces with no heavy elevation; use existing theme colors semantically (solar orange Kp, data cyan Bz, green/appropriate data accent wind, alert/solar accent flare). Make values stronger than labels. Keep geomagnetic status secondary. Preserve formatting and routing.

**Verify:** Run ./gradlew assembleDebugAndroidTest.

### Task 7: Add compact horizontal NOAA forecast browsing
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt, app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt

**Test first:** Keep the existing expand/collapse test and add coverage for forecast-section and forecast-track tags, then click forecast-solar and assert Forecast detail appears.

**Implementation:** Pass the existing expanded flag from HomeScreen into ForecastCards. In compact mode, keep the NOAA Forecast heading and render existing ForecastSection data in a LazyRow tagged forecast-track, sizing cards so another card is visibly available off-screen. Preserve title, summary, forecast, issueTime, forecast-<key> tags, and click-to-expand. Use restrained dark cards, compact labels, subdued issue time, and a quiet expand/collapse hint. Apply category accent only for clean existing key mappings; otherwise use a default existing accent. In expanded mode, show multiple cards without stretching one card across the full screen; keep the same interaction model. Invent no forecast fields.

**Verify:** Run ./gradlew assembleDebugAndroidTest.

### Task 8: Verify empty and partial Home states
**Files:** app/src/androidTest/java/ca/stewark/helioflux/ui/home/HomeDashboardTest.kt, app/src/main/java/ca/stewark/helioflux/ui/home/HomeScreen.kt, app/src/main/java/ca/stewark/helioflux/ui/home/ForecastCards.kt

**Test first:** Add regression coverage rendering HomeUiState defaults and asserting home-masthead, solar-hero, current-conditions, and forecast-section remain present, plus the existing unavailable metric placeholders.

**Implementation:** Make only the minimum guards needed so empty forecasts create no fabricated cards, condition placeholders remain visible, and compact/expanded composition does not depend on non-empty frame or forecast data.

**Verify:** Run ./gradlew assembleDebugAndroidTest and ./gradlew testDebugUnitTest.

### Task 9: Run the full local verification gate
**Files:** No production-file changes expected.

**Test first:** No new test; this is the verification gate for Tasks 1-8.

**Implementation:** Run:
- ./gradlew testDebugUnitTest
- ./gradlew assembleDebugAndroidTest
- ./gradlew lintDebug assembleDebug

If this Home pass causes a failure, make only the smallest correction in the planned files and rerun the failing command. Do not perform unrelated cleanup.

**Verify:** All three commands pass.

### Task 10: Push the Home UI batch and monitor CI
**Files:** Only the files planned above plus approved design/plan documentation.

**Test first:** Confirm the diff contains no unrelated files and Tasks 1-9 are green.

**Implementation:** Commit/push the completed Home UI batch with a focused message such as "feat: refresh home dashboard UI". Do not modify godwept/HelioFlux-web. Actively monitor the resulting GitHub Actions run in the same response, rechecking roughly every 45 seconds while pending. If CI fails, inspect logs, make the minimum focused correction, push, and continue until green or user input is genuinely required.

**Verify:** GitHub Actions completes GREEN for the implementation commit.

### Task 11: Physical Pixel acceptance pass
**Files:** No code changes unless physical testing exposes a reproducible defect.

**Test first:** On the physical Pixel verify: cold hero load; spinner remains until playback starts; first poster never blanks; hero and background form one continuous black field; pinch/pan/double-tap still work; condition metrics are readable and tappable; NOAA forecasts browse horizontally and expand/collapse; expanded/landscape composition works; cached/offline hero indication still appears where applicable.

**Implementation:** None if acceptance passes. If a defect appears, stop and route that defect through the project debugging workflow before changing code.

**Verify:** User confirms the Home screen matches the approved direction and the loading transition is visually continuous.

## Definition of Done
- [ ] All tasks completed in order
- [ ] Hero loading follows spinner -> poster + spinner -> animation with no blank transition
- [ ] Home and hero stage use a continuous true-black visual field
- [ ] Solar hero has no Card/container appearance
- [ ] Existing hero gestures and 200 ms blended playback remain intact
- [ ] HELIOFLUX masthead and compact conditions match the approved hybrid direction
- [ ] NOAA forecast uses compact browsable presentation without invented data
- [ ] Compact and expanded Home layouts remain functional
- [ ] All new behavior has focused regression coverage
- [ ] ./gradlew testDebugUnitTest passes
- [ ] ./gradlew assembleDebugAndroidTest passes
- [ ] ./gradlew lintDebug assembleDebug passes
- [ ] No unrelated files modified
- [ ] godwept/HelioFlux-web remains untouched
- [ ] GitHub Actions is green for the implementation commit
- [ ] Physical Pixel acceptance pass completed
