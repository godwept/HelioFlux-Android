# Chart Inspection Tooltip Placement Design

**Date:** 2026-09-22  
**Repository:** `godwept/HelioFlux-Android`

## Context

Shared HelioFlux charts use Vico 3.1.0 through `HelioFluxLineChart.kt`. Pressing and dragging across a chart displays a marker containing the selected timestamp and series values.

The marker currently uses:

```kotlin
labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint
```

`AroundPoint` was introduced to avoid the permanently reserved blank band produced by Vico's default top marker placement. On a physical Pixel, however, the transient label often appears directly beneath the user's finger and is difficult or impossible to read.

The approved direction is to keep the label visually associated with the selected point while deliberately offsetting it away from the finger.

## Goal and Success Criteria

During press-and-drag chart inspection:

- Keep the tooltip visually associated with the selected point.
- Prefer placing it above the selected point.
- Target a fixed 48 dp clearance, within the approved approximately 40–60 dp range.
- If there is insufficient room above, place the tooltip below the selected point.
- Keep the complete tooltip within the available chart bounds wherever geometrically possible.
- Do not reserve permanent marker space above the chart.
- Preserve the existing marker contents, timestamp and value formatting, semantic colors, guideline, press/drag interaction, chart domains, axes, gridlines, reference lines, and disabled chart zoom/pan.
- Apply the behavior through the shared renderer so Space Weather and Solar Activity charts behave consistently.

## Scope

The change is confined to the shared chart presentation layer, primarily:

- `app/src/main/java/ca/stewark/helioflux/ui/components/HelioFluxLineChart.kt`
- focused shared chart presentation tests

No chart consumer should require changes.

Out of scope:

- changing chart data or reduction
- changing timeframe/domain behavior
- changing marker contents or persistence
- changing press/drag semantics
- enabling chart zoom or pan
- changing axes, gridlines, series styling, or reference lines
- modifying the PWA
- introducing configuration that has no current use case

## Data and State

No new Compose interaction state is required.

Tooltip placement should be derived during marker drawing from the geometry Vico already supplies: the selected marker target, label dimensions, and available chart/layer bounds. This keeps placement transient and deterministic and avoids adding another interaction state machine.

## Interfaces and API

Keep the public `HelioFluxLineChart` API unchanged.

Use the smallest internal Vico extension necessary to control label placement while retaining the existing `DefaultCartesianMarker` behavior for marker content, targets, guideline, and interaction.

Introduce one internal 48 dp clearance value rather than exposing a new chart parameter. A configurable public clearance is YAGNI until another use case requires it.

## Placement Behavior

1. Determine the selected point used by the existing marker.
2. Prefer the tooltip above that point with 48 dp separation.
3. If the full tooltip cannot fit above with the preferred separation, flip it below the point.
4. Constrain the label horizontally so it remains within the available chart bounds.
5. Constrain it vertically so the label remains readable within the available chart bounds.
6. When the geometry cannot satisfy both full readability and the exact 48 dp separation, prioritize keeping the label fully visible over maintaining the exact clearance.
7. Preserve the marker's visual relationship with the selected point and existing guideline.

The implementation must not switch back to Vico's permanently top-reserved marker placement.

## Edge Cases

### Point near the top

Flip the tooltip below the selected point instead of squeezing it against the top edge or reserving space above the chart.

### Point near the bottom

When below placement would exceed the bottom bound, constrain placement within the available area. Readability takes priority over exact clearance.

### Point near left or right edge

Retain association with the selected X coordinate while shifting/clamping the label horizontally so it remains on-screen.

### Tall multi-series tooltip

If neither side provides enough room for both the entire tooltip and the preferred 48 dp separation, use the placement that keeps the tooltip readable within the available geometry, even if the separation must be reduced.

### Sparse or unusual marker targets

Placement logic must fail safely and must not interfere with chart rendering or alter the existing marker value selection behavior.

## Error Handling

Tooltip positioning is presentation-only and should not introduce user-visible errors.

Unexpected or constrained geometry should degrade to an in-bounds readable placement rather than throw, hide chart data, or modify inspection state.

Existing handling for missing/sparse series values remains unchanged.

## Testing Strategy

Use strict TDD.

Extend the focused shared chart presentation coverage rather than adding a broad UI-test suite.

First add failing tests around a small deterministic placement calculation covering:

- normal placement above the point with the 48 dp target clearance
- top-edge pressure causing placement below
- bottom-edge pressure remaining bounded
- left-edge horizontal containment
- right-edge horizontal containment
- unusually tall labels preferring readable in-bounds placement when exact clearance is impossible

Update the existing marker wiring/contract coverage to verify that the shared renderer uses the custom placement behavior while preserving:

- marker formatter
- marker line count
- guideline
- existing press/drag marker controller behavior

After automated tests pass, physically verify on a Pixel that a finger no longer obscures the tooltip during press-and-drag inspection and that Space Weather and Solar Activity charts behave consistently.

## Implementation Constraints

- Strict TDD.
- Minimal focused changes.
- YAGNI and DRY.
- Prefer one implementation batch because this is one shared chart subsystem.
- Do not modify unrelated files.
- After pushing implementation, actively monitor GitHub Actions until CI is green or user input is required.
