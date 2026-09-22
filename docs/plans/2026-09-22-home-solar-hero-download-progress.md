# Home Solar Hero Download Progress Implementation Plan

**Date:** 2026-09-22  
**Design:** `docs/specs/2026-09-22-home-solar-hero-download-progress-design.md`  
**Status:** Ready for implementation

## Goal

Add ENLIL-style `Loading frame X of Y` progress to the Home solar hero's existing concurrent frame preload without changing its repository contract, Coil pipeline, playback cadence, blending, caching, zoom/pan, or frame acceptance behavior.

## Task 1 — Add failing progress-format and preload-progress tests

**Files:**
- `app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt`

Add focused tests first for small deterministic helpers/contracts:

1. `solarHeroLoadingText(completed, total)` formats exactly `Loading frame 18 of 48`.
2. Empty totals do not produce `Loading frame 0 of 0` (use no progress text/null).
3. Add a testable preload seam/helper so a fake frame loader can prove:
   - progress begins at `0 of N`;
   - every successful attempt advances completion exactly once;
   - every failed attempt also advances completion exactly once;
   - total remains the selected frame count;
   - successful URLs retain source-frame ordering semantics.
4. Verify cancellation/new invocation cannot reuse a previous invocation's progress state; state reset itself remains owned by `LaunchedEffect(frames)`.

Run the focused test class and confirm the new tests fail for the expected missing behavior before production implementation.

**Verify:**
`./gradlew testDebugUnitTest --tests ca.stewark.helioflux.ui.home.SolarHeroPlaybackTest`

## Task 2 — Add minimal preload progress reporting

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt`

Implement only what the approved design requires:

1. Extend the private preload path with a progress callback carrying completed and total frame counts.
2. Preserve the existing `coroutineScope`, concurrent `async` frame requests, `SingletonImageLoader`, `solarFrameRequest()`, and successful-URL filtering.
3. Report initial progress as `0 of N` for a non-empty frame list.
4. Increment completed progress exactly once after each frame attempt finishes, regardless of `SuccessResult` or failure.
5. Keep progress reporting concurrency-safe. Do not serialize the existing concurrent preload just to simplify counting.
6. Keep the returned successful URL set and `selectPlayableSolarFrames()` behavior unchanged.
7. Add `solarHeroLoadingText(completedFrames, totalFrames)` as the smallest testable formatting helper. It returns no text for an empty total.

Run the focused tests until green.

## Task 3 — Wire transient progress into SolarHero UI

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt`

Use the existing `LaunchedEffect(frames)` lifecycle:

1. Add transient `completedFrames` state remembered by the current frame set.
2. At the start of each frame-set preload, reset completed progress to zero before work begins.
3. Feed the preload callback into that state.
4. Preserve the current `preloadComplete` and `preloadedUrls` ownership and cancellation boundary.
5. In the existing `solar-hero-loading` column, keep `CircularProgressIndicator()` unchanged and replace generic `Loading...` with `Loading frame X of Y` only when the current frame set is non-empty.
6. Do not render `0 of 0`.
7. Do not change the cached-imagery failure message or hero rendering/playback/interaction code.

Add/update source-contract coverage only where needed to verify the spinner remains and the exact progress wording/helper is wired. Avoid introducing a broad Compose test harness solely for this small presentation change.

Run the focused tests again.

## Task 4 — Regression verification

Run the normal CI-equivalent checks:

```bash
./gradlew testDebugUnitTest
./gradlew compileDebugAndroidTestKotlin
./gradlew lintDebug assembleDebug
```

Run connected instrumentation tests if a device is available, matching CI behavior.

Confirm no unrelated files were modified. Expected implementation files are only:

- `app/src/main/java/ca/stewark/helioflux/ui/home/SolarHero.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/home/SolarHeroPlaybackTest.kt`

The already-approved design and this plan are documentation-only supporting files.

## Task 5 — Commit, push, and monitor CI

Commit the implementation as one focused batch because the production and test changes are within the same Home hero subsystem.

After pushing, actively monitor the Android CI run approximately every 45 seconds until it is green. If CI fails:

1. inspect the exact failure;
2. add/adjust a regression test first when behavior is involved;
3. make the minimum correction;
4. push;
5. continue monitoring until green or user input is required.

## Task 6 — Physical Pixel acceptance

On a physical Pixel with an uncached Home hero load, verify:

- spinner appears as before;
- text begins at `Loading frame 0 of N`;
- frame count visibly advances as requests complete;
- failed individual frame attempts, if encountered, do not stall the completed count;
- progress disappears when preload completes;
- hero animation begins normally;
- blending/playback cadence is unchanged;
- zoom, pan, and double-tap reset remain unchanged;
- cached imagery failure messaging remains unchanged.

If physical testing exposes a reproducible progress issue, return to TDD and encode the failing case before changing implementation.

## Definition of Done

- [ ] Exact `Loading frame X of Y` wording
- [ ] Initial `0 of N` progress for non-empty preload
- [ ] Success and failure both advance completed count
- [ ] No `0 of 0`
- [ ] Progress resets for a new frame set
- [ ] Existing concurrent Coil preload preserved
- [ ] Existing successful-frame filtering/order preserved
- [ ] Existing spinner preserved
- [ ] Existing hero playback/blending/interactions unchanged
- [ ] Focused tests green
- [ ] Full CI-equivalent checks green
- [ ] No unrelated files modified
- [ ] Android CI green
- [ ] Physical Pixel acceptance completed
