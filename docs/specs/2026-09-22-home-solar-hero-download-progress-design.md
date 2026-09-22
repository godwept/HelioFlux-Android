# Home Solar Hero Download Progress Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal

Make Home solar hero loading visibly progressive while its animation frames preload, using the same user-facing frame-progress pattern already established for ENLIL.

Instead of the current generic `Loading...` text, the Home hero should display progress such as:

`Loading frame 18 of 48`

The existing spinner remains visible.

## Success Criteria

- The Home solar hero displays `Loading frame X of Y` while animation frames preload.
- Progress starts at `Loading frame 0 of N`.
- Each completed frame attempt advances the completed count exactly once, whether that frame succeeds or fails.
- Progress disappears when preload completes and normal hero playback begins.
- Progress resets when the hero frame set changes.
- Cancelled/replaced preload work cannot leave stale progress visible.
- Empty frame sets do not display `Loading frame 0 of 0`.
- Existing frame selection, preload behavior, playback cadence, blending, caching, zoom/pan, and failure treatment remain unchanged.
- ENLIL and Solar Activity media behavior remain unchanged.

## Scope

### In scope

- Home solar hero only.
- Add transient completed-frame progress to the existing `preloadSolarFrames()` operation.
- Replace the generic Home hero `Loading...` text with `Loading frame X of Y` while a non-empty frame set is actively preloading.
- Preserve the existing `CircularProgressIndicator`.
- Focused unit/UI contract coverage for the progress behavior.

### Out of scope

- Changes to ENLIL.
- Changes to Solar Activity imagery.
- Repository API changes.
- Database/schema changes.
- A second download/preload pipeline.
- Byte-level progress, percentages, speed, ETA, pause, cancel, or retry controls.
- Changes to hero image quality.
- Changes to playback timing or frame blending.
- Changes to hero zoom/pan behavior.
- PWA changes.

## Data and State

Add transient Compose preload progress alongside the existing Home hero preload state:

- total frame count: derived from the current `frames` collection
- completed frame count: initially zero for each new frame set
- existing `preloadedUrls`
- existing `preloadComplete`

Progress is runtime/presentation state only. It is not persisted and does not belong in repository or database models.

When a new frame set is received:

1. reset completed count to zero;
2. reset `preloadComplete` to false;
3. run the existing preload;
4. increment completed count after each individual frame attempt finishes, whether successful or failed;
5. retain successful URLs exactly as today;
6. mark preload complete when all attempts finish.

The existing `selectPlayableSolarFrames` result remains the authority for which frames can play.

## Interfaces and Implementation Boundary

Keep the existing Home screen and repository interfaces unchanged.

Extend the private `preloadSolarFrames()` helper with a small progress callback conceptually equivalent to:

`onProgress(completedFrames, totalFrames)`

The callback fires after each frame attempt completes.

The implementation should preserve the existing concurrent preload approach and Coil `SingletonImageLoader` / `ImageRequest` path. Progress reporting must not introduce a separate image transfer, change cache behavior, or serialize the preload.

The `SolarHero` composable owns the transient completed count and renders the progress text.

No reusable abstraction should be extracted solely to share this implementation with ENLIL unless an existing abstraction already fits directly. The user-facing behavior should match ENLIL, but YAGNI applies to implementation sharing.

## UI Behavior

While a non-empty hero frame set is preloading, keep the existing centered loading column:

- existing circular spinner
- `Loading frame X of Y`

Examples:

- `Loading frame 0 of 48`
- `Loading frame 18 of 48`
- `Loading frame 48 of 48` may appear briefly before the ready state takes over

Once preload completes, the loading UI disappears and the existing hero rendering/playback behavior takes over.

The existing cached-imagery failure message remains unchanged.

## Error Handling and Edge Cases

### Failed frame

A failed preload attempt increments the completed count because that frame's work has finished. It is not added to `preloadedUrls`. Existing playable-frame filtering remains unchanged.

### Frame-set change

Reset progress immediately to zero for the new frame count. Compose's existing `LaunchedEffect(frames)` cancellation/restart boundary should prevent the old preload from owning the new frame set's UI state.

### Cancellation

Progress is transient. When the preload coroutine is cancelled because the frame set changes or the composable leaves composition, stale progress must not be retained for a subsequent preload.

### Empty frames

Do not display `Loading frame 0 of 0`. Preserve the existing empty/unavailable behavior.

### Single frame

If one frame is supplied, it may report `Loading frame 0 of 1` and then `1 of 1` while preloading. Existing non-animation behavior for a single playable frame remains unchanged.

### Partial preload success

Preserve current behavior: successful frames remain eligible through `selectPlayableSolarFrames`; progress reporting does not change acceptance/playback rules.

## Testing Strategy

Follow strict TDD.

Add focused coverage around the Home hero preload/progress boundary.

Tests should verify:

- progress begins at zero for a non-empty frame set;
- each successful frame attempt advances completed count exactly once;
- each failed frame attempt also advances completed count exactly once;
- total count matches the selected frame set;
- completion order does not change successful-frame selection/order semantics;
- a new frame set resets progress;
- empty frames do not produce `0 of 0`;
- loading text follows the exact `Loading frame X of Y` wording;
- progress UI disappears when preload completes;
- the existing spinner remains present while loading;
- existing playback, blending, playable-frame selection, and interaction contracts remain green.

Run focused Home hero tests first, then the normal Android CI-equivalent verification. After the implementation push, actively monitor Android CI until green or user input is required.

Physical Pixel verification should confirm that the Home hero visibly advances through frame counts during an uncached preload and transitions cleanly into the existing animation without changing its playback or interaction behavior.

## Open Questions

None.
