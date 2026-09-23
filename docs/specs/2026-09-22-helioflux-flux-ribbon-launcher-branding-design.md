# HelioFlux Flux Ribbon Launcher Branding Design

**Date:** 2026-09-22  
**Status:** Approved

## Goal

Replace the current Earth/Aurora launcher branding with a simpler, distinctive HelioFlux mark that remains recognizable at small Android launcher sizes and works cleanly with Android adaptive and themed icons.

## Approved visual direction

The approved concept is **5A — Clean Flow**.

The primary mark is an abstract flowing ribbon symbol rather than a literal Sun, Earth, or aurora illustration. It uses three visually interlocking flowing forms around negative space to suggest solar wind, magnetic flux, auroral motion, and energy flow without depicting any one of those literally.

### Color treatment

- Pure black background.
- Upper ribbon transitions through green/teal into cyan.
- Lower/left flow emphasizes cyan and electric blue.
- Right-hand flow transitions through blue into violet.
- Controlled luminous edge/glow is acceptable, but the mark must remain crisp and readable at launcher-icon sizes.
- No text, stars, planets, Sun, border, or baked-in enclosing circle.

### Shape and composition

- Preserve the balanced 5A Clean Flow silhouette approved during brainstorming.
- Keep the complete mark comfortably inside Android's adaptive-icon safe region.
- The artwork itself must not assume a circular launcher mask; Android supplies the device/launcher mask.
- The black background extends through the full adaptive-icon background layer.
- Avoid unnecessary detail that disappears at small sizes.

## Android adaptive icon

Use the Clean Flow artwork as the foreground of the adaptive launcher icon and pure black as the adaptive background.

The same foreground must remain visually balanced under common launcher masks, including circle, squircle, and rounded-square shapes.

Legacy launcher support should continue only to the extent required by the app's existing minimum SDK and current resource strategy.

## Themed / monochrome icon

Provide a dedicated monochrome foreground for Android themed icons.

The monochrome asset uses the same recognizable Clean Flow silhouette rather than converting the colored artwork to grayscale. It should be a simple single-color mask so Android can apply the user's system theme.

## System splash

Keep Android's normal platform splash screen. Do not add a custom full-screen branded splash or startup delay.

The system splash background remains **pure black (#000000)** and displays the launcher mark centered using Android's normal splash behavior.

## Scope

This task changes launcher branding only:

- Replace the current Earth/Aurora foreground with Clean Flow.
- Replace the current Earth/Aurora monochrome foreground with the Clean Flow silhouette.
- Preserve adaptive and round launcher icon wiring.
- Preserve the pure-black Android system splash.
- Update focused regression coverage where necessary.

Out of scope:

- Home-screen UI changes.
- PWA changes.
- Custom splash screens or splash animations.
- Additional branding screens.
- Changes to app startup behavior.
- General theme or color-system changes.

## Verification

Implementation must follow TDD. Focused tests should protect:

1. Manifest launcher icon and round-icon wiring.
2. Adaptive icon resources.
3. Android 13+ monochrome/themed icon resource.
4. Pure-black Android 12+ system splash configuration.

After local/focused verification, run the repository's normal test/build checks and monitor the pushed GitHub Actions run to completion.

## Approved reference

The final visual reference is the generated **5A / Clean Flow** standalone artwork approved in chat on 2026-09-22: three flowing luminous ribbon forms on black, using green/teal, cyan/electric blue, and blue/violet transitions.
