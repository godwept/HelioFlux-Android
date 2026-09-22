# Solar Activity Event Card Color Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal and Success Criteria

Differentiate the Recent Flares and Recent CMEs cards visually while preserving the existing HelioFlux dark-space visual language.

Approved direction: **subtle tint**.

### Recent Flares

- Use a very dark warm/orange-tinted card surface.
- Use a warm orange/amber border.
- Use a matching warm title accent.
- Keep the surface tint restrained enough that the card still reads as a dark HelioFlux panel.

### Recent CMEs

- Use a very dark blue/cyan-tinted card surface.
- Use a cyan/blue border.
- Use a matching cool title accent.
- Keep the surface tint restrained enough that the card still reads as a dark HelioFlux panel.

Success means:

- the two cards are immediately distinguishable at a glance;
- both still feel visually related to the rest of HelioFlux;
- event text remains easy to read;
- existing flare-class colors remain clear;
- existing CME speed-emphasis colors remain clear;
- dividers remain visible;
- the CME Details action remains readable and functional;
- no layout or behavioral regression is introduced.

## Scope

### In scope

- Change only the visual treatment of the existing Recent Flares and Recent CMEs cards.
- Give Recent Flares a subtle warm-tinted dark surface.
- Give Recent Flares a warm orange/amber border.
- Give Recent Flares a matching warm title accent.
- Give Recent CMEs a subtle cool-tinted dark surface.
- Give Recent CMEs a cyan/blue border.
- Give Recent CMEs a matching cool title accent.
- Preserve the current full-width stacked layout.
- Preserve current spacing and padding.
- Preserve current typography.
- Preserve existing flare/CME row presentation.
- Preserve existing divider behavior.
- Preserve existing flare class emphasis.
- Preserve existing CME speed emphasis.
- Preserve the working CME Details external-link action.

### Out of scope

- No changes to flare or CME data sources.
- No changes to repository state handling.
- No changes to event ordering.
- No changes to list contents.
- No new icons.
- No gradients.
- No new shadows or glow effects.
- No animations.
- No redesign of the CME Details button.
- No changes to other Solar Activity cards.
- No PWA changes.
- Do not add new global theme colors unless the existing HelioFlux palette is insufficient for the approved treatment.

## Data and State

No data or state changes are required.

The cards continue receiving the existing:

- `RepositoryState<List<FlareEvent>>`
- `RepositoryState<List<CmeEvent>>`
- `onCmeDetails` callback

Loading, empty, cached, failure, and populated states remain unchanged.

This is a presentation-only change.

## Interfaces and Composition Boundary

Keep the change at the existing Solar Activity event-card composition boundary in:

`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

The existing `SolarEventCards(...)` function remains responsible for the two card containers.

No public API changes are required.

Prefer the existing theme palette:

- `SpaceSurface`
- `SpaceSurfaceVariant`
- `SolarOrange`
- `WarningAmber`
- `DataCyan`
- `DataBlue`

The implementation may derive low-alpha tinted surfaces from existing colors rather than introducing new named theme colors.

`FlareList.kt` and `CmeList.kt` should not change unless a focused contrast issue is discovered during implementation. Their existing semantic row colors are intentionally preserved.

## Error Handling and Edge Cases

The color treatment must not alter state behavior.

Requirements:

- Loading text remains readable on both tinted surfaces.
- Empty-state text remains readable.
- Cached/failure messages remain readable.
- Flare A/B/C/M/X class colors must retain sufficient visual separation from the warm flare card.
- CME Normal/Elevated/Strong speed emphasis colors must remain distinguishable from the cool CME card.
- Horizontal dividers must remain visible.
- The CME Details button must retain sufficient contrast and its existing external-link behavior.
- Compact and expanded layouts must use the same card treatment.
- Do not encode severity into the overall card background; the card color identifies event type only.

## Testing Strategy

Add focused presentation coverage that verifies the two cards intentionally use different visual tokens.

Preferred test shape:

- source-level or small presentation-policy coverage for:
  - distinct flare and CME card container treatments;
  - warm flare accent;
  - cool CME accent;
  - no return to a single shared `SpaceSurface` + `SolarOrange` treatment for both cards.

Preserve existing coverage for:

- stacked full-width card layout;
- flare rows;
- CME rows;
- loading/empty/failure states;
- CME Details action;
- app-level CME Details URI wiring;
- compact/expanded shared composition.

Run:

- focused Solar Activity unit tests;
- instrumentation-test compilation;
- connected tests when a device is available;
- normal Android CI.

Physical Pixel verification should confirm:

- Flares read as a subtle warm panel;
- CMEs read as a subtle cool panel;
- neither card looks saturated or disconnected from the black/dark HelioFlux background;
- row-level semantic colors remain clear;
- Details remains visible and works.

## Open Questions

None.
