# HelioFlux Aurora Globe Interaction Design

**Date:** 2026-09-20
**Status:** Approved

## Goal

Make the existing native Aurora globe feel continuously alive and directly manipulable without changing its NOAA OVATION visualization or the stabilized Filament resource lifecycle.

## Interaction contract

- While idle, the globe rotates continuously at the existing slow rate.
- A deliberate one-finger globe drag rotates longitude and latitude naturally.
- A two-finger pinch/spread changes camera distance within safe bounds.
- Automatic rotation pauses during direct manipulation and resumes after a short idle delay.
- A predominantly vertical one-finger swipe remains available to the surrounding Space Weather LazyColumn; the globe must not turn ordinary page scrolling into globe rotation.
- Once a gesture is claimed as globe manipulation, keep that gesture coherent until it ends rather than switching ownership mid-gesture.

## Gesture arbitration

Use a small presentation-only gesture-intent classifier at the globe boundary.

For a single pointer, do not claim the gesture immediately on touch-down. Wait until movement crosses touch slop. If horizontal movement is dominant, claim it for globe rotation. If vertical movement is dominant, leave it to the parent scroll container. Multi-touch is globe-owned so pinch zoom remains reliable.

Only claimed globe gestures update GlobeInteractionState or pause/resume auto-rotation. This prevents a normal vertical page swipe over the hero from rotating the Earth or delaying idle rotation.

Keep the existing SceneView/Filament rendering, materials, textures, Aurora layer, atmosphere layer, and lifecycle ownership unchanged. The change belongs at input arbitration/state handling, not resource management.

## State behavior

Retain GlobeInteractionState as the small deterministic state machine for yaw, pitch, camera distance, interaction state, and delayed auto-rotation. Keep pitch and zoom clamps. Do not introduce persistence, ViewModel state, inertia, fling physics, or a second rendering controller.

## PWA relationship

The PWA remains reference-only. It establishes the intended continuously rotating, draggable globe feel. Android additionally supports pinch zoom per the native requirement and explicitly arbitrates one-finger vertical swipes because the globe is embedded in a vertically scrolling screen.

## Testing

Use strict TDD.

- Preserve existing state tests for idle rotation, pause/resume, zoom clamps, and pitch clamps.
- Add focused unit tests for gesture classification: below-slop remains undecided, horizontal drag becomes globe-owned, vertical drag becomes parent-scroll-owned, and multi-touch becomes globe-owned.
- Keep existing composition recreation/lifecycle regression tests unchanged to guard the prior Filament texture/material crash.
- Avoid resource-lifecycle changes and brittle rendering assertions.

Physical Pixel validation after green CI must cover idle rotation, horizontal/diagonal drag, pinch zoom, vertical scrolling beginning over the globe, automatic rotation resuming, and navigating away/back without a crash.
