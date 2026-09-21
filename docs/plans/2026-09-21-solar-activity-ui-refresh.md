# Solar Activity UI Refresh Implementation Plan

**Date:** 2026-09-21  
**Design doc:** `docs/specs/2026-09-21-solar-activity-ui-refresh-design.md`  
**Status:** Ready for review

## Overview

Refresh Solar Activity without changing repositories, the ViewModel, data sources, or chart engine. Portrait becomes one true-black lazy scientific feed. Landscape becomes a fixed Solar Imagery pane on the left and an independently scrollable/pull-to-refresh science pane on the right, using the same HMI/C2/C3/ENLIL selector in both orientations. The work also replaces AssistChips and generic event cards with HelioFlux scientific presentation, formats timestamps in UTC, replaces the current card-sized imagery dialog with a true full-screen viewer, preserves HMI overlays and static-image zoom/pan, and reuses the existing ENLIL blended playback in full screen.

Tasks 1–14 modify the same Solar Activity UI subsystem and should be implemented as one TDD batch once this plan is approved. Run focused tests after each task, run the full local verification gate once, then push one implementation commit to minimize GitHub Actions usage and monitor Android CI to green.

## Tasks

### Task 1: Lock UTC and scientific-presentation formatting

**Files:**  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPresentationTest.kt` (new)

**Test first:**

Create pure unit tests for the presentation rules before adding production helpers. Use this fixed timestamp:

```kotlin
private const val SampleUtcMillis = 1789907640000L // 2026-09-20 12:34 UTC
```

Cover:

```kotlin
@Test
fun utcTimestampUsesHumanReadableSolarActivityFormat() {
    assertEquals("Sep 20, 12:34 UTC", formatSolarActivityUtc(SampleUtcMillis))
}

@Test
fun flareProbabilityPresentationUsesApprovedClassesValuesAndAccents() {
    val source = DataSourceKey("probabilities")
    val p =
        flareProbabilityPresentation(
            RepositoryState.Available(
                FlareProbabilities(c = 60, m = 25, x = 5),
                source,
                DataFreshness.Fresh,
            ),
        )

    assertEquals(listOf("C", "M", "X"), p.metrics.map { it.label })
    assertEquals(listOf("60%", "25%", "5%"), p.metrics.map { it.value })
    assertEquals(
        listOf(WarningAmber, SolarOrange, AlertRed),
        p.metrics.map { it.accent },
    )
    assertNull(p.message)
}

@Test
fun flareProbabilityPresentationKeepsLoadingAndUnavailableInline() {
    assertEquals(
        "Loading flare probabilities",
        flareProbabilityPresentation(RepositoryState.Loading).message,
    )
    val source = DataSourceKey("probabilities")
    assertEquals(
        "Flare probabilities unavailable",
        flareProbabilityPresentation(
            RepositoryState.Failure(source, "down", null, null),
        ).message,
    )
}

@Test
fun flarePresentationUsesUtcAndScientificMetadata() {
    val p =
        flareEventPresentation(
            FlareEvent(
                id = "flare",
                flareClass = "M1.7",
                timestampMillis = SampleUtcMillis,
                observatory = "GOES",
                region = "12345",
                location = "N12W34",
            ),
        )

    assertEquals("M1.7", p.flareClass)
    assertEquals("Sep 20, 12:34 UTC", p.time)
    assertEquals("GOES · AR 12345 · N12W34", p.metadata)
    assertEquals(SolarOrange, p.accent)
}

@Test
fun flareClassAccentsFollowApprovedPalette() {
    assertEquals(SpaceMuted, flareClassAccent("A1"))
    assertEquals(FreshGreen, flareClassAccent("B2"))
    assertEquals(DataCyan, flareClassAccent("C3"))
    assertEquals(SolarOrange, flareClassAccent("M4"))
    assertEquals(AlertRed, flareClassAccent("X5"))
}

@Test
fun cmePresentationPreservesSpeedDirectionAndFullWidth() {
    val p =
        cmeEventPresentation(
            CmeEvent(
                id = "cme",
                timestampMillis = SampleUtcMillis,
                speed = 1250.0,
                halfAngle = 35.0,
                direction = "NW",
                type = null,
                link = "https://example.test/cme",
            ),
        )

    assertEquals("1250 km/s", p.speed)
    assertEquals("Sep 20, 12:34 UTC", p.time)
    assertEquals("NW · 70° wide", p.metadata)
    assertEquals(CmeSpeedEmphasis.Strong, p.emphasis)
}
```

Also cover failure-with-retained flare probabilities so the retained C/M/X values are still presented.

**Implementation:**

No production changes in this task.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"
```

The new tests fail because the presentation helpers do not yet exist.

---

### Task 2: Add shared Solar Activity presentation helpers

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPresentation.kt` (new)  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPresentationTest.kt`

**Test first:**

Use the failing tests from Task 1.

**Implementation:**

Add an immutable UTC formatter local to Solar Activity:

```kotlin
private val SolarActivityUtcFormatter =
    DateTimeFormatter
        .ofPattern("MMM dd, HH:mm 'UTC'", Locale.US)
        .withZone(ZoneOffset.UTC)

internal fun formatSolarActivityUtc(timestampMillis: Long): String =
    SolarActivityUtcFormatter.format(Instant.ofEpochMilli(timestampMillis))
```

Add small immutable UI presentation models:

```kotlin
data class FlareProbabilityMetric(
    val label: String,
    val value: String,
    val accent: Color,
)

data class FlareProbabilityPresentation(
    val metrics: List<FlareProbabilityMetric>,
    val message: String?,
)

data class FlareEventPresentation(
    val flareClass: String,
    val time: String,
    val metadata: String,
    val accent: Color,
)

data class CmeEventPresentation(
    val speed: String,
    val time: String,
    val metadata: String,
    val emphasis: CmeSpeedEmphasis,
)
```

Implement:

- `flareProbabilityPresentation(state)`: Available and retained Failure -> C/M/X metrics; Loading -> loading message; Empty or failure without retained data -> unavailable message.
- C/M/X accents -> `WarningAmber`, `SolarOrange`, `AlertRed`.
- `flareClassAccent(value)` using existing `flareClassGroup`:
  - A -> `SpaceMuted`
  - B -> `FreshGreen`
  - C -> `DataCyan`
  - M -> `SolarOrange`
  - X -> `AlertRed`
- `flareEventPresentation(flare)`: UTC time plus nonblank metadata pieces joined with `" · "`; prefix a region with `"AR "`.
- `cmeEventPresentation(cme)`: integer km/s or `"Speed unavailable"`, UTC time, optional direction, optional full width as `2 * halfAngle`, and existing `cmeSpeedEmphasis`.

Do not create a global formatter or change chart formatters.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"
```

All Task 1 tests pass.

---

### Task 3: Replace flare-probability AssistChips with metric pills

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareProbabilityBadges.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`

**Test first:**

Update `SolarActivityComponentsTest.kt` away from the old `flareProbabilityLabels` contract and add a source test that asserts:

```kotlin
val source =
    File("src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareProbabilityBadges.kt")
        .readText()

assertFalse(source.contains("AssistChip("))
assertTrue(source.contains("RoundedCornerShape(50)"))
assertTrue(source.contains("SpaceSurface"))
assertTrue(source.contains("flare-probability-c"))
assertTrue(source.contains("flare-probability-m"))
assertTrue(source.contains("flare-probability-x"))
```

Run before implementation and confirm it fails on the current AssistChip UI.

**Implementation:**

Refactor `FlareProbabilityBadges` to call `flareProbabilityPresentation(state)`.

For metrics, render one full-width `Row` tagged `flare-probability-strip` with three equal-width pill `Surface` elements modeled on the existing Home/Space Weather metric language:

- `RoundedCornerShape(50)`;
- `SpaceSurface`;
- 1dp accent border at about 0.52 alpha;
- 5dp accent shadow using the same restrained alpha values as existing metric pills;
- compact 10dp horizontal / 6dp vertical padding;
- centered class label and semibold percentage;
- tags `flare-probability-c`, `flare-probability-m`, `flare-probability-x`.

If presentation contains only a message, render that message inline as small muted text. Do not add click behavior.

Delete `flareProbabilityLabels` after tests no longer use it.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest" --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"
```

Both focused classes pass.

---

### Task 4: Convert Recent Flares into compact scientific rows

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareList.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt`

**Test first:**

Keep existing `flareClassGroup` coverage and add:

```kotlin
@Test
fun flareListUsesRepositoryOrderAndScreenOwnedHeading() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareList.kt")
            .readText()

    assertFalse(source.contains("sortedByDescending"))
    assertFalse(source.contains("Recent flares"))
    assertTrue(source.contains("flare-row-"))
}
```

It should fail before production changes.

**Implementation:**

Change the API to:

```kotlin
@Composable
fun FlareList(
    state: RepositoryState<List<FlareEvent>>,
    modifier: Modifier = Modifier,
)
```

Handle state inside the component:

- Loading -> `"Loading flare data"`.
- Empty -> `"No recent flares"`.
- Failure without retained data -> `"Flare data unavailable"`.
- Failure with retained data -> render retained events and a subtle `"Showing cached flare data"` label.
- Available -> render events.

Render events exactly in supplied order; do not sort again.

Each row uses `flareEventPresentation`:

- flare class is the leading emphasized value using the class accent;
- UTC time is the other primary field;
- metadata appears below in `SpaceMuted`;
- use restrained spacing/dividers instead of Material cards;
- tag each row `flare-row-${flare.id}`.

Remove the local `Recent flares` title; the screen will own `HelioFluxSectionHeading("Recent Flares")`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test" --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"
```

The flare contracts pass.

---

### Task 5: Convert Recent CMEs into compact scientific rows

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/CmeList.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt`

**Test first:**

Keep existing `cmeSpeedEmphasis` threshold tests and add:

```kotlin
@Test
fun cmeListUsesRepositoryOrderAndScreenOwnedHeading() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/CmeList.kt")
            .readText()

    assertFalse(source.contains("sortedByDescending"))
    assertFalse(source.contains("Recent CMEs"))
    assertTrue(source.contains("cme-row-"))
    assertTrue(source.contains("Details"))
}
```

**Implementation:**

Change the API to:

```kotlin
@Composable
fun CmeList(
    state: RepositoryState<List<CmeEvent>>,
    onDetails: (CmeEvent) -> Unit,
    modifier: Modifier = Modifier,
)
```

Handle Loading, Empty, Failure-without-retained, Failure-with-retained, and Available the same way as the flare list.

Render events in supplied repository order.

Each row uses `cmeEventPresentation`:

- speed leads;
- UTC time is the other primary field;
- direction/full-width metadata appears below;
- Normal -> `SpaceMuted`, Elevated -> `SolarOrange`, Strong -> `AlertRed`;
- keep `Details` only when `cme.link != null`;
- tag each row `cme-row-${cme.id}`.

Remove the local `Recent CMEs` heading.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test" --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest"
```

The CME contracts pass.

---

### Task 6: Lock the shared HMI/C2/C3/ENLIL selector contract

**Files:**  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarImagerySelectorTest.kt` (new)  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt` (new)

**Test first:**

Create `SolarImagerySelectorTest.kt`:

```kotlin
@Test
fun imagerySelectorUsesApprovedOrderAndLabels() {
    assertEquals(
        listOf(
            SolarGalleryItem.Hmi,
            SolarGalleryItem.LascoC2,
            SolarGalleryItem.LascoC3,
            SolarGalleryItem.Enlil,
        ),
        SolarGalleryItem.entries,
    )
    assertEquals(
        listOf("HMI", "C2", "C3", "ENLIL"),
        SolarGalleryItem.entries.map { it.selectorLabel },
    )
}

@Test
fun hmiIsTheDefaultSolarImagerySelection() {
    assertEquals(SolarGalleryItem.Hmi, DefaultSolarGalleryItem)
}

@Test
fun oldCarouselAndTwoByTwoGridAreRemoved() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryGallery.kt")
            .readText()

    assertFalse(source.contains("LazyRow"))
    assertFalse(source.contains("chunked(2)"))
}
```

Create `SolarActivityUiTest.kt` with a helper state containing Available HMI/C2/C3 images and this interaction:

```kotlin
@Test
fun imagerySelectorStartsOnHmiAndSwitchesToC2() {
    compose.setContent {
        SolarActivityScreen(
            state = populatedImageryState(),
            expanded = false,
            nowMillis = SampleNow,
        )
    }

    compose.onNodeWithTag("solar-imagery-selector").assertExists()
    compose.onNodeWithTag("solar-imagery-stage-hmi").assertExists()
    compose.onNodeWithTag("solar-imagery-c2").performClick()
    compose.onNodeWithTag("solar-imagery-stage-c2").assertExists()
    compose.onNodeWithTag("solar-imagery-stage-hmi").assertDoesNotExist()
}
```

Use `https://example.test/...` URLs; the test only checks Compose state/semantics.

**Implementation:**

No production code yet.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarImagerySelectorTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

The new selector expectations are red.

---

### Task 7: Replace the imagery carousel/grid with one selected imagery stage

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryGallery.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarImagerySelectorTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Use Task 6 failures.

**Implementation:**

Refactor `SolarImageryGallery.kt`:

1. Keep `SolarGalleryItem`, but format it normally and add:
   ```kotlin
   internal val DefaultSolarGalleryItem = SolarGalleryItem.Hmi

   val SolarGalleryItem.selectorLabel: String
       get() = when (this) {
           SolarGalleryItem.Hmi -> "HMI"
           SolarGalleryItem.LascoC2 -> "C2"
           SolarGalleryItem.LascoC3 -> "C3"
           SolarGalleryItem.Enlil -> "ENLIL"
       }
   ```
2. Replace the `expanded:Boolean` parameter with:
   ```kotlin
   selected: SolarGalleryItem,
   onSelected: (SolarGalleryItem) -> Unit,
   ```
3. Render one full-width selector row tagged `solar-imagery-selector`.
4. Give selector items equal width, rounded dark surfaces, subtle borders, and a Solar Orange selected state.
5. Add tags:
   - `solar-imagery-hmi`
   - `solar-imagery-c2`
   - `solar-imagery-c3`
   - `solar-imagery-enlil`.
6. Beneath the selector render exactly one source:
   - HMI -> `MagnetogramCard`
   - C2 -> `SolarImageryCard("LASCO C2", "SOHO / LASCO", ...)`
   - C3 -> `SolarImageryCard("LASCO C3", "SOHO / LASCO", ...)`
   - ENLIL -> `EnlilCard(..., visible = true, ...)`
7. Tag selected stages:
   - `solar-imagery-stage-hmi`
   - `solar-imagery-stage-c2`
   - `solar-imagery-stage-c3`
   - `solar-imagery-stage-enlil`.
8. Do not add pager/swipe state or persist selection in the ViewModel.

Because only the selected branch is composed, ENLIL is not presented when another source is selected. Do not change `EnlilCard` playback/lifecycle internals in this task.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarImagerySelectorTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

The selector contract passes and instrumentation sources compile.

---

### Task 8: Polish imagery cards and centralize HMI overlay rendering

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/MagnetogramCard.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt`

**Test first:**

Extend `SolarActivityComponentsTest.kt`:

```kotlin
@Test
fun imageryPresentationFormatsUtcTimestamp() {
    val source = DataSourceKey("image")
    val p =
        solarImageryPresentation(
            "LASCO C2",
            "SOHO / LASCO",
            RepositoryState.Available(
                SolarImage(
                    SolarImageType.LascoC2,
                    1789907640000L,
                    "https://example.test/c2.gif",
                ),
                source,
                DataFreshness.Fresh,
            ),
        )

    assertEquals("Sep 20, 12:34 UTC", p.updatedTime)
}
```

Add source assertions that `SolarImageryCard.kt` uses `SpaceSurface`, a black image stage, and no longer renders raw `timestampMillis`.

Keep the existing `mapActiveRegion` assertions unchanged.

**Implementation:**

Update `SolarImageryPresentation` to expose a formatted optional `updatedTime` instead of making the UI render raw milliseconds.

Polish `SolarImageryCard`:

- dark `SpaceSurface` shell with restrained border;
- image stage is square, black, and `ContentScale.Fit`;
- title is primary;
- source uses a compact cyan/source-label treatment;
- timestamp reads `Updated <formatted UTC>`;
- loading/failure stays inside the card;
- click is enabled only when usable retained/available imagery has a URL.

Extract the region-label drawing from `MagnetogramCard` into:

```kotlin
@Composable
internal fun BoxScope.ActiveRegionOverlay(
    regions: List<ActiveRegion>,
    modifier: Modifier = Modifier,
)
```

Keep `mapActiveRegion` unchanged. Use `ActiveRegionOverlay` over the HMI image stage so the same overlay can later be reused by fullscreen.

Do not change active-region coordinate math.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
```

All imagery presentation/mapping tests pass.

---

### Task 9: Extract reusable ENLIL animation content for normal and fullscreen use

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarImagerySelectorTest.kt`

**Test first:**

Keep the existing preload-selection and 200ms blend-progress tests.

Add a source contract:

```kotlin
@Test
fun enlilCardDelegatesAnimatedStageToReusablePlayer() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt")
            .readText()

    assertTrue(source.contains("fun EnlilAnimation("))
    assertTrue(source.contains("EnlilAnimation("))
}
```

**Implementation:**

Extract the existing frame preload, playable-frame selection, index/blend state, `LaunchedEffect`, and two blended `AsyncImage` layers into:

```kotlin
@Composable
fun EnlilAnimation(
    state: RepositoryState<List<EnlilFrame>>,
    visible: Boolean,
    modifier: Modifier = Modifier,
    testTag: String? = null,
)
```

Keep:

- `selectPlayableEnlilFrames`;
- `enlilBlendProgress`;
- 200ms frame interval;
- poster fallback while preload completes;
- no-frame behavior.

Refactor `EnlilCard` to render `EnlilAnimation` inside its card and retain its title/click behavior.

Do not change cadence, preload strategy, or lifecycle conditions beyond moving the same logic into the reusable composable.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest" --tests "ca.stewark.helioflux.ui.solaractivity.SolarImagerySelectorTest"
```

Playback contracts stay green.

---

### Task 10: Replace AlertDialog with a true full-screen imagery surface

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Keep the existing `ImageryTransformState` clamp/reset test.

Add a source test:

```kotlin
@Test
fun fullscreenViewerIsEdgeToEdgeInsteadOfAlertDialog() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt")
            .readText()

    assertFalse(source.contains("AlertDialog("))
    assertTrue(source.contains("Dialog("))
    assertTrue(source.contains("usePlatformDefaultWidth = false"))
    assertTrue(source.contains("fillMaxSize()"))
    assertTrue(source.contains("fullscreen-imagery-viewer"))
}
```

Add an instrumentation test that taps the selected HMI stage, asserts `fullscreen-imagery-viewer` exists, taps `fullscreen-imagery-close`, and verifies the fullscreen tag disappears.

**Implementation:**

Rewrite `FullscreenImageryViewer` using `Dialog` with `DialogProperties(usePlatformDefaultWidth = false)`.

The root is:

- `Modifier.fillMaxSize()`;
- black background;
- tag `fullscreen-imagery-viewer`.

Provide a minimal top-end close `IconButton` tagged `fullscreen-imagery-close`.

Keep the image initially fitted to available bounds and preserve `ImageryTransformState` plus `transformable` for zoom/pan.

Change the API so the viewer can render either a zoomable static image or ENLIL content without adding separate dialogs. A focused form is:

```kotlin
@Composable
fun FullscreenImageryViewer(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
)
```

Add a small reusable `ZoomableSolarImage` composable in the same file that owns `ImageryTransformState`, `AsyncImage`, and an optional overlay slot. Do not add navigation, download, share, or playback controls.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test"
./gradlew :app:compileDebugAndroidTestKotlin
```

Fullscreen source contract passes and Android tests compile.

---

### Task 11: Wire HMI overlays and animated ENLIL into fullscreen

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Extend `SolarActivityUiTest.kt`:

1. With Available HMI plus one active region, open HMI fullscreen and assert `fullscreen-active-region-overlay` exists.
2. Select ENLIL with at least two test frames, open it, and assert `fullscreen-enlil-player` exists.
3. Close the dialog and assert the selector still indicates/continues to render the previously selected imagery item.

The test verifies content selection and reuse, not real network frame loading.

**Implementation:**

In `SolarActivityScreen`, keep local:

```kotlin
var selectedImagery by remember { mutableStateOf(DefaultSolarGalleryItem) }
var viewer by remember { mutableStateOf<SolarGalleryItem?>(null) }
```

When `viewer` is:

- HMI: resolve Available or retained image URL, render `ZoomableSolarImage`, and reuse `ActiveRegionOverlay` in an overlay box tagged `fullscreen-active-region-overlay`.
- C2/C3: resolve Available or retained image URL and render `ZoomableSolarImage`.
- ENLIL: render `EnlilAnimation(state.enlil, visible = true, Modifier.fillMaxSize(), testTag = "fullscreen-enlil-player")`.

Do not reduce ENLIL to `firstOrNull().url`.

Closing fullscreen changes only `viewer = null`; it must not reset `selectedImagery`.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest" --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest"
```

The new instrumentation sources compile and existing HMI/ENLIL unit contracts remain green.

---

### Task 12: Lock the adaptive screen hierarchy before restructuring the screen

**Files:**  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt` (new)  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`

**Test first:**

Create `SolarActivityScreenSourceTest.kt` with source-contract assertions for the approved hierarchy:

```kotlin
@Test
fun solarActivityUsesSharedSectionHeadingsAndNoGenericPageTitle() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt")
            .readText()

    assertFalse(source.contains("Text(\"Solar Activity\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"Solar Imagery\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"X-Ray Activity\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"Recent Flares\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"Recent CMEs\""))
    assertTrue(source.contains("HelioFluxSectionHeading(\"Particle Environment\""))
}

@Test
fun compactSolarActivityUsesLazyFeedAndExpandedUsesFixedSplit() {
    val source =
        File("src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt")
            .readText()

    assertTrue(source.contains("LazyColumn("))
    assertTrue(source.contains("ExpandedSolarImageryWeight"))
    assertTrue(source.contains("ExpandedSolarDataWeight"))
    assertTrue(source.contains("solar-activity-expanded-imagery"))
    assertTrue(source.contains("solar-activity-expanded-content"))
}
```

Add instrumentation expectations:

- compact: `solar-activity-compact`, `flare-probability-strip`, `Solar Imagery`, selector, selected HMI stage; scroll to `X-Ray Activity`, `Recent Flares`, `Recent CMEs`, and `Particle Environment`;
- expanded: `solar-activity-expanded`, `solar-activity-expanded-imagery`, `solar-activity-expanded-content`, `solar-activity-expanded-scroll`;
- selector exists in both modes.

Update `PartialDataTest.kt` so it no longer expects the removed `Solar Activity` page title. It should instead assert the probability value plus at least one stable section heading such as `Solar Imagery`, while still proving mixed section states do not become a screen-wide error.

**Implementation:**

No production changes in this task.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

The new layout contract is red against the current eager Column.

---

### Task 13: Build the portrait true-black lazy Solar Activity feed

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/AceEpamChart.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`

**Test first:**

Use the compact failures from Task 12.

**Implementation:**

Refactor `SolarActivityScreen` to dispatch:

```kotlin
if (expanded) {
    ExpandedSolarActivityLayout(...)
} else {
    CompactSolarActivityLayout(...)
}
```

For compact/portrait:

1. Keep one `PullToRefreshBox` tagged `solar-activity-pull-refresh`.
2. Use a root true-black surface/background.
3. Use one vertical `LazyColumn` tagged `solar-activity-compact` with horizontal 16dp and vertical 12dp padding.
4. Remove the generic `Solar Activity` title row.
5. Keep the current aggregate freshness derivation and show `FreshnessIndicator` compactly at the top end of the content, without reintroducing a page title.
6. Render in this exact order:
   - `FlareProbabilityBadges`
   - `HelioFluxSectionHeading("Solar Imagery", topSpacing = 0.dp)`
   - `SolarImageryGallery(selectedImagery, ...)`
   - `HelioFluxSectionHeading("X-Ray Activity")`
   - `XrayChart(..., height = 260.dp)`
   - `HelioFluxSectionHeading("Recent Flares")`
   - `FlareList(state.flares)`
   - `HelioFluxSectionHeading("Recent CMEs")`
   - `CmeList(state.cmes, onCmeDetails)`
   - `HelioFluxSectionHeading("Particle Environment")`
   - `AceEpamChart(...)`.
7. Keep the current 72-hour `chartDomain`.
8. Do not add nested vertical scrolling to event lists.
9. Stop extracting flare/CME lists into local raw-data variables; pass repository states so each section can show loading/empty/retained failure correctly.
10. Keep X-Ray/EPAM Available/retained-data extraction only as needed by the existing chart APIs.

In `AceEpamChart.kt`, remove its internal `Text("ACE EPAM")` title because the screen now owns the `Particle Environment` section heading. Keep the chart series, 240dp chart height, scientific axis format, and empty message unchanged.

Do not change `XrayChart.kt`.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

Compact layout contract passes and instrumentation compiles.

---

### Task 14: Build the fixed-imagery / scrolling-data landscape layout

**Files:**  
`app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt`  
`app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`

**Test first:**

Use the expanded failures from Task 12. Add exact split assertions:

```kotlin
@Test
fun expandedSolarActivityUsesApprovedSplitWeights() {
    assertEquals(0.43f, ExpandedSolarImageryWeight, 0.0f)
    assertEquals(0.57f, ExpandedSolarDataWeight, 0.0f)
}
```

**Implementation:**

Add:

```kotlin
internal const val ExpandedSolarImageryWeight = 0.43f
internal const val ExpandedSolarDataWeight = 0.57f
```

Implement `ExpandedSolarActivityLayout` as a true-black `Row` tagged `solar-activity-expanded`.

Left pane:

- weight `ExpandedSolarImageryWeight`;
- fill height;
- start 16dp / top 12dp / end 8dp / bottom 12dp padding, matching the established adaptive screens;
- tag `solar-activity-expanded-imagery`;
- `HelioFluxSectionHeading("Solar Imagery", topSpacing = 0.dp)`;
- shared HMI/C2/C3/ENLIL selector;
- large selected imagery stage;
- fixed in place with no vertical scroll.

Right pane:

- one `PullToRefreshBox`;
- weight `ExpandedSolarDataWeight`;
- fill height;
- tag `solar-activity-expanded-content`;
- one `LazyColumn` tagged `solar-activity-expanded-scroll`;
- start 8dp / end 16dp / vertical 12dp padding;
- render:
  - freshness indicator + C/M/X probability pills;
  - X-Ray Activity heading/chart;
  - Recent Flares heading/list;
  - Recent CMEs heading/list;
  - Particle Environment heading/ACE EPAM.

The left imagery pane must not be inside the right scroll container or pull-to-refresh surface. The selector/state is shared with portrait and fullscreen.

Do not change 43/57 unless implementation proves literal clipping; if that occurs, stop and revise the design/plan rather than silently changing the approved proportions.

**Verify:**

```bash
./gradlew :app:testDebugUnitTest --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
./gradlew :app:compileDebugAndroidTestKotlin
```

Expanded layout contract passes and instrumentation sources compile.

---

### Task 15: Complete Solar Activity interaction and resilience instrumentation coverage

**Files:**  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityRefreshTest.kt`  
`app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt`

**Test first:**

Before any correction, run the new/updated instrumentation sources through compilation and, if an emulator/device is available, execute the Solar Activity classes.

Coverage in `SolarActivityUiTest` should include:

- compact selector starts HMI and switches C2/C3/ENLIL;
- expanded mode exposes the same selector;
- only the selected imagery stage is present;
- compact order is reachable by scrolling;
- expanded imagery pane remains present while the right pane scrolls to Particle Environment;
- HMI opens true fullscreen and closes;
- fullscreen HMI has the active-region overlay tag when region data exists;
- ENLIL fullscreen uses the animated-player tag;
- closing fullscreen preserves the selected source;
- CME Details invokes the existing callback for a linked CME.

Keep `SolarActivityRefreshTest` using the existing `solar-activity-pull-refresh` tag in compact mode. Add an expanded refresh test against `solar-activity-expanded-content` only if the Compose pull gesture is stable in the existing test environment; otherwise source/layout contract plus compact PTR regression is sufficient and physical testing covers expanded PTR.

Update `PartialDataTest` to assert mixed state rendering through stable section/probability text, not the removed page title.

**Implementation:**

No planned production changes in this task. If a test exposes a regression, add the narrowest reproduction and make only the correction required by Tasks 1–14.

If the implementation reveals a distinct ENLIL lifecycle/performance bug beyond the selected-only composition required by this design, stop and route that defect through `$debugprompt` before modifying playback/lifecycle logic.

**Verify:**

```bash
./gradlew :app:compileDebugAndroidTestKotlin
```

If a connected device/emulator is available:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=ca.stewark.helioflux.ui.solaractivity
```

Instrumentation compiles, and connected Solar Activity tests pass when available.

---

### Task 16: Run focused Solar Activity regression tests

**Files:** No planned production changes.

**Test first:**

No new test solely for this gate.

**Implementation / verification:**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPresentationTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarImagerySelectorTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityComponentsTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityPhase10Test" \
  --tests "ca.stewark.helioflux.ui.solaractivity.EnlilPlaybackTest" \
  --tests "ca.stewark.helioflux.ui.solaractivity.SolarActivityScreenSourceTest"
```

Confirm specifically that:

- 72-hour chart domain/reference-line tests still pass;
- no Space Weather timeframe selector appears;
- HMI mapping math is unchanged;
- ENLIL 200ms blend contracts are unchanged;
- CME thresholds are unchanged;
- new UTC/event/selector/adaptive-layout tests pass.

**Verify:**

All focused Solar Activity unit tests pass.

---

### Task 17: Run the full local verification gate and inspect scope

**Files:** No planned production changes. Any newly discovered defect gets a focused failing test before correction.

**Test first:**

No new test solely for this gate.

**Implementation / verification:**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:lintDebug :app:assembleDebug
```

Inspect the diff:

```bash
git status --short
git diff --stat
```

Expected production files are limited to:

```text
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPresentation.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreen.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryGallery.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/SolarImageryCard.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/MagnetogramCard.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/EnlilCard.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareProbabilityBadges.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FlareList.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/CmeList.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/FullscreenImageryViewer.kt
app/src/main/java/ca/stewark/helioflux/ui/solaractivity/AceEpamChart.kt
```

Expected test files are limited to the existing Solar Activity tests plus the three planned new tests:

```text
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPresentationTest.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarImagerySelectorTest.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityScreenSourceTest.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityComponentsTest.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityPhase10Test.kt
app/src/test/java/ca/stewark/helioflux/ui/solaractivity/EnlilPlaybackTest.kt
app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityUiTest.kt
app/src/androidTest/java/ca/stewark/helioflux/ui/solaractivity/SolarActivityRefreshTest.kt
app/src/androidTest/java/ca/stewark/helioflux/ui/PartialDataTest.kt
```

Do not modify `SolarActivityViewModel.kt`, repositories, DAOs, parsers, network code, chart engine files, Home, Space Weather, or the PWA.

**Verify:**

All Gradle commands pass and no unplanned files are modified.

---

### Task 18: Push one implementation batch and monitor Android CI

**Files:** No additional planned source changes.

**Test first:**

No test for the push itself. If CI exposes a defect, reproduce it locally with the narrowest relevant failing test before changing code.

**Implementation / verification:**

1. Commit Tasks 1–15 together after Tasks 16–17 are green.
2. Suggested commit message:
   ```text
   ui: refresh Solar Activity presentation
   ```
3. Push once to `main`.
4. Actively monitor the resulting Android CI run during the same implementation response.
5. Re-check roughly every 45 seconds while pending.
6. If CI fails:
   - inspect the failing job/log;
   - reproduce locally with focused coverage where possible;
   - make the minimum correction;
   - push the correction;
   - continue monitoring until green.
7. Do not begin unrelated UI work while CI is pending.

**Verify:**

Android CI completes successfully for the final implementation SHA.

---

### Task 19: Perform the physical Pixel acceptance pass

**Files:** No planned source changes. Any discovered defect starts the project `$debugprompt` workflow before production changes.

**Test first:**

No automated test is added solely for manual acceptance.

**Implementation / verification:**

On the physical Pixel, verify portrait:

1. True-black Solar Activity canvas.
2. No generic `Solar Activity` page title.
3. C/M/X scientific pills are readable and centered.
4. `Solar Imagery` uses HMI/C2/C3/ENLIL selector, not a carousel.
5. HMI is the initial selection.
6. Switching all four sources updates the single large visual.
7. HMI active-region markers remain aligned.
8. C2/C3 imagery fits correctly.
9. ENLIL still blends/animates normally.
10. Tapping HMI/C2/C3 opens a true full-screen black viewer; pinch/pan remains smooth.
11. HMI overlays remain visible/aligned in full screen.
12. Tapping ENLIL opens full screen and the animation continues.
13. Closing full screen returns to the previously selected source.
14. X-Ray Activity remains the existing 72-hour chart.
15. Flares and CMEs show compact UTC rows and CME Details still works.
16. ACE EPAM remains the existing chart under Particle Environment.
17. Pull-to-refresh still works.
18. Long-page scrolling is smooth and sections are reachable.

Rotate to landscape and verify:

19. Solar Imagery remains fixed in the left pane.
20. The same selector works on the left.
21. The right pane begins with probability pills and scrolls independently.
22. X-Ray, Flares, CMEs, and Particle Environment remain reachable.
23. Pull-to-refresh acts from the right pane while imagery remains fixed.
24. Fullscreen static imagery and ENLIL work from landscape.
25. Rotate back to portrait; selection/layout returns without clipped or duplicated content.

If a real ENLIL lifecycle/performance issue is observed, stop this feature workflow and run `.github/agents/debugprompt.agent.md` for that defect before changing ENLIL lifecycle behavior.

**Verify:**

The complete portrait/landscape device checklist passes with no chart, refresh, imagery, or event-detail regression.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production change was preceded by focused failing coverage
- [ ] Solar Activity uses a true-black canvas
- [ ] Generic Solar Activity page title is removed
- [ ] Shared HelioFlux headings are used for Solar Imagery, X-Ray Activity, Recent Flares, Recent CMEs, and Particle Environment
- [ ] C/M/X probabilities use scientific metric pills
- [ ] Portrait and landscape use the same HMI/C2/C3/ENLIL selector
- [ ] HMI is the default selected source
- [ ] Old imagery carousel and 2x2 expanded grid are removed
- [ ] Only the selected imagery source is presented
- [ ] Portrait is a lazy single feed
- [ ] Landscape is fixed imagery left / scrolling data right at 43% / 57%
- [ ] Static imagery opens in a true full-screen black viewer
- [ ] Static fullscreen keeps pinch-to-zoom and pan
- [ ] HMI active-region overlays remain aligned in normal and fullscreen presentation
- [ ] ENLIL keeps the existing 200ms blended animation and remains animated fullscreen
- [ ] Human-readable UTC timestamps replace raw event/image milliseconds
- [ ] Flare/CME lists do not redundantly re-sort repository-ordered events
- [ ] CME Details remains available
- [ ] X-Ray and ACE EPAM chart engine behavior is unchanged
- [ ] Solar Activity stays at a fixed 72-hour chart domain
- [ ] Loading, empty, failure, retained-data, mixed partial-data, and pull-to-refresh behavior remain functional
- [ ] No nested vertical event scrolling is introduced
- [ ] `SolarActivityViewModel.kt`, repository/database/network/parser code, chart engine files, Home, Space Weather, and PWA are unchanged
- [ ] Focused Solar Activity unit tests pass
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:compileDebugAndroidTestKotlin` passes
- [ ] `./gradlew :app:lintDebug :app:assembleDebug` passes
- [ ] No unplanned files are modified
- [ ] Final Android CI is green
- [ ] Physical Pixel acceptance passes
