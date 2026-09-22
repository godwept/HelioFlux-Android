# Solar Activity Download Progress Implementation Plan

**Date:** 2026-09-22  
**Design doc:** docs/specs/2026-09-22-solar-activity-download-progress-design.md  
**Status:** Ready for review

## Overview

Add real download progress for LASCO C2/C3 and completed-frame progress for ENLIL without introducing a second media download path. A small app-level image-download progress registry and OkHttp response-body wrapper will feed the existing Coil singleton used by `AsyncImage` and ENLIL preloading. C2/C3 will show downloaded KB while retaining the existing spinner; ENLIL will show `Loading frame X of Y` while preserving the current four-request concurrency cap, full-preload requirement, five-failure tolerance, frame ordering, caching, and playback cadence. HMI and the Home solar animation remain untouched.

## Tasks

### Task 1: Add the in-memory image download progress model and registry

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/imageloading/ImageDownloadProgress.kt` — new  
- `app/src/test/java/ca/stewark/helioflux/imageloading/ImageDownloadProgressTest.kt` — new

**Test first:**

Create `ImageDownloadProgressTest.kt` and add focused registry tests before the production file exists:

```kotlin
@Test
fun registryPublishesProgressByUrl() = runTest {
    val registry = ImageDownloadProgressRegistry()
    val url = "https://example.test/c2small.gif"

    registry.update(url, bytesRead = 8_420L * 1024L, totalBytes = 17_860L * 1024L)

    assertEquals(
        ImageDownloadProgress(
            bytesRead = 8_420L * 1024L,
            totalBytes = 17_860L * 1024L,
        ),
        registry.observe(url).first(),
    )
}

@Test
fun registryKeepsUrlsIndependentAndClearsOnlyRequestedUrl() = runTest {
    val registry = ImageDownloadProgressRegistry()
    registry.update("c2", 1024L, 2048L)
    registry.update("c3", 4096L, null)

    registry.clear("c2")

    assertNull(registry.observe("c2").first())
    assertEquals(ImageDownloadProgress(4096L, null), registry.observe("c3").first())
}
```

Run the focused test and confirm it fails because the model/registry do not yet exist.

**Implementation:**

Create `ImageDownloadProgress.kt` in package `ca.stewark.helioflux.imageloading` with:

- `internal data class ImageDownloadProgress(val bytesRead: Long, val totalBytes: Long?)`
- `internal class ImageDownloadProgressRegistry`
- a private `MutableStateFlow<Map<String, ImageDownloadProgress>>`
- `fun observe(url: String): Flow<ImageDownloadProgress?>` implemented by mapping the URL entry and applying `distinctUntilChanged()`
- `fun update(url: String, bytesRead: Long, totalBytes: Long?)`
- `fun clear(url: String)`
- one process-local shared instance named `imageDownloadProgressRegistry`

Normalize invalid totals when publishing: only retain `totalBytes` when it is greater than zero; otherwise store `null`.

Do not add Room state, repository state, persistence, percentage calculation, speed, or ETA.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.imageloading.ImageDownloadProgressTest"
```

All new registry tests pass.

---

### Task 2: Report actual bytes while Coil consumes an OkHttp response body

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/imageloading/ImageDownloadProgress.kt`  
- `app/src/test/java/ca/stewark/helioflux/imageloading/ImageDownloadProgressTest.kt`

**Test first:**

Extend `ImageDownloadProgressTest.kt` with response-body tests using OkHttp/Okio directly; do not add MockWebServer or make real network requests.

Cover known content length:

```kotlin
@Test
fun progressResponseBodyReportsIncrementalBytesAndKnownLength() = runTest {
    val registry = ImageDownloadProgressRegistry()
    val url = "https://example.test/c2small.gif"
    val delegate = ByteArray(4096).toResponseBody()

    val body = ProgressResponseBody(url, delegate, registry)
    val sink = Buffer()

    body.source().read(sink, 1024)

    assertEquals(
        ImageDownloadProgress(bytesRead = 1024L, totalBytes = 4096L),
        registry.observe(url).first(),
    )
}
```

Also add tests proving:

- a delegate with unknown `contentLength()` publishes `totalBytes = null`;
- successive reads publish cumulative bytes rather than per-read bytes;
- closing before EOF clears the URL entry;
- a read exception clears the URL entry and rethrows the exception;
- reaching EOF leaves the final byte count available until Coil reports media success/failure, so decoding can finish while the last download count remains visible.

Run the focused tests and confirm they fail before implementation.

**Implementation:**

In `ImageDownloadProgress.kt`, add:

- `internal class ProgressResponseBody(...): ResponseBody`
- delegate `contentType()` and `contentLength()`
- lazily wrap `delegate.source()` with an Okio `ForwardingSource`
- maintain cumulative `bytesRead`
- on every positive read, publish `bytesRead` plus `delegate.contentLength().takeIf { it > 0L }`
- on EOF, mark the wrapper complete but do not clear the final progress entry
- on exception, clear the URL then rethrow
- on `close()` before EOF, clear the URL
- on normal `close()` after EOF, retain progress until the media callback clears it

Add `internal class ImageDownloadProgressInterceptor(private val registry: ImageDownloadProgressRegistry) : Interceptor`:

1. capture the request URL string before `chain.proceed`;
2. clear stale progress for that URL before starting the network request;
3. call `chain.proceed(request)`;
4. if the response body is non-null, replace it with `ProgressResponseBody(url, body, registry)`;
5. if `chain.proceed` throws, clear the URL and rethrow.

The interceptor only observes the existing response stream; it must never start its own request.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.imageloading.ImageDownloadProgressTest"
```

All response-body and registry tests pass.

---

### Task 3: Make the app’s Coil singleton use the progress-aware OkHttp path

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/AppContainer.kt`  
- `app/src/main/java/ca/stewark/helioflux/HelioFluxApplication.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/NetworkImageLoadingDependencyTest.kt`

**Test first:**

Extend `NetworkImageLoadingDependencyTest.kt` using the project’s existing source-policy test style.

Read:

- `src/main/java/ca/stewark/helioflux/AppContainer.kt`
- `src/main/java/ca/stewark/helioflux/HelioFluxApplication.kt`

Assert that the intended wiring exists:

```kotlin
@Test
fun coilSingletonUsesProgressAwareOkHttpFetcher() {
    assertTrue(containerSource.contains("OkHttpNetworkFetcherFactory"))
    assertTrue(containerSource.contains("ImageDownloadProgressInterceptor"))
    assertTrue(applicationSource.contains("SingletonImageLoader.setSafe"))
    assertTrue(applicationSource.contains("container.imageLoader"))
}
```

Keep the existing dependency checks for `coil-network-okhttp` and `coil-gif`.

Run the focused test and confirm it fails before wiring.

**Implementation:**

In `AppContainer.kt`:

- import `imageDownloadProgressRegistry` and `ImageDownloadProgressInterceptor`;
- import `coil3.network.okhttp.OkHttpNetworkFetcherFactory`;
- import `okhttp3.OkHttpClient`;
- replace the current bare `ImageLoader.Builder(context).build()` with:

```kotlin
ImageLoader.Builder(context)
    .components {
        add(
            OkHttpNetworkFetcherFactory(
                callFactory = {
                    OkHttpClient.Builder()
                        .addInterceptor(
                            ImageDownloadProgressInterceptor(imageDownloadProgressRegistry),
                        )
                        .build()
                },
            ),
        )
    }
    .build()
```

Do not change Coil, OkHttp, or Gradle dependency versions.

In `HelioFluxApplication.onCreate()`, immediately after `container = AppContainer.create(this)`, configure Coil’s singleton:

```kotlin
SingletonImageLoader.setSafe { container.imageLoader }
```

This ensures both Compose `AsyncImage` and existing `SingletonImageLoader.get(context)` calls, including ENLIL preload, use the same configured loader and normal Coil memory/disk caching.

Do not create a second ImageLoader for Solar Activity and do not add a separate GIF downloader.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.NetworkImageLoadingDependencyTest"
./gradlew :app:assembleDebug
```

The wiring test and app compilation pass.

---

### Task 4: Format C2/C3 KB progress and add the shared loading indicator

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

In `SolarActivityComponentsTest.kt`, add formatting tests using exact byte values:

```kotlin
@Test
fun solarDownloadProgressFormatsKnownTotalInKilobytes() {
    assertEquals(
        "Downloading… 8,420 KB / 17,860 KB",
        formatSolarDownloadProgress(
            ImageDownloadProgress(
                bytesRead = 8_420L * 1024L,
                totalBytes = 17_860L * 1024L,
            ),
        ),
    )
}

@Test
fun solarDownloadProgressFormatsUnknownTotalWithDownloadedBytesOnly() {
    assertEquals(
        "Downloading… 8,420 KB",
        formatSolarDownloadProgress(
            ImageDownloadProgress(
                bytesRead = 8_420L * 1024L,
                totalBytes = null,
            ),
        ),
    )
}
```

Use integer `bytes / 1024` conversion and comma-grouped integer output so the implementation matches the approved display examples.

In `SolarActivityUiTest.kt`, add a direct Compose test for the shared loading indicator:

- render the indicator with `Downloading… 8,420 KB / 17,860 KB`;
- assert the spinner/loading test tag exists;
- assert the progress text exists.

Run the focused unit test and instrumentation-test compilation before implementation.

**Implementation:**

In `SolarImageryCard.kt`:

1. import `ImageDownloadProgress`, `imageDownloadProgressRegistry`, `flowOf`, and `collectAsState`;
2. add `internal fun formatSolarDownloadProgress(progress: ImageDownloadProgress): String`;
3. format KB as integer `bytes / 1024L`;
4. use a fixed comma-grouped formatter matching the approved English display examples;
5. add a small shared composable `SolarMediaLoadingIndicator(progressText, loadingTag, progressTag)` that renders:
   - the existing `CircularProgressIndicator`;
   - optional progress text underneath with modest spacing and `SpaceMuted`;
6. preserve existing loading tags when moving the spinner into the shared indicator.

Also add:

```kotlin
@Composable
internal fun rememberSolarDownloadProgress(
    imageUrl: String?,
    enabled: Boolean,
): ImageDownloadProgress?
```

It should collect `imageDownloadProgressRegistry.observe(imageUrl)` only when tracking is enabled and the URL is non-null; otherwise collect a `flowOf(null)`.

This helper observes existing network activity only. It must not create an image request.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:assembleDebugAndroidTest
```

Formatting tests pass and instrumentation tests compile.

---

### Task 5: Show byte progress on the normal C2/C3 card

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Add/extend UI coverage for the card loading overlay using the shared indicator from Task 4.

The expected card semantics are:

- `solar-media-loading` remains present while loading;
- progress text uses test tag `solar-media-download-progress`;
- the text is absent when no registry progress exists, preserving spinner-only cached/metadata behavior.

Keep the existing `c2RepositoryLoadingShowsMediaSpinner` regression test.

Run `assembleDebugAndroidTest` before implementation.

**Implementation:**

Inside `SolarImageryCard`:

1. call `rememberSolarDownloadProgress(item.imageUrl, trackMediaLoading)`;
2. only create progress text when:
   - media state is `Loading`, and
   - registry progress for the current URL is non-null;
3. replace the current standalone spinner with:

```kotlin
SolarMediaLoadingIndicator(
    progressText = downloadProgress?.let(::formatSolarDownloadProgress),
    loadingTag = "solar-media-loading",
    progressTag = "solar-media-download-progress",
)
```

4. in `AsyncImage.onSuccess`, clear `imageDownloadProgressRegistry` for the captured URL before/while moving media state to Ready;
5. in `AsyncImage.onError`, clear the same URL before/while moving media state to Failed;
6. leave `onLoading`, cached-image messaging, unavailable treatment, click behavior, and HMI’s `trackMediaLoading = false` behavior unchanged.

Do not add progress handling to `MagnetogramCard`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:assembleDebugAndroidTest
```

Card behavior compiles and all focused tests pass.

---

### Task 6: Show the same byte progress in fullscreen C2/C3

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Extend the shared loading-indicator UI coverage so fullscreen uses distinct stable tags:

- `fullscreen-solar-media-loading`
- `fullscreen-solar-media-download-progress`

Keep the existing `c2AndC3OpenFullscreenViewer` regression test.

Run `assembleDebugAndroidTest` before implementation.

**Implementation:**

In `ZoomableSolarImage`:

1. collect the same `rememberSolarDownloadProgress(imageUrl, trackMediaLoading)` source used by the card;
2. replace the standalone fullscreen spinner with `SolarMediaLoadingIndicator`;
3. pass `fullscreen-solar-media-loading` and `fullscreen-solar-media-download-progress` tags;
4. clear `imageDownloadProgressRegistry` for the captured URL from both `onSuccess` and `onError`;
5. preserve the existing transform state, pinch/pan behavior, unavailable treatment, and overlay slot.

No `SolarActivityScreen.kt` changes should be needed because C2/C3 already pass `trackMediaLoading = true` into `ZoomableSolarImage`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:assembleDebugAndroidTest
```

Fullscreen progress wiring compiles and existing fullscreen tests remain intact.

---

### Task 7: Emit monotonic ENLIL completed-frame progress from the bounded preload helper

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`

**Test first:**

Extend `EnlilPlaybackTest.kt` before changing the helper.

Add a progress-sequence test:

```kotlin
@Test
fun boundedPreloadReportsZeroThenEveryCompletedFrame() = runTest {
    val updates = mutableListOf<Pair<Int, Int>>()
    val urls = listOf("one", "two", "three")

    preloadEnlilUrls(
        urls = urls,
        load = { url ->
            delay(if (url == "one") 20 else 5)
            url != "two"
        },
        onProgress = { completed, total ->
            updates += completed to total
        },
    )

    assertEquals(
        listOf(0 to 3, 1 to 3, 2 to 3, 3 to 3),
        updates,
    )
}
```

Also add tests proving:

- a failed frame still advances the completed count;
- the existing maximum concurrency remains four;
- final successful URLs remain in input order;
- cancellation after the initial `0 of N` notification produces no later progress callbacks.

For cancellation, launch `preloadEnlilUrls` with a loader suspended in `awaitCancellation()`, wait until loading starts, cancel/join the job, and assert the progress list contains only the initial `0 to N`.

Run the focused test and confirm failure before implementation.

**Implementation:**

Change `preloadEnlilUrls` to accept:

```kotlin
onProgress: (completedFrames: Int, totalFrames: Int) -> Unit = { _, _ -> },
```

Behavior:

1. for a non-empty URL list, invoke `onProgress(0, urls.size)` before launching frame loads;
2. keep `Semaphore(EnlilPreloadConcurrency)` unchanged;
3. keep cancellation propagation unchanged;
4. after each load attempt returns success or a non-cancellation failure is converted to `false`, increment a shared completed counter exactly once;
5. protect increment + callback with a `Mutex` so callbacks are monotonic even when several loads finish together;
6. call `onProgress(completed, urls.size)`;
7. do not emit a completion increment for a frame cancelled by `CancellationException`;
8. keep the result calculation and successful URL input-order preservation unchanged.

Update `preloadEnlilFrames(context, urls, onProgress)` to forward the callback while retaining the same Coil request.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
```

All old and new ENLIL preload tests pass.

---

### Task 8: Render ENLIL frame progress throughout preload

**Files:**  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

In `EnlilPlaybackTest.kt`, add a pure formatter test:

```kotlin
@Test
fun enlilProgressFormatsCompletedFrames() {
    assertEquals("Loading frame 18 of 48", formatEnlilProgress(18, 48))
}
```

In `SolarActivityUiTest.kt`, add a direct Compose test for the ENLIL loading indicator:

- render it with `completedFrames = 18`, `totalFrames = 48`;
- assert `enlil-loading` exists;
- assert `enlil-progress` exists;
- assert the text is `Loading frame 18 of 48`.

Keep the existing repository-loading spinner test, which should continue showing a spinner without `0 of 0`.

Run the focused unit test and instrumentation-test compilation before implementation.

**Implementation:**

In `EnlilCard.kt`:

1. add `internal fun formatEnlilProgress(completedFrames: Int, totalFrames: Int): String`;
2. add a small `EnlilLoadingIndicator(completedFrames: Int, totalFrames: Int)` composable:
   - always render the existing spinner;
   - only render progress text when `totalFrames > 0`;
   - tag the container/spinner path with existing `enlil-loading`;
   - tag the text with `enlil-progress`;
3. in `EnlilAnimation`, add `completedFrames` state remembered by `urls`;
4. reset it to zero whenever `urls` changes;
5. pass the preload callback into `preloadEnlilFrames` and assign each completed count to the Compose state;
6. while `EnlilMediaLoadState.Loading`, render `EnlilLoadingIndicator(completedFrames, urls.size)`;
7. when preload becomes Ready or Failed, the loading branch disappears naturally, so no separate persisted progress cleanup is required.

Do not change:

- four-request preload concurrency;
- five-failure tolerance;
- successful-frame ordering;
- 200 ms animation cadence;
- cached repository messaging;
- fullscreen ENLIL wiring, which already reuses `EnlilAnimation`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
./gradlew :app:assembleDebugAndroidTest
```

ENLIL unit tests pass and instrumentation tests compile.

---

### Task 9: Run focused regression coverage and full local verification

**Files:** No new files expected. Only correct files already listed above if verification exposes a regression caused by this feature.

**Test first:**

Run the focused suites:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.imageloading.ImageDownloadProgressTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.NetworkImageLoadingDependencyTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.*"
./gradlew :app:assembleDebugAndroidTest
```

If any focused test is red, diagnose it before proceeding and make only the minimum correction within this design.

**Implementation:**

After focused coverage is green, run the same local verification sequence used by Android CI:

```bash
./gradlew projects
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

If an Android device is connected locally:

```bash
./gradlew connectedDebugAndroidTest
```

Inspect the final diff and confirm:

- no HMI files changed;
- no Home solar animation files changed;
- no repository/database/Worker/PWA changes were added;
- no Coil or OkHttp version upgrade was made;
- no second GIF download path exists;
- existing C2/C3 small GIF URLs remain unchanged;
- existing ENLIL preload/failure/playback rules remain unchanged except for progress callbacks.

**Verify:**

All local verification commands pass.

---

### Task 10: Commit once, push, and monitor Android CI

**Files:** Only the approved design/plan and implementation/test files from Tasks 1-9.

**Test first:**

Before committing, confirm the final diff contains only:

- the new image progress infrastructure/test files;
- `AppContainer.kt`;
- `HelioFluxApplication.kt`;
- `NetworkImageLoadingDependencyTest.kt`;
- `SolarImageryCard.kt`;
- `FullscreenImageryViewer.kt`;
- `EnlilCard.kt`;
- the listed Solar Activity tests.

**Implementation:**

To minimize GitHub Actions usage, squash/commit the completed implementation as one focused batch on `main`, then push once.

Monitor the triggered Android CI run during the implementation response. While pending, continue checking the run. If CI fails:

1. inspect the failed job/step logs;
2. reproduce or identify the failure;
3. add/fix tests first if behavior changes;
4. make the minimum correction;
5. push the correction;
6. continue monitoring until green.

**Verify:**

Android CI completes with `conclusion: success`.

Then physically verify on the Pixel:

- C2 card shows `Downloading… X KB / Y KB` when total length is available;
- C3 card shows the same;
- unknown-length media falls back to `Downloading… X KB`;
- fullscreen C2/C3 use the same byte progress;
- cached C2/C3 loads do not invent download counts;
- spinner remains visible through download/decode;
- ENLIL visibly advances from `Loading frame 0 of N` through completion;
- failed ENLIL frames still advance the completed count;
- accepted ENLIL preload begins playback and removes progress text;
- rejected ENLIL preload shows unavailable;
- HMI remains unchanged.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] C2/C3 report real bytes read from the existing Coil/OkHttp transfer.
- [ ] Known total displays `Downloading… X KB / Y KB`.
- [ ] Unknown total displays `Downloading… X KB`.
- [ ] Card and fullscreen C2/C3 observe the same URL-keyed progress registry.
- [ ] Existing C2/C3 spinners remain visible during loading.
- [ ] Cached loads do not fabricate network progress.
- [ ] Media success/failure clears C2/C3 progress.
- [ ] Cancellation/error clears stale network progress.
- [ ] ENLIL reports `Loading frame X of Y`.
- [ ] ENLIL success and failure attempts both advance completed-frame progress.
- [ ] ENLIL concurrency remains capped at four.
- [ ] ENLIL still waits for the full preload pass.
- [ ] ENLIL still accepts at most five failed frames.
- [ ] ENLIL successful-frame ordering and playback cadence remain unchanged.
- [ ] HMI and Home solar animation are unchanged.
- [ ] No dependency versions changed.
- [ ] All focused tests pass.
- [ ] Full Android CI-equivalent verification passes.
- [ ] No unrelated files are modified.
- [ ] Android CI is green after the implementation push.
- [ ] Physical Pixel verification confirms progress presentation and existing playback behavior.
