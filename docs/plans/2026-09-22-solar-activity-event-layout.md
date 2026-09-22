# Solar Activity Event Layout Implementation Plan

**Date:** 2026-09-22  
**Design doc:** `docs/specs/2026-09-22-solar-activity-event-layout-design.md`  
**Status:** Ready for review

## Overview

Reorder the Solar Activity science feed so the two chart sections stay together and the recent-event cards follow them. The final order will be **X-Ray Activity → Particle Environment → Recent Flares → Recent CMEs**. The flare and CME cards will remain visually unchanged internally, but they will be stacked vertically as separate full-width cards instead of sharing one horizontal row. No ViewModel, repository, database, refresh, chart-engine, or event-list behavior will change.

## Baseline

Current `main`:

`f299895b0d2d15f192601ac9da5de7ffa664e7fa`

Latest code CI:

- Android CI #297
- Run ID `35773983665`
- GREEN

The design-doc commit did not trigger a new Android CI run.

## Tasks

### Task 1: Add source-level regressions for ordering and vertical event-card layout

**Files:**
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`

**Test first:**

Replace the existing paired-row assertion with focused structure/order coverage.

Add a test that locates the shared `solarActivityScienceItems` function and verifies that the Particle Environment section is emitted before `SolarEventCards`:

```kotlin
@Test
fun particleEnvironmentPrecedesRecentEvents() {
    val scienceStart = source.indexOf("private fun LazyListScope.solarActivityScienceItems(")
    val xray = source.indexOf("XrayActivitySection(", scienceStart)
    val particleHeading =
        source.indexOf("HelioFluxSectionHeading(\"Particle Environment\")", scienceStart)
    val particle = source.indexOf("ParticleSection(state.epam, chartDomain)", scienceStart)
    val events = source.indexOf("SolarEventCards(", scienceStart)

    assertTrue(scienceStart >= 0)
    assertTrue(xray in (scienceStart + 1) until particleHeading)
    assertTrue(particleHeading < particle)
    assertTrue(particle < events)
}
```

Replace `recentEventsUseOnePairedCardRow` with a test that verifies the event-card composition is vertical and full-width.

Use the `SolarEventCards` source slice so the test does not confuse unrelated `Column` or `Row` calls elsewhere in the screen:

```kotlin
@Test
fun recentEventsUseStackedFullWidthCards() {
    val eventsStart = source.indexOf("private fun SolarEventCards(")
    val eventsEnd = source.indexOf("\n@Composable\nprivate fun XraySection(", eventsStart)
    val eventsSource = source.substring(eventsStart, eventsEnd)

    assertTrue(eventsSource.contains("Column("))
    assertFalse(eventsSource.contains("Row("))
    assertTrue(eventsSource.contains("testTag(\"recent-flares-card\")"))
    assertTrue(eventsSource.contains("testTag(\"recent-cmes-card\")"))
    assertEquals(2, Regex("Modifier\\.fillMaxWidth\\(\\)\\.testTag").findAll(eventsSource).count())
}
```

Keep the existing nested-scroll protection test unchanged.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

Expected before implementation: the new ordering/stacking assertions fail because Particle Environment currently follows the event cards and `SolarEventCards` currently uses a horizontal `Row` with weighted cards.

---

### Task 2: Update instrumentation coverage for the new shared ordering and stacked cards

**Files:**
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Update the event-card tests so their names and assertions describe the approved layout rather than the old paired-row design.

For compact mode, rename:

`compactEventCardsKeepCompleteListsAndDetailsAction`

to:

`compactEventCardsStackAndKeepCompleteListsAndDetailsAction`

Keep the existing checks for:

- `recent-flares-card`;
- `recent-cmes-card`;
- both flare rows;
- both CME rows;
- the CME `Details` action.

Update the scroll target to the first event card instead of the old row tag:

```kotlin
compose.onNodeWithTag("solar-activity-compact")
    .performScrollToNode(hasTestTag("recent-flares-card"))
```

For expanded mode, rename:

`expandedSciencePaneUsesSamePairedEventCards`

to:

`expandedSciencePaneUsesSameStackedEventCards`

and scroll to `recent-flares-card`, then verify:

```kotlin
compose.onNodeWithTag("recent-flares-card").assertExists()
compose.onNodeWithTag("recent-cmes-card").assertExists()
compose.onNodeWithTag("solar-activity-expanded-imagery").assertExists()
```

Add one focused compact ordering test using the shared LazyColumn semantics. Scroll first to Particle Environment, verify it exists, then scroll farther to Recent Flares and verify it exists. This confirms both sections remain reachable in the approved sequence without introducing nested scrolling:

```kotlin
@Test
fun compactScienceFeedPlacesParticleEnvironmentBeforeRecentEvents() {
    compose.setContent {
        SolarActivityScreen(populatedEventState(), expanded = false)
    }

    compose.onNodeWithTag("solar-activity-compact")
        .performScrollToNode(hasText("Particle Environment"))
    compose.onNodeWithText("Particle Environment").assertExists()

    compose.onNodeWithTag("solar-activity-compact")
        .performScrollToNode(hasText("Recent Flares"))
    compose.onNodeWithText("Recent Flares").assertExists()
}
```

Do not add screenshot tests, fixed-height assertions, or geometry-specific pixel checks.

**Verify:**

Compile the instrumentation tests:

```bash
./gradlew :app:assembleDebugAndroidTest
```

If an Android device/emulator is available, run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected before production implementation: source-level Task 1 still provides the deterministic red test for structure; instrumentation changes should continue to compile against the current public composable API.

---

### Task 3: Reorder Particle Environment ahead of recent events

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Use the failing `particleEnvironmentPrecedesRecentEvents` test from Task 1.

**Implementation:**

In `solarActivityScienceItems(...)`, change only item order.

Current logical order:

```text
XrayActivitySection
SolarEventCards
Particle Environment heading
ParticleSection
```

Required order:

```text
XrayActivitySection
Particle Environment heading
ParticleSection
SolarEventCards
```

The resulting structure should remain four `LazyColumn` items:

```kotlin
item {
    XrayActivitySection(
        state = state.xray,
        probabilities = state.probabilities,
        chartDomain = chartDomain,
    )
}
item {
    HelioFluxSectionHeading("Particle Environment")
}
item {
    ParticleSection(state.epam, chartDomain)
}
item {
    SolarEventCards(
        flares = state.flares,
        cmes = state.cmes,
        onCmeDetails = onCmeDetails,
    )
}
```

Do not change:

- X-Ray section contents;
- Particle Environment heading styling;
- `ParticleSection`;
- chart height/styling;
- `AceEpamChart`;
- `animateInitial = false`;
- state or refresh behavior.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

The ordering regression passes.

---

### Task 4: Stack Recent Flares and Recent CMEs as full-width cards

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Use the failing `recentEventsUseStackedFullWidthCards` test from Task 1.

**Implementation:**

Inside `SolarEventCards(...)`:

1. Replace the outer `Row` with a `Column`.
2. Keep the existing `Modifier.fillMaxWidth()`.
3. Use `verticalArrangement = Arrangement.spacedBy(10.dp)`.
4. Remove `Modifier.weight(1f)` from both cards.
5. Give each card `Modifier.fillMaxWidth()` while preserving the existing test tags.
6. Leave card colors, border, padding, title typography, title colors, `FlareList`, `CmeList`, and `onCmeDetails` unchanged.

Target structure:

```kotlin
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("recent-flares-card"),
        // existing colors/border unchanged
    ) {
        // existing Recent Flares content unchanged
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("recent-cmes-card"),
        // existing colors/border unchanged
    ) {
        // existing Recent CMEs content unchanged
    }
}
```

The old `recent-events-row` tag is no longer needed because the container is no longer a row and tests can target the actual cards. Remove that tag rather than introducing a new public/test-only container contract.

Do not modify:

- `FlareList.kt`;
- `CmeList.kt`;
- event row rendering;
- CME Details behavior;
- empty/loading/failure presentation;
- event data ordering.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

All source-level layout/order regressions pass.

---

### Task 5: Run focused Solar Activity regression coverage

**Files:** No additional files expected.

**Test first:**

Run the focused JVM tests:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.AceEpamChartTest"
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.*"
```

Compile instrumentation coverage:

```bash
./gradlew :app:assembleDebugAndroidTest
```

If a device/emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Inspect failures before making any correction. Only fix issues caused by this layout/order change.

**Verify:**

Confirm:

- X-Ray Activity remains first in the science feed;
- Particle Environment is immediately after X-Ray Activity;
- Recent Flares follows Particle Environment;
- Recent CMEs follows Recent Flares;
- both cards are full-width;
- flare/CME row contents are unchanged;
- CME Details still works;
- no nested scroll container was added;
- `AceEpamChart.kt` still contains `animateInitial = false`;
- compact and expanded layouts still call the same `solarActivityScienceItems(...)`.

---

### Task 6: Run full verification, commit once, and monitor CI

**Files:** No additional files expected.

**Test first / full verification:**

Run the normal Android verification sequence:

```bash
./gradlew projects
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew lintDebug assembleDebug
```

If an Android device is available:

```bash
./gradlew connectedDebugAndroidTest
```

Inspect the final diff and confirm implementation changes are limited to:

- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

No changes should appear in ViewModels, repositories, database code, chart renderer code, `FlareList.kt`, `CmeList.kt`, or `AceEpamChart.kt`.

To minimize GitHub Actions usage:

1. perform test-first work on an isolated branch;
2. keep intermediate TDD commits there if useful;
3. squash to one focused implementation commit on `main`;
4. push once.

Suggested commit message:

`ui: stack solar activity event cards`

Monitor the triggered Android CI run during the implementation response. If CI fails, inspect the failing step/logs, make the smallest correction consistent with this plan, push it, and continue monitoring until green.

**Verify:**

Android CI completes successfully.

Then physically verify on the Pixel:

- visible order is **X-Ray Activity → Particle Environment → Recent Flares → Recent CMEs**;
- Recent Flares spans the available content width;
- Recent CMEs spans the available content width;
- both event lists remain easy to scan;
- CME Details still opens;
- scrolling remains smooth;
- Particle Environment does not replay its initial chart animation when scrolled off-screen and back.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Particle Environment is directly below X-Ray Activity.
- [ ] Recent Flares is below Particle Environment.
- [ ] Recent CMEs is below Recent Flares.
- [ ] Flare and CME cards are vertically stacked.
- [ ] Both event cards use the full available content width.
- [ ] Existing card styling is unchanged.
- [ ] Existing flare/CME list contents and states are unchanged.
- [ ] CME Details behavior is unchanged.
- [ ] No nested vertical scrolling is introduced.
- [ ] Particle Environment `animateInitial = false` behavior remains intact.
- [ ] Compact and expanded modes share the same science-item ordering.
- [ ] Focused Solar Activity tests pass.
- [ ] Full Android verification passes.
- [ ] No unrelated files are modified.
- [ ] Android CI is green.
- [ ] Pixel verification confirms the approved layout and smooth scrolling.
