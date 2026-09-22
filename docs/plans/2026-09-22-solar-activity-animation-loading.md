# Solar Activity Animation Loading Implementation Plan

**Date:** 2026-09-22  
**Design doc:** docs/specs/2026-09-22-solar-activity-animation-loading-design.md  
**Status:** Ready for review

## Overview

Improve Solar Activity animation loading without changing the existing HMI path or broader screen design. LASCO C2/C3 will switch to SOHO's small GIFs for both card and fullscreen use. C2/C3 will expose actual Coil media readiness so a centered spinner remains visible until decoding succeeds or fails. ENLIL will keep the current sampled frame set, but preload it with a four-request concurrency cap, tolerate up to five failed frames, and only begin playback after the entire preload pass completes. All work is test-first and limited to the existing Solar Activity/repository files.

## Tasks

### Task 1: Switch LASCO C2/C3 to the small GIF endpoints

**Files:**  
- `core/data/src/test/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepositoryTest.kt`  
- `core/data/src/main/java/ca/stewark/helioflux/core/data/repository/SolarImageryRepository.kt`

**Test first:**

Update `refreshAllUsesWorkingLascoAnimationEndpoints` so it expects:

```kotlin
assertEquals(
    HelioFluxEndpoints.lasco + "LATEST/current_c2small.gif",
    f.image.m[SolarImageType.LascoC2]?.value?.url,
)
assertEquals(
    HelioFluxEndpoints.lasco + "LATEST/current_c3small.gif",
    f.image.m[SolarImageType.LascoC3]?.value?.url,
)
```

Also update the C2 URL used by `headLastModifiedBecomesNormalizedTimestamp` to `current_c2small.gif` so the repository test suite consistently reflects the selected source.

Run the focused test and confirm it fails because production still references the large GIF filenames.

**Implementation:**

In `SolarImageryRepository.refreshAll()`, replace only:

- `LATEST/current_c2.gif` → `LATEST/current_c2small.gif`
- `LATEST/current_c3.gif` → `LATEST/current_c3small.gif`

Do not change `refreshImage`, freshness handling, database models, or Worker routing. Because the resulting `SolarImage.url` is already used by card and fullscreen paths, no separate fullscreen URL mapping is required.

**Verify:**

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest"
```

All `SolarImageryRepositoryTest` tests pass.

---

### Task 2: Add testable C2/C3 media-load state semantics

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt`

**Test first:**

Add unit coverage for a small transient media-state model that is independent of `RepositoryState`.

Cover these cases:

```kotlin
@Test
fun availableImageStartsInMediaLoadingState() {
    assertEquals(SolarMediaLoadState.Loading, initialSolarMediaLoadState("https://example.test/c2small.gif"))
}

@Test
fun absentImageDoesNotRemainMediaLoading() {
    assertEquals(SolarMediaLoadState.Failed, initialSolarMediaLoadState(null))
}

@Test
fun mediaCallbacksTransitionToReadyOrFailed() {
    assertEquals(
        SolarMediaLoadState.Ready,
        reduceSolarMediaLoadState(SolarMediaLoadState.Loading, SolarMediaLoadEvent.Success),
    )
    assertEquals(
        SolarMediaLoadState.Failed,
        reduceSolarMediaLoadState(SolarMediaLoadState.Loading, SolarMediaLoadEvent.Error),
    )
}
```

The important assertion is that an available repository image URL can still be in `SolarMediaLoadState.Loading`; repository availability must not imply decoded-media readiness.

Run the focused test and confirm it fails because the media-state types/helpers do not yet exist.

**Implementation:**

In `SolarImageryCard.kt`, add the minimum internal transient state definitions required by the tests:

- `SolarMediaLoadState.Loading`
- `SolarMediaLoadState.Ready`
- `SolarMediaLoadState.Failed`
- success/error events or an equivalent small reducer
- `initialSolarMediaLoadState(imageUrl)`

Keep this presentation-only. Do not add fields to `SolarImage`, `RepositoryState`, Room entities, or ViewModel state.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
```

All component unit tests pass.

---

### Task 3: Make the normal C2/C3 card spinner follow Coil media readiness

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt`

**Test first:**

Add pure/unit assertions for the display decision used by the card, for example:

```kotlin
@Test
fun availableRepositoryImageStillShowsSpinnerUntilMediaReady() {
    assertTrue(
        shouldShowSolarMediaSpinner(
            repositoryLoading = false,
            imageUrl = "https://example.test/c2small.gif",
            mediaState = SolarMediaLoadState.Loading,
        ),
    )
    assertFalse(
        shouldShowSolarMediaSpinner(
            repositoryLoading = false,
            imageUrl = "https://example.test/c2small.gif",
            mediaState = SolarMediaLoadState.Ready,
        ),
    )
}
```

Add a stable card spinner test tag such as `solar-media-loading` and an unavailable tag such as `solar-media-unavailable`. Extend the Compose test to verify the loading-state overlay exists when the card is given a loading repository state.

Run the focused tests before implementation.

**Implementation:**

In `SolarImageryCard`:

1. Create transient `mediaState` with `remember(item.imageUrl)`, initialized from `initialSolarMediaLoadState`.
2. Use Coil `AsyncImage` request callbacks:
   - `onLoading` → Loading
   - `onSuccess` → Ready
   - `onError` → Failed
3. Keep the centered `CircularProgressIndicator` visible when:
   - repository state is `RepositoryState.Loading`, or
   - a non-null image URL exists and media state is Loading.
4. Do not show the image-unavailable text while media is merely loading.
5. If Coil reports Failed, stop the spinner and show the existing unavailable treatment.
6. Preserve the overlay slot used by HMI; do not alter HMI coordinate/label behavior.

Do not add retry logic or fallback to the large GIF.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:assembleDebugAndroidTest
```

The unit suite passes and instrumentation tests compile.

---

### Task 4: Apply the same media-readiness behavior to fullscreen C2/C3

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Add/extend tests so the fullscreen image path is expected to use the same transient media states as the card. At minimum cover:

- non-null URL starts Loading;
- Ready removes the spinner;
- Failed removes the spinner and exposes unavailable treatment.

Add stable fullscreen tags such as:

- `fullscreen-solar-media-loading`
- `fullscreen-solar-media-unavailable`

Extend `SolarActivityUiTest` to open C2 and C3 fullscreen and assert the fullscreen viewer exists. The URL source remains the repository `SolarImage.url`; there must be no separate large-GIF substitution.

Run focused tests before implementation.

**Implementation:**

Update `ZoomableSolarImage` to:

1. remember media state keyed by `imageUrl`;
2. connect Coil `AsyncImage` loading/success/error callbacks to the shared media-state reducer;
3. show a centered spinner while the non-null image is loading;
4. hide the spinner on success;
5. show `Image unavailable` after media error or when `imageUrl == null`;
6. preserve the existing transform/zoom behavior and HMI overlay slot.

Do not change `SolarActivityScreen` URL selection except as necessary for test tags or wiring; C2/C3 fullscreen should continue receiving `state.lascoC2.screenData()?.url` and `state.lascoC3.screenData()?.url`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:assembleDebugAndroidTest
```

All focused tests pass/compile.

---

### Task 5: Define ENLIL preload result and acceptance rules

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`

**Test first:**

Replace the current poster-while-loading expectation because the approved design requires full preload before playback.

Add tests for a result model such as `EnlilPreloadResult(successfulUrls, failureCount)` and an acceptance helper.

Cover:

```kotlin
@Test
fun preloadAcceptsUpToFiveFailuresAndPreservesSuccessfulOrder() {
    val urls = listOf("one", "two", "three", "four")
    val result = EnlilPreloadResult(
        successfulUrls = listOf("one", "three", "four"),
        failureCount = 1,
    )

    assertEquals(listOf("one", "three", "four"), playableEnlilFrames(result))
    assertTrue(isAcceptedEnlilPreload(result))
}

@Test
fun preloadRejectsSixFailures() {
    assertFalse(
        isAcceptedEnlilPreload(
            EnlilPreloadResult(successfulUrls = listOf("one"), failureCount = 6),
        ),
    )
}
```

Also assert the exact policy constants:

- maximum concurrent loads = 4
- maximum tolerated failures = 5

Run the focused test and confirm it fails before implementation.

**Implementation:**

In `EnlilCard.kt`:

- add `EnlilPreloadResult` containing ordered `successfulUrls` and `failureCount`;
- add constants for concurrency 4 and tolerated failures 5;
- replace `selectPlayableEnlilFrames(urls, preloadedUrls)` poster fallback with helpers that only expose successful URLs after the preload result exists and is accepted;
- keep `enlilBlendProgress` unchanged.

A zero-success result must not be considered playable, even if its numeric failure count were otherwise within tolerance.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
```

All ENLIL playback-policy tests pass.

---

### Task 6: Bound ENLIL preload concurrency at four

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`

**Test first:**

Extract the network-independent preload orchestration behind a suspend helper that accepts a loader function:

```kotlin
suspend fun preloadEnlilUrls(
    urls: List<String>,
    maxConcurrency: Int = EnlilPreloadConcurrency,
    load: suspend (String) -> Boolean,
): EnlilPreloadResult
```

Add coroutine tests that prove:

1. peak in-flight loads never exceed 4;
2. the helper waits until every URL completes;
3. successful URLs retain the original input order even if completion order differs;
4. failures are counted exactly once.

Use `kotlinx.coroutines.test.runTest` and a controlled fake loader with counters/deferred gates; do not make network requests in unit tests.

Run the focused tests and confirm failure before production implementation.

**Implementation:**

Implement `preloadEnlilUrls` using structured concurrency and a bounded permit mechanism such as `kotlinx.coroutines.sync.Semaphore` + `withPermit`.

Requirements:

- launch work under the calling coroutine scope so cancellation propagates;
- cap active `load(url)` calls at `maxConcurrency`;
- await all inputs;
- build `successfulUrls` in original URL order, not completion order;
- count failures;
- do not stop early on the first failure.

Update the Android-specific `preloadEnlilFrames(context, urls)` adapter to call this helper with Coil's `ImageLoader.execute(ImageRequest)` success check.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
```

All bounded-preload tests pass.

---

### Task 7: Gate ENLIL playback behind the complete preload and add spinner/failure UI

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`

**Test first:**

Add pure playback-state tests covering:

- URL list present + no preload result → Loading and no playable frames;
- accepted result with 0-5 failures → Ready with only successful ordered frames;
- result with 6 failures → Failed and no playable frames;
- single successful frame → Ready but non-animating;
- a new URL list is treated as a new preload identity rather than reusing the previous result.

Add stable Compose tags:

- `enlil-loading`
- `enlil-unavailable`

Extend `SolarActivityUiTest` to assert that a repository Loading state exposes `enlil-loading` and a no-data non-loading state exposes the unavailable treatment. Keep actual bounded network orchestration covered by unit tests rather than external network calls in instrumentation tests.

Run focused tests before implementation.

**Implementation:**

Refactor `EnlilAnimation` transient state:

1. derive `urls` from current repository/cached frames;
2. `remember(urls)` a nullable `EnlilPreloadResult`;
3. in `LaunchedEffect(urls)`, reset the result to null, then run the bounded full preload when URLs are present;
4. rely on `LaunchedEffect` cancellation when `urls` changes or the composable leaves composition;
5. while repository data is loading or a non-empty URL set has no completed preload result, render the centered spinner and no ENLIL frame;
6. accepted result → render only `successfulUrls`;
7. rejected result (>5 failures or no successful frames) → render unavailable state and do not enter playback loop;
8. only enter the existing 200 ms blend loop when `visible` and there are at least two accepted playable frames;
9. one accepted frame renders statically;
10. preserve existing cached-data messaging in `EnlilCard`.

Do not alter `EnlilListingParser` sampling or animation cadence.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
./gradlew :app:assembleDebugAndroidTest
```

The focused unit tests pass and instrumentation tests compile.

---

### Task 8: Run Solar Activity regression tests and full local verification

**Files:** No production changes expected. Only fix files already listed above if a test exposes a regression caused by this plan.

**Test first:**

No new behavior is introduced in this task. Run the relevant existing tests to detect regressions:

```bash
./gradlew :core:data:testDebugUnitTest --tests "ca.stewark.helioflux.core.data.repository.SolarImageryRepositoryTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.*"
./gradlew :app:assembleDebugAndroidTest
```

If a test fails, diagnose the failure and make only the minimum correction within the files and behavior covered by this design.

**Implementation:**

After focused tests are green, run the same verification sequence as Android CI:

```bash
./gradlew projects
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

If an Android device is connected locally, also run:

```bash
./gradlew connectedDebugAndroidTest
```

Confirm no HMI implementation files or unrelated Solar Activity features were modified.

**Verify:**

All local verification commands pass.

---

### Task 9: Commit, push, and monitor Android CI

**Files:** Only the approved design/plan and files modified by Tasks 1-8.

**Test first:**

Before committing, inspect the final diff and confirm:

- no large-GIF LASCO endpoint remains in the Solar Activity refresh path;
- no HMI behavior was changed;
- no ENLIL frame-count reduction was introduced;
- no unrelated files were modified.

**Implementation:**

Create one focused implementation commit for the completed batch, push `main`, and monitor the triggered Android CI run.

While CI is pending, re-check the run during the current response. If it fails:

1. inspect the failed job/step logs;
2. reproduce or identify the failure;
3. add/fix tests first when behavior changes;
4. make the minimum correction;
5. push the correction;
6. continue monitoring until green.

**Verify:**

Android CI completes with `conclusion: success`.

Then physically verify on the Pixel:

- C2 small GIF quality is acceptable in card and fullscreen;
- C3 small GIF quality is acceptable in card and fullscreen;
- C2/C3 spinner remains visible until the GIF is actually ready;
- ENLIL spinner remains visible until the full bounded preload finishes;
- ENLIL begins smooth playback afterward;
- HMI still behaves exactly as previously verified.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] C2 uses `current_c2small.gif`.
- [ ] C3 uses `current_c3small.gif`.
- [ ] Card and fullscreen C2/C3 loading state follows actual Coil readiness.
- [ ] C2/C3 stop spinning on both success and failure.
- [ ] ENLIL keeps the current sampled frame set.
- [ ] ENLIL preload has a hard maximum of four concurrent loads.
- [ ] ENLIL waits for the complete preload pass before playback.
- [ ] ENLIL accepts 0-5 failed frames and preserves successful-frame order.
- [ ] ENLIL rejects 6+ failed frames.
- [ ] Single-frame ENLIL renders without animation.
- [ ] Existing HMI, refresh, cached-data messaging, zoom/fullscreen behavior, and unrelated Solar Activity functionality remain intact.
- [ ] All focused tests pass.
- [ ] Full Android CI-equivalent local verification passes.
- [ ] No unrelated files were modified.
- [ ] Android CI is green after the implementation push.
- [ ] Physical Pixel verification is completed for visual quality and loading behavior.
