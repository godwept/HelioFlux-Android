# Aurora Globe Hero Refresh Design

**Date:** 2026-09-20
**Status:** Approved

## Goal
Make the Space Weather Aurora globe feel like it is floating in space, visually matching the Home solar hero treatment while preserving the now-working globe interaction behavior.

## Success Criteria
- [ ] Aurora hero uses a pure black stage with no Material card surface.
- [ ] Hero uses the full available content width in a 1:1 square.
- [ ] Globe is substantially larger on initial load and nearly fills the square while leaving enough room for the atmosphere glow.
- [ ] Existing 16 dp rounded outer clipping is retained.
- [ ] Freshness indicator remains layered above the globe and readable.
- [ ] Auto-rotation, continuous drag, pinch zoom, delayed rotation resume, Aurora rendering, and Filament lifecycle behavior remain unchanged.

## Scope
**In scope:**
- Replace the current fixed-height Material Card presentation with a full-width square black stage.
- Keep the existing 16 dp rounded clipping.
- Increase the initial globe framing by reducing the SceneView orbit radius.
- Preserve the freshness indicator overlay.

**Out of scope:**
- Changes to SceneView gesture handling.
- Changes to touch ownership or LazyColumn gesture cancellation handling.
- Changes to Aurora data, color mapping, atmosphere rendering, or Filament resource lifecycle.
- New controls, labels, animations, or effects.
- Changes to Space Weather charts or other screen sections.
- Changes to the PWA.

## Design

### Goal and presentation
The Aurora visualization should use the same visual language as the Home solar hero: a black, edge-to-edge stage where the rendered object reads as floating in space rather than sitting inside a dashboard card.

### Layout
The hero uses the full available content width and a 1:1 aspect ratio. The existing 16 dp rounded clipping remains. The current fixed `340.dp` height is removed.

### Globe framing
The initial SceneView camera orbit radius changes from `4.25f` to `3.0f`. This brings the Earth significantly closer so the globe and atmosphere occupy nearly all of the square hero while retaining a small visual margin around the atmosphere. Pinch zoom remains available after initial load.

### State and interfaces
No new app state, data model, or public API is introduced. Existing `AuroraGlobe`, Aurora points, freshness state, touch callback, camera manipulator, and auto-rotation state remain intact.

### Error handling and edge cases
Existing renderer/data fallback behavior remains unchanged. The black hero stage remains stable even if Aurora data is missing. The freshness badge continues to render over the globe.

## Testing Strategy
- Add a regression test asserting the Aurora hero uses a full-width 1:1 stage, explicit `Color.Black`, and 16 dp rounded clipping, and no longer uses the fixed `340.dp` height.
- Add a regression test asserting the initial camera distance is `3.0f`.
- Keep all existing globe-control regression tests unchanged and green.
- Run the full Android CI suite.
- Physically verify on a Pixel that the globe is substantially larger, the atmosphere is not visibly clipped, and drag/pinch/auto-rotation still behave exactly as before.

## Open Questions
None.
