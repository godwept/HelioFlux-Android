# Solar Activity Event Card Colors Implementation Plan

**Date:** 2026-09-22  
**Design doc:** `docs/specs/2026-09-22-solar-activity-event-card-colors-design.md`  
**Status:** Ready for review

## Overview

Differentiate the existing full-width Recent Flares and Recent CMEs cards using subtle event-type tinting while preserving the current dark HelioFlux theme and all existing behavior. Recent Flares will use a low-alpha `SolarOrange` surface with a warm `WarningAmber` border and `SolarOrange` title. Recent CMEs will use a low-alpha `DataCyan` surface with a cool `DataBlue` border and `DataCyan` title. No data, state, list-row, Details-action, layout, chart, or global-theme behavior will change.

## Baseline

Current `main`:

`f761e7fae54040102b97c80e213fe67e43c927e6`

Latest code CI:

- Android CI #301
- Run ID `35789366860`
- GREEN
- Head SHA `5949b19d0ead8abe5fd6cc857e98183a3591c951`

The design-doc commit is under `docs/**` and did not trigger a new Android CI run.

## Tasks

### Task 1: Add a failing regression for distinct event-card visual tokens

**Files:**
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`

**Test first:**

Add a source-level test alongside `recentEventsUseStackedFullWidthCards` that slices only `SolarEventCards(...)` and requires the approved warm/cool treatments.

Use the same function boundaries already used by the existing layout regression:

```kotlin
@Test
fun recentEventCardsUseDistinctThemeTints() {
    val eventsStart = source.indexOf("private fun SolarEventCards(")
    val eventsEnd = source.indexOf("private fun XraySection(", eventsStart)

    assertTrue(eventsStart >= 0)
    assertTrue(eventsEnd > eventsStart)
    val eventsSource = source.substring(eventsStart, eventsEnd)

    assertTrue(
        eventsSource.contains(
            "containerColor = SolarOrange.copy(alpha = 0.08f)",
        ),
    )
    assertTrue(
        eventsSource.contains(
            "BorderStroke(1.dp, WarningAmber.copy(alpha = 0.36f))",
        ),
    )
    assertTrue(eventsSource.contains("color = SolarOrange"))

    assertTrue(
        eventsSource.contains(
            "containerColor = DataCyan.copy(alpha = 0.08f)",
        ),
    )
    assertTrue(
        eventsSource.contains(
            "BorderStroke(1.dp, DataBlue.copy(alpha = 0.36f))",
        ),
    )
    assertTrue(eventsSource.contains("color = DataCyan"))

    assertFalse(eventsSource.contains("containerColor = SpaceSurface"))
}
```

The alpha values intentionally keep the card surfaces subtle:
- surface tint: 8%;
- border accent: 36%.

Do not add screenshot/golden infrastructure for this change.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

Expected before implementation: `recentEventCardsUseDistinctThemeTints` fails because both cards still use `SpaceSurface` and `SolarOrange`.

---

### Task 2: Apply the warm flare-card treatment

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Use the failing `recentEventCardsUseDistinctThemeTints` regression from Task 1.

**Implementation:**

Add only the existing theme imports needed by the approved flare treatment:

```kotlin
import ca.stewark.helioflux.ui.theme.WarningAmber
```

In the `recent-flares-card` `Card`, replace the shared neutral styling:

```kotlin
colors = CardDefaults.cardColors(containerColor = SpaceSurface),
border = BorderStroke(1.dp, SolarOrange.copy(alpha = 0.24f)),
```

with:

```kotlin
colors =
    CardDefaults.cardColors(
        containerColor = SolarOrange.copy(alpha = 0.08f),
    ),
border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.36f)),
```

Keep the existing title treatment:

```kotlin
color = SolarOrange
```

Do not change:
- card modifier;
- card size;
- padding;
- spacing;
- typography;
- `FlareList(flares)`;
- flare row/class colors;
- loading/empty/failure handling.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

The flare assertions should now pass while the CME assertions remain red until Task 3.

---

### Task 3: Apply the cool CME-card treatment

**Files:**
- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`

**Test first:**

Continue from the failing CME portion of `recentEventCardsUseDistinctThemeTints`.

**Implementation:**

Add the existing theme imports:

```kotlin
import ca.stewark.helioflux.ui.theme.DataBlue
import ca.stewark.helioflux.ui.theme.DataCyan
```

In the `recent-cmes-card` `Card`, replace:

```kotlin
colors = CardDefaults.cardColors(containerColor = SpaceSurface),
border = BorderStroke(1.dp, SolarOrange.copy(alpha = 0.24f)),
```

with:

```kotlin
colors =
    CardDefaults.cardColors(
        containerColor = DataCyan.copy(alpha = 0.08f),
    ),
border = BorderStroke(1.dp, DataBlue.copy(alpha = 0.36f)),
```

Change only the card heading accent:

```kotlin
Text(
    "Recent CMEs",
    style = MaterialTheme.typography.titleMedium,
    color = DataCyan,
)
```

After both cards stop using it, remove the now-unused `SpaceSurface` import from `SolarActivityScreen.kt`.

Do not change:
- `CmeList.kt`;
- CME speed-emphasis colors;
- horizontal dividers;
- Details button styling;
- `onCmeDetails`;
- external URI handling in `HelioFluxApp.kt`;
- card modifier, padding, spacing, or typography.

**Verify:**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

All Solar Activity source tests pass, including:
- stacked full-width cards;
- distinct warm/cool theme tints;
- app-level CME Details URI wiring;
- no nested scrolling.

---

### Task 4: Re-run behavioral regressions around the cards and Details action

**Files:** No production changes expected.

**Test first / verification:**

Run the focused JVM Solar Activity suite:

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.*"
```

Compile instrumentation tests:

```bash
./gradlew :app:assembleDebugAndroidTest
```

If an Android device/emulator is available, run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

The existing instrumentation coverage must continue to verify:
- both event cards render;
- flare rows remain present;
- CME rows remain present;
- CME Details still invokes the supplied callback;
- compact and expanded layouts both expose the same stacked cards.

The existing app-level navigation test must continue to compile and, when connected instrumentation runs, verify that CME Details opens the event link through the URI handler.

**Verify:**

Confirm there are no modifications to:
- `FlareList.kt`;
- `CmeList.kt`;
- `HelioFluxApp.kt`;
- ViewModels;
- repositories;
- model/database code;
- charts;
- global theme color definitions.

---

### Task 5: Run full verification, commit once, and monitor CI

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

Inspect the final implementation diff. It should be limited to:

- `app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`
- `app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`

No new theme constants or abstractions should be introduced unless compilation demonstrates that the approved direct palette expressions are impossible.

To minimize GitHub Actions usage:

1. create an isolated implementation branch from current `main`;
2. make the test-first change there;
3. make the two card-style changes there;
4. verify the exact diff;
5. squash to one focused implementation commit on `main`;
6. push once;
7. monitor the triggered Android CI run during the implementation response.

Suggested commit message:

`style: distinguish solar event cards`

If CI fails, inspect the failure, make the minimum correction consistent with this plan, push, and continue monitoring until green.

**Verify:**

Android CI completes successfully.

Then physically verify on the Pixel:

- Recent Flares reads as a subtly warm/orange dark card;
- Recent CMEs reads as a subtly cool cyan/blue dark card;
- neither surface looks bright or saturated;
- flare class colors remain easy to distinguish;
- CME Normal/Elevated/Strong speed colors remain easy to distinguish;
- dividers remain visible;
- CME Details remains readable and opens the external event page;
- compact and expanded layouts retain the existing spacing and stacked full-width geometry.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Recent Flares uses a subtle warm/orange-tinted dark surface.
- [ ] Recent Flares uses a warm amber border.
- [ ] Recent Flares title remains SolarOrange.
- [ ] Recent CMEs uses a subtle cool cyan-tinted dark surface.
- [ ] Recent CMEs uses a cool blue border.
- [ ] Recent CMEs title uses DataCyan.
- [ ] The two cards are visually distinct without becoming bright/saturated.
- [ ] No new global theme colors are introduced.
- [ ] Existing stacked full-width layout is unchanged.
- [ ] Existing event-row styling is unchanged.
- [ ] Existing loading/empty/failure behavior is unchanged.
- [ ] Flare class emphasis remains unchanged.
- [ ] CME speed emphasis remains unchanged.
- [ ] CME Details action remains functional.
- [ ] Focused Solar Activity tests pass.
- [ ] Instrumentation tests compile.
- [ ] Full Android verification passes.
- [ ] No unrelated files are modified.
- [ ] Android CI is green.
- [ ] Pixel verification confirms the approved warm/cool treatment and readable Details action.
