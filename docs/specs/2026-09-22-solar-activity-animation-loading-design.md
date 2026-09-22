# Solar Activity Animation Loading Design

**Date:** 2026-09-22
**Status:** Approved

## Goal

Improve the perceived responsiveness and resource behavior of the Solar Activity LASCO C2, LASCO C3, and WSA-Enlil cards by using lower-resolution LASCO animations, exposing actual media-loading state to the user, and preventing ENLIL from issuing a large burst of simultaneous frame loads. The change should preserve current Solar Activity behavior, visual structure, caching, fullscreen viewing, and the working HMI implementation.

## Success Criteria

- [ ] LASCO C2 uses `current_c2small.gif` for both the card and fullscreen viewer.
- [ ] LASCO C3 uses `current_c3small.gif` for both the card and fullscreen viewer.
- [ ] C2 and C3 show a centered loading spinner until Coil has finished loading and decoding the complete GIF.
- [ ] C2 and C3 stop showing the spinner on either media success or media failure.
- [ ] ENLIL continues using the current sampled frame set rather than deliberately reducing frame count.
- [ ] ENLIL preloads the complete selected frame set before playback starts.
- [ ] ENLIL preload concurrency is capped at four frame loads at a time.
- [ ] ENLIL tolerates up to five failed frames; failed frames are dropped and the successful frames play in original order.
- [ ] ENLIL treats six or more preload failures as a failed animation and does not start playback.
- [ ] ENLIL shows a centered loading spinner until the complete preload pass finishes.
- [ ] Existing HMI, caching/failure messaging, refresh behavior, fullscreen behavior, and other Solar Activity functionality remain unchanged.

## Scope

**In scope:**

- Switch LASCO C2 from `current_c2.gif` to `current_c2small.gif`.
- Switch LASCO C3 from `current_c3.gif` to `current_c3small.gif`.
- Use the small LASCO GIFs consistently in normal-card and fullscreen contexts.
- Track actual Coil media loading separately from repository metadata readiness.
- Show a centered `CircularProgressIndicator` while C2/C3 media is still loading/decoding.
- Preserve the existing ENLIL sampled frame set.
- Preload the entire ENLIL frame set before playback with a maximum of four concurrent loads.
- Allow up to five ENLIL frame-load failures and play the remaining successful frames.
- Surface an ENLIL media failure when more than five frames fail.
- Continue relying on Coil's existing cache.

**Out of scope:**

- Changes to HMI.
- Changes to C2/C3 animation cadence or controls.
- Reducing ENLIL below the current sampled frame set.
- Adaptive quality based on Wi-Fi or mobile data.
- Progress percentages or byte-count displays.
- Imagery-card redesign.
- Cloudflare Worker changes.
- Changes to `godwept/HelioFlux-web`.
- General Solar Activity performance work outside C2, C3, and ENLIL.
- Automatic media retries, retry buttons, or fallback to the large LASCO GIFs.

## Design

### Repository and media responsibilities

The repository remains responsible for locating and exposing the media URL or ENLIL frame list. Media readiness belongs in the Compose/Coil layer because the repository only knows that metadata or URLs are available; it does not know when Coil has finished downloading and decoding the actual media.

No database schema changes, model changes, or persistent loading state are required.

### LASCO C2 and C3

The repository will refresh LASCO C2 and C3 using the SOHO small-animation filenames:

- `LATEST/current_c2small.gif`
- `LATEST/current_c3small.gif`

The resulting `SolarImage.url` is used by both the normal imagery card and fullscreen viewer, so both surfaces use the same lower-resolution animation.

Each displayed GIF has transient media state:

- **Loading** — a usable URL exists, but Coil is still downloading/decoding the GIF.
- **Ready** — Coil reports successful media load; the spinner disappears and the GIF is displayed.
- **Failed** — Coil reports media failure; the spinner disappears and an unavailable/error treatment is shown.

Repository `Loading` and Coil media `Loading` are distinct states. A repository may already be `Available` while the GIF itself is still loading.

### ENLIL preload behavior

The existing ENLIL parser continues selecting the current sampled frame set, currently capped at roughly 48 frames.

When the ENLIL URL list changes, the player resets transient preload state and starts a new preload pass. The old preload coroutine is cancelled through the existing Compose coroutine lifecycle.

The preload helper must:

- load no more than four ENLIL frames concurrently;
- wait until every requested frame has either succeeded or failed;
- count each failed frame once;
- preserve the original URL ordering for successful frames;
- return the successful frame URLs and total failure count.

Playback behavior:

- **0-5 failed frames:** drop failed frames, mark media Ready, and play the successful frame set in original order.
- **6 or more failed frames:** mark media Failed and do not start playback.
- **Single successful frame:** display the frame without entering the animation loop.
- Playback never begins while preload state is Loading.

The existing animation cadence and blend behavior are preserved.

### Shared card and fullscreen behavior

The card and fullscreen viewer use the same lower-resolution LASCO source URLs and the same media-readiness semantics.

For ENLIL, both card and fullscreen playback continue using Coil's cache. No new app-level disk or memory cache is introduced.

## Error Handling and Edge Cases

### C2/C3

- If no usable repository URL exists, retain the current unavailable behavior.
- If Coil fails while loading or decoding the small GIF, stop the spinner and show the unavailable/error treatment.
- If Coil satisfies the request from cache, transition to Ready as soon as Coil reports success.
- Pull-to-refresh continues refreshing repository metadata and does not introduce a separate media-cache invalidation mechanism.

### ENLIL

- Preload concurrency is capped at four.
- Failed frames are removed only after the complete preload pass has finished.
- Up to five failed frames are tolerated.
- Six or more failed frames produce a Failed media state.
- A new ENLIL run resets the preload state and cancels prior preload work.
- Leaving composition cancels preload work through normal coroutine lifecycle handling.
- Cached repository failure messaging remains intact and is independent from transient media readiness.

## Testing Strategy

### Repository tests

- Assert LASCO C2 refresh uses `current_c2small.gif`.
- Assert LASCO C3 refresh uses `current_c3small.gif`.
- Verify existing metadata/freshness behavior remains unchanged.

### C2/C3 presentation tests

- Verify media loading is distinct from repository `Loading`.
- Verify the spinner remains visible until Coil reports success.
- Verify media failure removes the spinner and exposes the unavailable/error state.
- Verify fullscreen uses the same small GIF URL as the card.

### ENLIL unit tests

Test bounded preload behavior independently from Compose:

- never exceed four concurrent loads;
- wait for the complete preload pass;
- return all successful frames;
- accept up to five failures;
- reject six or more failures;
- preserve original frame ordering after failed frames are removed.

Test playback-state behavior:

- playback does not begin before preload completion;
- accepted preload starts playback with successful frames only;
- a single successful frame displays without animation;
- a changed URL set resets preload state.

### Compose/UI tests

- C2/C3 loading spinner is visible while media is loading.
- ENLIL spinner is visible throughout full preload.
- The spinner disappears once an accepted preload completes.
- ENLIL failure state appears when more than five frames fail.

### Verification

Run focused unit/UI tests first, then the normal full Android test/build suite before pushing. After pushing, Android CI must complete successfully before the implementation is considered complete.

Physical Pixel verification will confirm:

- whether the small C2/C3 animations retain acceptable visual quality;
- whether the loading indicators make the wait understandable;
- whether ENLIL playback feels smooth after bounded full preload.

## Open Questions

None.
