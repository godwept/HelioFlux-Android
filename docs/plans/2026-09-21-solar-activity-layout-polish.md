# Solar Activity Layout Polish Implementation Plan

**Date:** 2026-09-21  
**Design doc:** `docs/specs/2026-09-21-solar-activity-layout-polish-design.md`  
**Status:** Ready for review

## Overview

Polish the refreshed Solar Activity screen in one focused UI batch. Add status-bar-safe top inset handling for the probability pills, group the X-Ray heading and existing chart into one tightly spaced block, and replace the separate full-width flare/CME sections with two equal-width side-by-side cards that preserve the complete event lists and existing state handling. Keep all data, ViewModel, repository, chart-engine, imagery, fullscreen, ENLIL, and refresh behavior unchanged.

The implementation should remain local to `SolarActivityScreen.kt`. Existing `FlareList` and `CmeList` stay responsible for state-aware event content and do not gain nested scrolling or outer-card responsibilities.

## Tasks

### Task 1: Lock the status-bar inset contract

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Extend `SolarActivityScreenSourceTest` with a source-level contract that requires top-only system inset handling on both scrollable Solar Activity content panes:

```kotlin
@Test
fun solarActivityKeepsTopContentBelowStatusBar() {
    val insetCall =
        "windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))"

    assertEquals(2, source.windowed(insetCall.length).count { it == insetCall })
}
```

Run the focused test and confirm it fails because the current compact and expanded lists do not apply status-bar insets.

**Implementation:**

In `SolarActivityScreen.kt`:

1. Keep the existing `PaddingValues(horizontal = 16.dp, vertical = 12.dp)` for compact content.
2. Add
   `windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))`
   to the compact `LazyColumn` modifier.
3. Add the same top-only inset padding to the expanded right-pane `LazyColumn` modifier.
4. Do not put window-inset logic inside `FlareProbabilityBadges`.
5. Do not add hardcoded status-bar heights.
6. Do not add the inset to lower sections or event cards.

This makes the probability strip begin below the physical status bar while retaining the existing 12dp content breathing room.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

The new inset-contract test and existing compact/expanded structure tests pass.

---

### Task 2: Lock the grouped X-Ray section contract

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Add a source-level test that requires one screen-owned X-Ray block instead of two independent lazy items:

```kotlin
@Test
fun xrayHeadingAndChartAreGroupedIntoOneSection() {
    assertTrue(source.contains("fun XrayActivitySection("))
    assertTrue(source.contains("HelioFluxSectionHeading(\"X-Ray Activity\", topSpacing = 0.dp)"))
    assertTrue(source.contains("XraySection(state, chartDomain)"))
}
```

Update the existing heading contract so `X-Ray Activity` is still present but the test does not require it to be a standalone lazy-list item.

Run the focused test and confirm it fails before implementation.

**Implementation:**

In `SolarActivityScreen.kt`:

1. Add a private composable:
   ```kotlin
   @Composable
   private fun XrayActivitySection(
       state: RepositoryState<List<XrayFluxSample>>,
       chartDomain: ChartDomain,
   )
   ```
2. Inside it, render:
   - `HelioFluxSectionHeading("X-Ray Activity", topSpacing = 0.dp)`
   - the existing `XraySection(state, chartDomain)` immediately below.
3. In `solarActivityScienceItems`, replace the current separate X-Ray heading item + chart item with one:
   ```kotlin
   item {
       XrayActivitySection(state.xray, chartDomain)
   }
   ```
4. Do not change `XraySection`, `XrayChart.kt`, `HelioFluxLineChart.kt`, chart height, chart domain, axes, references, reduction, or gestures.

Because `HelioFluxSectionHeading` already supplies 8dp bottom padding, composing the chart directly beneath it gives the approved roughly 8dp visual gap without negative margins or duplicated outer spacing.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

Confirm the grouped-section test passes and the existing 72-hour X-Ray tests remain untouched.

---

### Task 3: Lock the paired event-card structure

**Files:**  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Update `SolarActivityScreenSourceTest` so the screen no longer expects orange-rail section headings for Recent Flares / Recent CMEs and instead requires one paired event-card block:

```kotlin
@Test
fun recentEventsUseOnePairedCardRow() {
    assertTrue(source.contains("fun SolarEventCards("))
    assertTrue(source.contains("testTag(\"recent-events-row\")"))
    assertTrue(source.contains("testTag(\"recent-flares-card\")"))
    assertTrue(source.contains("testTag(\"recent-cmes-card\")"))
    assertFalse(source.contains("HelioFluxSectionHeading(\"Recent Flares\""))
    assertFalse(source.contains("HelioFluxSectionHeading(\"Recent CMEs\""))
}

@Test
fun eventCardsDoNotAddNestedScrolling() {
    assertEquals(2, Regex("LazyColumn\\(").findAll(source).count())
    assertFalse(source.contains("verticalScroll("))
}
```

Keep the existing `SolarActivityPhase10Test.eventListsUseRepositoryOrderAndScreenOwnedHeadings` assertions that `FlareList` and `CmeList` do not own the Recent headings and do not re-sort data.

Run both focused unit tests and confirm the new structural test fails.

**Implementation:**

In `SolarActivityScreen.kt` add:

```kotlin
@Composable
private fun SolarEventCards(
    flares: RepositoryState<List<FlareEvent>>,
    cmes: RepositoryState<List<CmeEvent>>,
    onCmeDetails: (CmeEvent) -> Unit,
)
```

Implement it as:

1. One `Row` with `Modifier.fillMaxWidth().testTag("recent-events-row")`.
2. Use `Arrangement.spacedBy(10.dp)` or the existing Solar Activity block rhythm; do not add a new global spacing constant.
3. Render exactly two `Card` children, each with `Modifier.weight(1f)`:
   - `recent-flares-card`
   - `recent-cmes-card`
4. Match the restrained existing Solar Activity card treatment:
   - `CardDefaults.cardColors(containerColor = SpaceSurface)`
   - `BorderStroke(1.dp, SolarOrange.copy(alpha = 0.24f))`
5. Inside each card use a padded `Column`:
   - `Modifier.fillMaxWidth().padding(12.dp)`
   - `Arrangement.spacedBy(8.dp)`
6. Put the card title inside the card:
   - `Text("Recent Flares", ...)`
   - `Text("Recent CMEs", ...)`
7. Render the existing `FlareList(flares)` and `CmeList(cmes, onCmeDetails)` under their titles.
8. Do not add `LazyColumn`, `verticalScroll`, height forcing, truncation, or independent card scrolling.

In `solarActivityScienceItems`, remove the four current heading/list items for Recent Flares and Recent CMEs and replace them with one:

```kotlin
item {
    SolarEventCards(
        flares = state.flares,
        cmes = state.cmes,
        onCmeDetails = onCmeDetails,
    )
}
```

Because both compact and expanded layouts reuse `solarActivityScienceItems`, this automatically applies the paired cards to portrait and the landscape right pane without duplicate implementations.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest" --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test"
```

Both tests pass. `FlareList.kt` and `CmeList.kt` remain unmodified.

---

### Task 4: Prove both cards preserve full event content and CME Details

**Files:**  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Add a populated event-state helper to `SolarActivityUiTest` with at least:

- two flare events with distinct IDs;
- two CME events with distinct IDs;
- one CME with a non-null Details link.

Add:

```kotlin
@Test
fun compactEventCardsKeepCompleteListsAndDetailsAction() {
    var openedCme: CmeEvent? = null
    compose.setContent {
        SolarActivityScreen(
            state = populatedEventState(),
            expanded = false,
            onCmeDetails = { openedCme = it },
        )
    }

    compose.onNodeWithTag("solar-activity-compact")
        .performScrollToNode(hasTestTag("recent-events-row"))

    compose.onNodeWithTag("recent-flares-card").assertExists()
    compose.onNodeWithTag("recent-cmes-card").assertExists()
    compose.onNodeWithTag("flare-row-flare-1").assertExists()
    compose.onNodeWithTag("flare-row-flare-2").assertExists()
    compose.onNodeWithTag("cme-row-cme-1").assertExists()
    compose.onNodeWithTag("cme-row-cme-2").assertExists()
    compose.onNodeWithText("Details").performClick()

    compose.runOnIdle {
        assert(openedCme?.id == "cme-1")
    }
}
```

Run instrumentation compilation and confirm the test compiles against the intended tags/API but fails until the paired-card implementation exists.

**Implementation:**

No additional production behavior should be needed beyond Task 3. If the test exposes a missing tag or callback path, make the minimum correction in `SolarActivityScreen.kt` only.

Do not modify event sorting, repository order, UTC formatting, semantic accent rules, or link semantics.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

Instrumentation sources compile with the new event-card coverage.

---

### Task 5: Prove mixed event states remain independent inside the paired cards

**Files:**  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Extend the existing mixed-state test. Its current state already provides cached retained flare data and an empty CME state.

After scrolling to the paired event row, require:

```kotlin
rule.onNodeWithTag("recent-events-row").assertExists()
rule.onNodeWithTag("recent-flares-card").assertExists()
rule.onNodeWithTag("recent-cmes-card").assertExists()
rule.onNodeWithText("Showing cached flare data").assertExists()
rule.onNodeWithText("No recent CMEs").assertExists()
rule.onNodeWithTag("flare-row-flare-1").assertExists()
```

Keep the existing probability and Solar Imagery assertions.

**Implementation:**

No new state machinery should be added. The existing `FlareList` and `CmeList` must continue receiving their original independent `RepositoryState` values inside their respective cards.

If the test fails because the screen transforms the repository states, correct that wiring only. Do not change `FlareList.kt` or `CmeList.kt`.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

The mixed-state instrumentation test compiles and all existing partial-data structure remains intact.

---

### Task 6: Prove the paired cards are shared by expanded layout

**Files:**  
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Extend `expandedLayoutPinsImageryAndScrollsSciencePane` or add a focused test:

```kotlin
@Test
fun expandedSciencePaneUsesSamePairedEventCards() {
    compose.setContent {
        SolarActivityScreen(populatedEventState(), expanded = true)
    }

    compose.onNodeWithTag("solar-activity-expanded-scroll")
        .performScrollToNode(hasTestTag("recent-events-row"))

    compose.onNodeWithTag("recent-events-row").assertExists()
    compose.onNodeWithTag("recent-flares-card").assertExists()
    compose.onNodeWithTag("recent-cmes-card").assertExists()
    compose.onNodeWithTag("solar-activity-expanded-imagery").assertExists()
}
```

**Implementation:**

No separate expanded implementation should be introduced. Keep `SolarEventCards` inside the existing shared `solarActivityScienceItems` path.

If the test requires duplicated expanded-only event layout code, stop and correct the composition so both layouts share the same helper.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

The expanded-layout coverage compiles and uses the shared paired-event block.

---

### Task 7: Run focused Solar Activity regressions

**Files:** No production changes expected.

**Test first / verification gate:**

Run the focused unit tests that protect the touched behavior and nearby contracts:

```bash
./gradlew :app:testDebugUnitTest   --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"   --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test"   --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"   --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
```

Then compile instrumentation tests:

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

**Implementation:**

Fix only failures caused by this planned layout refinement. Do not broaden scope into chart, imagery, repository, or lifecycle refactors.

**Verify:**

All focused tests pass/compile. Specifically confirm:

- X-Ray 72-hour domain tests remain green.
- X-Ray reference-line tests remain green.
- Flare/CME repository-order and semantic presentation tests remain green.
- Imagery/fullscreen/ENLIL tests remain green.
- There are still only the two outer Solar Activity `LazyColumn` instances.

---

### Task 8: Run the full local verification gate and inspect scope

**Files:** No production changes expected.

**Test first / verification gate:**

Run exactly:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Inspect the final diff.

**Implementation:**

The expected production change is limited to:

- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

Expected test changes are limited to:

- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`
- `app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`
- `app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`

`SolarActivityPhase10Test.kt` should only change if needed to preserve/clarify an existing event-list contract; otherwise leave it untouched.

Do not modify:

- `XrayChart.kt`
- `HelioFluxLineChart.kt`
- `FlareList.kt`
- `CmeList.kt`
- `FlareProbabilityBadges.kt`
- imagery/fullscreen/ENLIL files
- ViewModel/repository/network/data files
- Home or Space Weather files

**Verify:**

All commands pass and the diff contains only planned files.

---

### Task 9: Push one implementation batch and monitor Android CI

**Files:** Git history only.

**Test first / pre-push gate:**

Do not push until Task 8 is fully green.

**Implementation:**

1. Commit the entire implementation as one focused batch to minimize GitHub Actions usage.
2. Suggested commit message:
   `ui: polish Solar Activity layout spacing`
3. Push to `main` once.
4. Monitor the resulting Android CI run during the same response.
5. Re-check roughly every 45 seconds while pending.
6. If CI fails:
   - inspect the failing step/logs;
   - reproduce or reason from the failure;
   - make the minimum correction;
   - push the fix;
   - continue monitoring until green.
7. Do not start unrelated work while CI is pending.

**Verify:**

Android CI is green through unit tests, instrumentation compilation, lint, debug APK assembly, and artifact upload. If the runner has no connected Android device, explicitly note that connected instrumentation execution remains pending rather than implying it ran.

---

### Task 10: Perform the physical Pixel acceptance pass

**Files:** No code changes unless a new defect is found.

**Test first / physical verification:**

On the Pixel, verify portrait and landscape:

1. C/M/X pills sit comfortably below the Android status bar.
2. Rotating the device does not reintroduce status-bar overlap.
3. X-Ray Activity heading sits tightly above the chart with an intentional ~8–12dp visual gap.
4. X-Ray chart itself is unchanged: 72-hour data, axes, guides, legend, inspection, and gestures still behave as before.
5. Recent Flares and Recent CMEs appear in two equal-width side-by-side cards.
6. Both cards show the complete event lists.
7. Long metadata wraps inside its own card without horizontal clipping.
8. One taller card is allowed to extend below the other; no forced equal-height blank space is introduced.
9. The page remains the only vertical scrolling surface; neither event card scrolls independently.
10. Cached/empty/failure states stay inside the correct card.
11. CME Details remains easy to tap.
12. Landscape keeps the imagery pane fixed and the right science pane independently scrollable.
13. Imagery selector, fullscreen imagery, HMI overlays, ENLIL playback, ACE EPAM, and pull-to-refresh remain unchanged.

**Implementation:**

If the physical pass reveals a distinct bug rather than a simple mismatch with this approved plan, stop and route that defect through `$debugprompt` before changing code.

**Verify:**

The Pixel acceptance checklist passes in portrait and landscape.

## Definition of Done

- [ ] Status-bar-safe top inset is applied at the Solar Activity screen boundary.
- [ ] Probability pills no longer collide with system status content.
- [ ] X-Ray heading and chart are grouped with tight intentional spacing.
- [ ] `XrayChart.kt` and shared chart internals are unchanged.
- [ ] Recent Flares and Recent CMEs render as two equal-width side-by-side cards.
- [ ] Both cards keep complete event lists.
- [ ] Event cards preserve UTC formatting, semantic accents, repository order, and state messages.
- [ ] CME Details remains wired.
- [ ] No nested event scrolling is introduced.
- [ ] Compact and expanded layouts share the same event-card implementation.
- [ ] All new behavior has test coverage written before production changes.
- [ ] Focused unit tests pass.
- [ ] Instrumentation tests compile.
- [ ] Full unit suite, lint, and debug APK build pass.
- [ ] No unrelated files are modified.
- [ ] One focused implementation batch is pushed.
- [ ] Android CI is green.
- [ ] Physical Pixel portrait/landscape acceptance passes.
