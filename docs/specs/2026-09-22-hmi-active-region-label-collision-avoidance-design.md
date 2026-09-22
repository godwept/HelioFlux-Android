# HMI Active-Region Label Collision Avoidance Design

**Date:** 2026-09-22
**Status:** Approved

## Goal

Improve readability of NOAA active-region labels over the HMI magnetogram when multiple regions are close together, while preserving each region's true sunspot coordinate as the visual anchor. Labels may move only enough to avoid crowding, and labels that move beyond a small threshold should receive a subtle leader line back to their exact anchor.

## Success Criteria

- [ ] Nearby HMI active-region labels maintain a visible minimum separation instead of overlapping or crowding.
- [ ] Each label remains close to its associated sunspot/active-region anchor.
- [ ] The true helioprojective anchor position never changes.
- [ ] Labels that move more than a small threshold show a subtle leader line back to the anchor.
- [ ] Label placement is deterministic and does not visibly jump between recompositions.
- [ ] Labels remain inside the visible magnetogram stage.
- [ ] The same collision-avoidance behavior works in both the normal HMI card and fullscreen viewer.
- [ ] Existing HMIBC imagery, HEK acquisition, region numbering, zoom/pan, and other Solar Activity behavior remain unchanged.

## Scope

**In scope:**
- Screen-space collision avoidance for active-region text labels.
- A small minimum gap between rendered label bounds.
- Nearby candidate positions around each anchor.
- A cap on how far labels may move.
- Subtle leader lines when displacement exceeds the approved threshold.
- Reusing the shared `ActiveRegionOverlay` so normal and fullscreen HMI views behave consistently.
- Focused unit/UI coverage for collision resolution and leader-line decisions.

**Out of scope:**
- Changing HEK queries or active-region data.
- Changing helioprojective coordinate mapping.
- Moving or altering the actual sunspot/active-region anchor.
- Redesigning the magnetogram image source or LaTiS/ISWA integration.
- General graph-layout or physics-based label simulation.
- Animated label movement.
- Changing LASCO, ENLIL, DONKI, Helioviewer, or other data sources.
- Broad Solar Activity UI redesign.

## Design

### Anchor model

The existing `mapActiveRegion()` result remains the source of truth for each region's anchor. The true projected active-region coordinate is never modified.

Label placement is derived from the anchor in screen space after the overlay knows its rendered width and height.

### Minimum spacing

Treat each active-region label as a screen-space rectangle. Resolved label rectangles should maintain approximately **10 dp** of minimum separation.

The spacing value should be centralized with the collision helper rather than duplicated between normal and fullscreen rendering.

### Candidate placement

Start with the label at or immediately adjacent to its true anchor. If that position conflicts with an already placed label, test nearby candidate positions in a deterministic order:

1. above
2. below
3. left
4. right
5. upper-left
6. upper-right
7. lower-left
8. lower-right

If necessary, repeat at progressively larger offsets until a valid candidate is found or the maximum displacement is reached.

Choose the nearest valid candidate to the anchor. Candidate generation and selection must be deterministic for a stable region list and overlay size.

### Maximum displacement

A label may move at most approximately **48 dp** from its anchor.

If all candidates within that radius still conflict, use the least-overlapping candidate within the cap rather than allowing the label to drift farther away from its sunspot.

### Leader lines

If the resolved label position is displaced by more than approximately **16 dp** from the true anchor, draw a thin, low-emphasis leader line from the anchor toward the label.

The line should:
- originate at the exact active-region anchor;
- terminate at the nearest sensible edge/point of the label bounds rather than through the text when practical;
- use a subtle light gray/white treatment so it clarifies association without competing with the label text or magnetogram.

Labels displaced less than or equal to the threshold do not need a leader line.

### Bounds handling

Candidate positions must keep the entire label rectangle inside the visible overlay/stage bounds.

When an anchor lies near an edge, prefer candidate positions that move inward rather than clipping the label.

### Placement order

Resolve labels in a deterministic order independent of Compose iteration instability. Use a stable ordering derived from the region data (for example active-region number/id) so the same set of regions at the same viewport size produces the same layout.

No randomization or continuously iterating layout solver is introduced.

### Rendering architecture

Keep `ActiveRegionOverlay` as the shared rendering component for the normal HMI card and fullscreen HMI viewer.

Introduce a small pure layout helper that accepts:
- active-region anchors;
- measured/estimated label sizes;
- overlay dimensions;
- density-derived spacing/displacement thresholds;

and returns resolved label placements plus whether a leader line is required.

The Compose layer should remain responsible only for measurement and rendering. Collision-resolution math should remain pure/testable.

Do not introduce a physics engine, animation loop, or general-purpose graph-layout abstraction.

### Zoom and fullscreen behavior

The normal card and fullscreen viewer continue to reuse `ActiveRegionOverlay`.

In fullscreen, the overlay remains inside the same transformed stage as the magnetogram, so existing zoom/pan behavior remains intact. Collision placement is computed for the overlay's local stage coordinates, not global window coordinates.

## Error Handling and Edge Cases

- If there is only one active region, place it near its anchor with no unnecessary displacement or leader line.
- If labels do not collide, preserve their near-anchor placement.
- If several regions form a tight cluster, spread labels only within the maximum displacement cap and add leader lines as needed.
- If the overlay size is zero during initial composition, defer collision placement until a non-zero size is available.
- If a label cannot be fully separated within the cap, prefer the lowest-overlap in-bounds candidate rather than hiding the label.
- No label is dropped solely because of crowding.
- Existing region filtering from HEK remains unchanged.

## Testing Strategy

Use strict TDD.

1. Add pure layout tests for:
   - two non-overlapping anchors retaining near-anchor placement;
   - two close labels meeting the minimum gap;
   - a tight cluster selecting deterministic alternate positions;
   - resolved labels remaining within stage bounds;
   - displacement staying within the maximum radius;
   - leader line enabled only past the displacement threshold.
2. Keep existing `mapActiveRegion()` tests unchanged to prove helioprojective mapping is not altered.
3. Add/update Compose coverage showing active-region labels still render in the HMI overlay with the collision-resolved positions.
4. Verify fullscreen continues to use the same overlay and retains zoom/pan behavior.
5. Run focused unit/UI tests, then the full Android verification gates and CI.
6. Physically verify on the Pixel with a current HMIBC frame containing closely spaced NOAA regions.

## Open Questions

None. Approved defaults are:
- minimum label gap: approximately 10 dp;
- maximum label displacement: approximately 48 dp;
- leader line threshold: approximately 16 dp;
- deterministic nearest-candidate placement;
- subtle leader line for displaced labels.
