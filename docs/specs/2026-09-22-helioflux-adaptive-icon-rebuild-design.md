# HelioFlux Adaptive Icon Rebuild Design

**Date:** 2026-09-22
**Status:** Approved

## Goal

Rebuild the HelioFlux Android launcher artwork from the supplied `HelioFlux_CleanFlow_Color.png` mockup, using that image itself as the authoritative visual reference. Preserve the mockup's glossy color treatment for the normal launcher icon while providing a purpose-built monochrome vector whose internal negative spaces remain recognizable when Android applies a themed-icon tint.

## Success Criteria

- [ ] The normal launcher icon closely matches the supplied Clean Flow PNG rather than a prose-based or hand-drawn approximation.
- [ ] The foreground remains balanced and unclipped under circle, squircle, and rounded-square adaptive-icon masks.
- [ ] The Android 13+ themed icon reads as the same three-flow mark and retains visible internal separation instead of becoming a solid blob.
- [ ] API 24–25 legacy launcher icons remain usable.
- [ ] The adaptive-icon background and Android system splash remain pure black.
- [ ] Direct visual comparison with the original PNG is part of implementation and acceptance.

## Scope

**In scope:**

- Use the supplied transparent PNG as the source for the full-color launcher foreground.
- Position and, only if mask previews require it, uniformly scale the artwork within Android's adaptive-icon safe region.
- Create a new, simplified monochrome vector by tracing the visible form and negative spaces in the mockup itself.
- Preserve legacy, adaptive, round, themed, and system-splash resource support.
- Add focused automated resource checks and perform visual mask/launcher-size verification.

**Out of scope:**

- Recreating the glossy full-color artwork as vector paths.
- Reusing the existing hand-authored color or monochrome path geometry as a design source.
- Changing application UI, general theme, startup behavior, splash timing, or adding animation.
- Adding branding outside the Android launcher and platform splash resources.

## Design

### Authoritative visual reference

The authoritative source is the user-supplied `HelioFlux_CleanFlow_Color.png`, not the earlier icon XML or a textual description. Before producing or judging replacement artwork, the implementer must open and inspect the source image directly at full resolution.

The inspected source is a 432×432 ARGB PNG with a transparent canvas. Its non-transparent bounds are approximately x=69–362 and y=66–365. The mark has a pointed green/teal upper curl, a broad cyan flow sweeping across the middle and lower-left, and a blue/violet right-hand flow curling down to a point. Dark and transparent valleys between those flows provide the depth and recognizable internal structure. Those observed relationships, including the crossing order and negative spaces, govern visual acceptance.

The implementation workflow must produce side-by-side previews of the source and built icon at both full size and launcher size. It must also preview the built icon under at least circle, squircle, and rounded-square masks. Documentation alone is not sufficient evidence of fidelity.

### Full-color foreground

Use the supplied PNG as a density-qualified bitmap foreground. Its transparent 432×432 canvas naturally maps to Android's 108 dp adaptive foreground canvas when treated as an xxxhdpi asset. Android may downsample it for lower-density displays.

Keep pure black as a separate adaptive background resource; do not flatten the entire foreground onto black. The small amount of dark shading that belongs to the mark may remain because it creates its depth against the black background.

Start with the source's existing centered composition. Apply only uniform scaling and centering if actual mask previews show clipping or imbalance. Do not crop, stretch, redraw, recolor, sharpen, or otherwise reinterpret the supplied artwork merely to satisfy numerical bounds.

### Monochrome themed foreground

Create the monochrome asset independently as a single-color vector mask. Trace it while viewing the original PNG directly; do not derive it from the discarded vector XML and do not merely convert the color image to grayscale or fill its outer silhouette.

The mask should simplify the three flowing forms enough to remain legible at launcher size. Transparent channels must follow the prominent dark valleys in the source so that the upper curl, middle/lower-left sweep, and right/downward curl remain visually distinguishable after Android supplies a single theme color. Where the color artwork relies on overlap and shading, use explicit transparent cutouts or separated vector regions to express that structure.

The mask must use only one opaque foreground color plus transparency. Gradients, semitransparent shading, background-colored paths, and overlapping opaque regions that close the important channels are not part of the monochrome design.

### Android resource integration

Retain the manifest references to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`. Retain black as the adaptive background and preserve the existing API 26 adaptive and API 33 monochrome resource roles.

The full-color resource changes from the discarded vector approximation to the supplied bitmap foreground. The monochrome resource remains a separately referenced vector. The pre-26 fallback may keep the existing layered resource structure if verification confirms that it renders the bitmap at the intended scale; otherwise, it should receive the smallest resource-only adjustment needed for a correct legacy icon.

The Android 12+ platform splash continues to use the launcher icon on `#000000`. No custom splash screen or startup code is introduced.

### Edge cases and failure handling

- If a common adaptive mask clips a visually important point, uniformly reduce and recenter the color foreground; never distort an individual lobe.
- If downsampling closes a monochrome channel, widen that transparent channel in the vector rather than adding shading or extra colors.
- If the color asset appears haloed on black, distinguish intentional luminous edge pixels from unwanted matte contamination by comparing directly with the source at 100% and launcher scale.
- Resource-build, density-selection, or pre-26 scaling failures are treated as implementation defects, not reasons to replace the mockup with an approximate vector.
- A structurally valid resource is not accepted when direct visual comparison shows the wrong silhouette, crossing order, scale, or balance.

## Testing Strategy

Automated regression coverage will verify:

- manifest launcher and round-icon references;
- API 26 adaptive foreground/background wiring;
- API 33 monochrome wiring;
- pure-black adaptive and system-splash backgrounds;
- expected PNG dimensions, alpha support, and substantial transparent canvas;
- a single-color monochrome vector with explicit negative-space structure;
- absence of the discarded color-vector approximation from resource selection.

Visual verification will render the built color and monochrome resources at launcher size and compare them directly with the opened source image. The color resource will be checked under circle, squircle, and rounded-square masks. The monochrome render will be checked at small size with contrasting themed colors to confirm that its internal channels remain open and the result is not a featureless blob.

Normal project unit tests, lint, and debug builds must also pass. Physical-device launcher and themed-icon inspection remains the final aesthetic acceptance check because launcher masking and themed presentation vary by device.

## Open Questions

None. Minor uniform scale adjustments are intentionally deferred to implementation-time mask previews and direct visual comparison.
