# HelioFlux Navigation Cleanup Implementation Plan

**Date:** 2026-09-20  
**Design doc:** docs/specs/2026-09-20-helioflux-navigation-cleanup-design.md  
**Status:** Ready for review

## Overview
Replace the current letter-placeholder Material navigation treatment with a focused HelioFlux navigation component for compact and expanded layouts. Reuse the existing destination state and callbacks, add stable destination icon metadata, apply the approved near-black/orange/gray visual language, and animate one selection indicator horizontally on the bottom bar and vertically on the rail. Routing and screen content remain unchanged.

## Tasks

### Task 1: Lock navigation identity and selected-indicator semantics
**Files:** `app/src/androidTest/java/ca/stewark/helioflux/ui/AdaptiveNavigationTest.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/MainNavigationTest.kt`

**Test first:**
- Extend compact navigation coverage to assert stable nodes for Home, Space Weather, and Solar Activity navigation items and a compact selected-indicator node.
- Extend expanded coverage to assert the same three destination nodes and a rail selected-indicator node.
- Keep the existing navigation behavior test and selected semantics assertions intact.
- Add a selection change assertion that verifies the indicator remains present and the newly clicked destination becomes selected. Do not assert intermediate animation frames or pixel positions.

**Implementation:**
- No production implementation in this task. Run the focused instrumentation tests to establish the intended regression contract and confirm they fail because the new tags/indicator do not yet exist.

**Verify:** Run the focused Android instrumentation tests for `AdaptiveNavigationTest` and `MainNavigationTest`; the new assertions should fail for the expected missing navigation item/indicator semantics while existing routing assertions remain valid.

---

### Task 2: Give destinations real navigation icons
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/navigation/HelioFluxDestination.kt`, `app/src/main/java/ca/stewark/helioflux/ui/HelioFluxApp.kt`

**Test first:**
- Use the failing stable destination-node assertions from Task 1 as the test-first contract.
- If icon semantics are exposed separately, assert by stable test tag rather than content description text so visible labels remain the accessibility name.

**Implementation:**
- Add the minimal icon metadata needed by the three existing enum entries: house for Home, globe/public-style icon for Space Weather, and sun/light-mode-style icon for Solar Activity.
- Use Material icon vectors already available to the app; do not add an icon library or asset pipeline unless compilation proves the required icons are unavailable.
- Replace the current `Text(d.label.take(1))` icon placeholders in both compact and expanded navigation.
- Give each navigation item a stable test tag derived from its route while retaining its visible label and selected semantics.

**Verify:** Run the focused instrumentation tests and the app compile task. The three navigation destinations should be identifiable and no letter-placeholder icon code should remain.

---

### Task 3: Build the compact HelioFlux bottom bar
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/HelioFluxApp.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/AdaptiveNavigationTest.kt`

**Test first:**
- Keep the compact selected-indicator assertion red until the custom bottom bar is present.
- Add only any additional stable semantic assertion required to distinguish the compact indicator from the rail indicator.

**Implementation:**
- Extract a focused compact navigation composable inside the existing UI/navigation area rather than changing routing architecture.
- Keep the bar full-width and attached to the bottom edge.
- Use a near-black container, muted cool-gray inactive content, and warm-orange selected icon/label content.
- Suppress the stock Material selection pill so selection is communicated by color/glow plus the custom indicator.
- Add a subtle top separator.
- Draw one short, thin orange indicator at the top of the bar and derive its target slot from the existing selected `HelioFluxDestination`.
- Animate its horizontal offset with a tween of approximately 200–250 ms and smooth easing.
- Keep minimum Android touch targets and visible labels. Do not use Orbitron for labels.
- Keep glow restrained and decorative; it must not affect semantics or hit targets.

**Verify:** Run `AdaptiveNavigationTest` and `MainNavigationTest`. Compact navigation should expose the indicator, retain all three labels/items, and still navigate correctly.

---

### Task 4: Apply the matching expanded navigation rail
**Files:** `app/src/main/java/ca/stewark/helioflux/ui/HelioFluxApp.kt`, `app/src/androidTest/java/ca/stewark/helioflux/ui/AdaptiveNavigationTest.kt`

**Test first:**
- Use the failing expanded rail-indicator assertion from Task 1.
- Confirm the test still requires the bottom navigation to be absent at expanded width.

**Implementation:**
- Apply the same house/globe/sun iconography, near-black surface, orange selected content, gray inactive content, readable labels, and restrained glow to the expanded rail.
- Remove the stock selection pill as the primary selection treatment.
- Add one short orange indicator alongside the rail items.
- Derive its target from the same existing selected destination and animate its vertical offset with the same approximately 200–250 ms smooth tween.
- Keep the existing 840dp adaptive breakpoint and destination selection callback unchanged.

**Verify:** Run `AdaptiveNavigationTest` and `MainNavigationTest`. Expanded layout should show only the rail, expose its selected indicator, and preserve navigation behavior.

---

### Task 5: Regression verification and physical-device handoff
**Files:** No production files unless a focused test/build failure requires a correction within the approved scope.

**Test first:**
- No new test is introduced here; this task validates the completed TDD work from Tasks 1–4.

**Implementation:**
- Run the repository's existing test/build checks used by CI.
- Review the final diff to confirm no routing, destination set, screen content, Home solar hero behavior, or unrelated files changed.
- Push the completed atomic navigation-cleanup batch.
- Monitor the resulting GitHub Actions run until it completes. If it fails, inspect the failing job/log, make the minimum focused correction, push, and continue monitoring until green.

**Verify:** GitHub Actions is green. Hand back for physical Pixel verification of icon clarity, label readability, orange/gray balance, glow restraint, attached bar spacing, horizontal indicator movement, and navigation behavior.

## Definition of Done
- [ ] All tasks completed in order
- [ ] All new navigation behavior/semantics have regression coverage
- [ ] All tests and CI pass
- [ ] No unplanned files modified
- [ ] Existing routing and destination state behavior are unchanged
- [ ] Compact navigation is full-width/attached with house, globe, and sun icons
- [ ] Compact selected indicator slides horizontally with restrained HelioFlux styling
- [ ] Expanded rail uses the matching styling and a vertical selected indicator
- [ ] Home solar hero behavior is untouched
