# Solar Activity Download Progress Design

**Date:** 2026-09-22
**Status:** Approved

## Goal

Make long Solar Activity media loads visibly progressive rather than appearing stalled.

LASCO C2 and C3 should report actual downloaded kilobytes from the existing Coil/OkHttp transfer. ENLIL should report completed-frame preload progress. The feature must preserve the existing media pipeline, caching, playback rules, fullscreen behavior, and HMI behavior.

## Success Criteria

- [ ] C2 and C3 show actual downloaded KB while their GIFs are loading.
- [ ] When total size is known, C2/C3 show `Downloading… 8,420 KB / 17,860 KB`.
- [ ] When total size is unknown, C2/C3 show `Downloading… 8,420 KB`.
- [ ] C2/C3 card and fullscreen surfaces use the same download-progress source.
- [ ] Existing C2/C3 loading spinners remain visible during media loading.
- [ ] Cached C2/C3 loads do not fabricate network byte progress.
- [ ] ENLIL shows completed-frame progress such as `Loading frame 18 of 48`.
- [ ] ENLIL progress advances after each frame attempt completes, whether that frame succeeds or fails.
- [ ] ENLIL still preloads the full selected frame set before playback.
- [ ] Existing ENLIL maximum concurrency of four is unchanged.
- [ ] Existing ENLIL tolerance of up to five failed frames is unchanged.
- [ ] Progress is cleared on completion, failure, cancellation, or source change.
- [ ] HMI and the Home solar animation remain unchanged.

## Scope

### In scope

- C2/C3 download progress based on actual bytes read by the existing Coil/OkHttp request.
- C2/C3 progress text:
  - known total: `Downloading… 8,420 KB / 17,860 KB`
  - unknown total: `Downloading… 8,420 KB`
- ENLIL preload progress based on completed frames:
  - `Loading frame 18 of 48`
- Progress reset when the media URL or ENLIL frame set changes.
- Progress removal on media success or failure.
- Keep the existing spinner alongside progress text.
- Card and fullscreen C2/C3 use the same progress source.
- Existing ENLIL bounded concurrency, full-preload requirement, failure tolerance, cache usage, and playback behavior remain unchanged.

### Out of scope

- Download speed such as `KB/s`.
- ETA or remaining time.
- Percentage-only displays.
- Pause/cancel controls.
- Retry buttons.
- Persisting progress across app restarts.
- Progress tracking for HMI.
- Progress tracking for the Home solar animation.
- Changing GIF or ENLIL frame quality.
- Redesigning Solar Activity cards.
- Repository API changes.
- Database schema changes.
- Worker or PWA changes.

## Data and State

### C2/C3

Add transient download-progress state alongside the existing media loading state:

- `bytesRead`
- `totalBytes` when known
- the current media URL identity

This state is presentation/runtime-only and is not stored in Room, repository models, or persistent app state.

The existing media states remain:

- `Loading`
- `Ready`
- `Failed`

While media state is `Loading`, the UI formats progress as:

- known total: `Downloading… 8,420 KB / 17,860 KB`
- unknown total: `Downloading… 8,420 KB`

When Coil reports success or failure, the progress entry is cleared.

### ENLIL

Extend transient preload state to track:

- total selected frames
- completed frame count
- successful frame count
- failed frame count

Displayed progress uses completed frames rather than successful frames. A failed frame still increments the completed count because its preload work has finished.

Example:

`Loading frame 18 of 48`

After all frames complete, existing acceptance rules remain unchanged:

- 0-5 failures: play the successful frames in original order.
- 6 or more failures: show failed/unavailable state.

No progress state survives navigation, process death, refresh to a different source, or app restart.

## Interfaces and Implementation Boundaries

### C2/C3

Progress belongs at the media-loading boundary rather than in the repository.

The intended responsibility split is:

- Coil continues to own image decoding, memory caching, and disk caching.
- OkHttp continues to perform the network transfer.
- Add a small progress-aware response-body wrapper/interceptor to the HTTP client used by Coil.
- The wrapper reports:
  - request URL
  - cumulative bytes read
  - total content length when available
  - completion/error
- A lightweight in-memory progress registry maps active media URLs to current download progress.
- `SolarImageryCard` and `ZoomableSolarImage` observe progress for their current URL and format the KB text.

The implementation must not introduce a separate GIF download. The existing Coil request remains the single media transfer and cache path.

### ENLIL

No networking abstraction change is required.

The existing bounded preload helper should expose progress through a callback conceptually equivalent to:

`onProgress(completedFrames, totalFrames)`

The callback fires after each frame attempt succeeds or fails.

Compose stores the transient completed count and renders `Loading frame X of Y`.

The preload result remains the sole authority for playback acceptance. Progress reporting does not alter failure tolerance, ordering, concurrency, or playback cadence.

## Error Handling and Edge Cases

### C2/C3

- If the response has a valid `Content-Length`, display downloaded and total KB.
- If total size is unknown, display downloaded KB only.
- If Coil serves the media from memory or disk cache without a network transfer, do not fabricate byte progress.
- Progress resets when the URL changes.
- Failed or cancelled downloads clear their progress entry.
- Multiple UI surfaces observing the same URL share the same progress state rather than initiating duplicate transfers.
- Progress reporting must not interfere with Coil cache behavior.
- Existing unavailable treatment remains responsible for failed media.

### ENLIL

- Progress starts at `Loading frame 0 of N`.
- Each completed frame attempt increments the completed count exactly once.
- Successful and failed frame attempts both advance progress.
- If preload is cancelled because the URL set changes or composition ends, the previous progress is discarded.
- On accepted preload, progress disappears and playback begins.
- On rejected preload, progress disappears and unavailable state appears.
- A single selected frame still reports preload progress, then displays statically.
- An empty frame list continues to show unavailable rather than `0 of 0`.

No retry loops, ETA estimation, synthetic percentages, or stale progress persistence are added.

## Testing Strategy

### C2/C3 progress unit tests

Test the progress-reporting layer independently from real network UI behavior:

- cumulative byte counts are reported correctly as the response body is read;
- total bytes are reported when `Content-Length` is known;
- unknown content length remains absent;
- progress is keyed by URL;
- progress resets and clears on completion;
- progress clears on failure;
- progress clears on cancellation;
- URL changes do not retain stale values;
- formatting produces:
  - `Downloading… 8,420 KB / 17,860 KB`
  - `Downloading… 8,420 KB`
- cached/no-network loads do not expose stale network progress.

### C2/C3 Compose tests

Verify:

- byte-progress text appears while tracked network loading is active;
- progress disappears after media success;
- progress disappears after media failure;
- the existing spinner remains present during loading;
- card and fullscreen use the same progress source;
- HMI remains unaffected.

### ENLIL unit tests

Extend the existing bounded-preload coverage to verify:

- progress begins at 0 of N;
- each completed frame advances the count exactly once;
- success and failure both advance progress;
- maximum concurrency remains four;
- completion order does not alter final successful-frame order;
- cancellation stops further progress updates;
- existing five-failure tolerance remains unchanged.

### ENLIL Compose tests

Verify:

- frame-progress text is visible throughout preload;
- displayed count updates as frames complete;
- progress disappears when accepted preload begins playback;
- rejected preload transitions to unavailable state.

### Full verification

Run focused tests first, then the normal Android CI-equivalent suite:

- unit tests
- instrumentation-test compilation
- connected instrumentation tests when a device is available
- lint
- debug APK build

After the implementation push, monitor Android CI until completion.

Physical Pixel verification should confirm:

- C2/C3 KB text updates smoothly and remains readable during downloads;
- cached loads do not show misleading byte progress;
- ENLIL frame count visibly advances during preload;
- progress disappears cleanly when media becomes ready;
- existing C2/C3/ENLIL playback remains correct;
- HMI remains unchanged.

## Open Questions

None.
