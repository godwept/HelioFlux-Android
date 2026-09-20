# Metric Pill Text Centering Design

**Date:** 2026-09-20
**Status:** Approved

## Goal
Center the existing label/value text groups inside the metric pills on the Home and Space Weather screens without changing any other visual or behavioral characteristics.

## Scope

### In scope
- Home current-condition metric pills in `app/src/main/java/ca/stewark/helioflux/ui/home/CurrentConditions.kt`.
- Space Weather solar-wind metric pills in `app/src/main/java/ca/stewark/helioflux/ui/spaceweather/SolarWindMetrics.kt`.
- Center the complete existing `LABEL + VALUE` group horizontally within each pill.

### Out of scope
- Pill dimensions.
- Padding values.
- Label/value spacing.
- Typography.
- Colors, borders, shadows, or shapes.
- Data formatting.
- Click/navigation behavior.
- Any other Home or Space Weather layout changes.

## Design
Each pill keeps its current single-line `Row`, label/value order, spacing, and padding.

The internal `Row` will fill the available pill width and use centered horizontal arrangement, specifically:
- `Modifier.fillMaxWidth().padding(...)`
- `horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)`

This centers the label/value pair as one visual group while preserving the existing 5 dp spacing between the two texts.

## State and interfaces
No new state, models, or public interfaces are introduced.

## Testing
Add focused source-level regression coverage proving:
- Home pill content rows fill the available width and center the label/value group.
- Space Weather pill content rows fill the available width and center the label/value group.
- Existing pill tags, formatting, colors, and navigation tests remain unchanged and green.

Run the full Android CI workflow after implementation.

## Open Questions
None.
