# Home Solar Background Zoom Design

**Date:** 2026-09-24
**Status:** Approved

## Goal

Let people customize the home screen by zooming and positioning the animated Sun as a fixed background behind the home content. The chosen view survives a fresh app launch. Resetting zoom restores the existing square hero layout.

## Success Criteria

- [ ] Pinching the square solar hero past a small threshold shows the animation across the home content area behind the HELIOFLUX title, condition badges, and NOAA Forecast cards.
- [ ] The background remains fixed while home content scrolls, and the title has no background panel.
- [ ] Pinch and drag on exposed animation adjust its framing without disrupting card taps, badge taps, or scrolling.
- [ ] Zoom and position return after a fresh app launch and remain usable after a screen size change.
- [ ] Returning to 1× zoom or double tapping exposed animation restores the square hero and clears its position.
- [ ] Loading and cached imagery keep the home content usable.

## Scope

**In scope:**

- Compact and expanded home layouts.
- A switch between the existing square hero at normal zoom and a fixed, full home content background while zoomed.
- Persistent zoom and position for the solar animation.
- Existing pinch, drag, and double tap controls on the animation.

**Out of scope:**

- Changes to other destinations, system bars, or bottom navigation and navigation rail backgrounds.
- A separate customization or settings screen.
- Translucent forecast cards or changes to their content and actions.

## Design

### Presentation

At 1× zoom, the home screen retains its square solar hero and current content arrangement. After zoom crosses a small threshold, the square leaves the content layout and the same animation fills the home content area behind the foreground. On compact screens, the condition badges and forecast cards move into the space formerly occupied by the square. On expanded screens, the foreground retains its current masthead and right-side content arrangement. The background remains fixed while that content scrolls.

The HELIOFLUX lettering and glow sit directly over the animation with no rectangular title surface. The letters retain their current orange appearance. Forecast cards keep their dark surfaces for readable text; the animation is visible around them.

### Saved View State

The saved view consists of a zoom level in the current 1×–4× range and a position relative to the available viewport. Persist it after a gesture settles so continuous pinch and drag remain smooth. A saved zoom above 1× restores background mode on a fresh launch. Returning to 1× clears the position and restores the square hero. When the viewport changes size, adapt and constrain the saved position so the animation stays visible.

### Interaction

Pinch and drag operate only on exposed animation. Foreground controls retain their existing taps and scrolling. Double tapping exposed animation resets zoom and position. A small transition threshold around 1× prevents repeated switches between layouts during a pinch. The layout switch should feel smooth without running a second independent solar animation.

### Loading and Edge Cases

If background mode is restored before imagery loads, show the foreground over a dark background, then display the animation in its saved position when frames are ready. Preserve the existing cached imagery and loading behavior. A missing frame sequence does not block the title, conditions, or forecast cards.

## Testing Strategy

- Automated state tests for entering background mode, returning to square mode, clearing the position, restoring a saved view after a fresh launch, and adapting it to a changed viewport.
- Home screen UI tests for title and card presence, card and badge actions, scrolling, and the loading state in both layouts.
- Visual checks on compact and expanded screens for transition quality, framing, readability, and gesture feel.

## Open Questions

None.
