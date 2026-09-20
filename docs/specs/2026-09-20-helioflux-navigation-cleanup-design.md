# HelioFlux Navigation Cleanup Design

**Date:** 2026-09-20
**Status:** Approved

## Goal
Refine HelioFlux's compact bottom navigation and expanded navigation rail so they feel intentionally integrated with the app's dark space-weather visual language instead of stock Material navigation with letter placeholders. Preserve the existing destinations, routing, screen state, and behavior.

## Success Criteria
- [ ] Home, Space Weather, and Solar Activity use recognizable icons instead of letter placeholders.
- [ ] Compact navigation remains full-width and attached to the bottom edge.
- [ ] The selected destination uses the HelioFlux warm-orange accent with a restrained glow.
- [ ] Compact navigation has a short orange top indicator that slides smoothly between destinations.
- [ ] Inactive destinations remain readable in a subdued cool gray.
- [ ] The expanded navigation rail uses the same iconography and visual language with a vertically moving selection indicator.
- [ ] Navigation touch targets, labels, selected semantics, and existing routing behavior remain intact.

## Scope
**In scope:**
- Bottom navigation visual treatment on compact layouts.
- Navigation rail visual treatment on expanded layouts.
- Icons for all three existing destinations.
- Selected/inactive colors and restrained glow.
- Animated selected indicator.
- Minor spacing and typography refinement needed for the navigation treatment.
- Focused UI regression coverage for navigation identity and behavior.

**Out of scope:**
- Adding, removing, or renaming destinations.
- Changing routing or destination state management.
- Changing screen content.
- Reworking Home solar hero loading, playback, zoom, or pan.
- Broad theme redesign.
- Decorative animation beyond the selected navigation indicator and subtle selection color/glow transition.

## Design

### Visual direction
Use a restrained HelioFlux sci-fi treatment rather than stock Material selection styling. The navigation surface is near-black and visually attached to the app. A subtle warm-orange top edge/separator may distinguish it from content without becoming a bright border.

Do not use Orbitron for navigation labels. Orbitron remains a branding treatment for the HELIOFLUX masthead; navigation labels should prioritize compact readability.

### Compact bottom navigation
The bar remains full-width and attached to the bottom edge.

Destinations:
- Home: house icon.
- Space Weather: globe icon with geographic/grid character where practical.
- Solar Activity: sun icon with rays.

Inactive icons and labels use a muted cool gray. The selected icon and label use the same warm-orange family established by the Home masthead.

Remove the stock Material selection pill as the primary selected-state treatment. Instead, place a short thin orange indicator at the top of the navigation bar, centered over the selected destination. The indicator has a restrained orange glow.

When selection changes, the indicator slides horizontally to the new destination over approximately 200–250 ms using a smooth easing curve. Icons should not bounce or perform large scaling animations. Their selected color/glow may transition subtly.

Keep Android-appropriate touch targets and visible labels.

### Expanded navigation rail
Use the same destination icons, active orange, inactive gray, label typography, and restrained glow.

Adapt the selected indicator to the vertical geometry: a short orange indicator moves vertically alongside the selected destination. Do not force a horizontal bottom-bar metaphor onto the rail.

### State and behavior
Continue using the existing selected destination state and selection callback. The navigation cleanup must not introduce a second navigation state machine. Indicator position is derived from the existing selected destination.

### Edge cases
- Initial destination must position the indicator correctly without requiring user interaction.
- Restored selected destination must render the matching icon/label/indicator state.
- Compact and expanded layouts must both expose all three destinations.
- Fast repeated tab changes must settle on the current selected destination without affecting routing.
- Navigation remains usable if decorative glow/animation is not perceptible.

## Testing Strategy
Add focused Compose UI regression coverage around the navigation surface:
- Assert the three destinations are present with stable semantics/test tags.
- Assert selecting each destination still changes the destination content.
- Assert compact and expanded navigation surfaces remain available at their expected window sizes.
- Expose only minimal stable test tags needed to verify the custom selected indicator is present and associated with the selected destination; do not test animation frame timing or pixel-perfect glow.
- Run the existing Android test/build suite and verify GitHub Actions before physical-device review.

Physical Pixel verification should confirm icon clarity, orange/gray balance, glow restraint, bar spacing, and smooth indicator movement.

## Open Questions
None.
