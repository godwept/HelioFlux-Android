# HelioFlux Adaptive Icon Rebuild Implementation Plan

**Date:** 2026-09-22
**Design doc:** `docs/specs/2026-09-22-helioflux-adaptive-icon-rebuild-design.md`
**Status:** Ready for review

## Overview

Replace the discarded hand-authored launcher foreground with the exact user-supplied transparent Clean Flow PNG, then rebuild the Android 13+ monochrome drawable as a deliberately simplified single-color mask with explicit negative-space channels. Preserve the existing manifest, adaptive/round resource wiring, black background, legacy fallback, and platform splash. The original mockup must be opened and inspected before asset work, and the built resources must be opened again in generated circle, squircle, rounded-square, and monochrome previews before acceptance.

This plan supersedes `docs/plans/2026-09-22-helioflux-flux-ribbon-launcher-branding.md`. Do not reuse path geometry from the current `ic_launcher_foreground.xml` or `ic_launcher_monochrome.xml`.

## Source Asset Baseline

- Source: `C:\Users\stewark2\Downloads\HelioFlux_CleanFlow_Color.png`
- Expected size: 432×432 ARGB PNG
- Expected SHA-256: `5DB22FB32AAF184FDA84C705BF7AD5EC2D7B196E0C516E32191F2AFF017D2CD8`
- Observed non-transparent bounds: approximately x=69–362, y=66–365
- Target bitmap resource: `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png`

## Tasks

### Task 1: Reinspect the authoritative mockup

**Files:** Read only: `C:\Users\stewark2\Downloads\HelioFlux_CleanFlow_Color.png`

**Test first:** Open the PNG itself at original resolution with the available image-viewing tool. Do not use the old XML or design prose as a substitute. Confirm visually that it contains:

- the pointed green/teal upper curl;
- the broad cyan middle/lower-left sweep;
- the blue/violet right flow ending in a downward point;
- three dark valleys that communicate overlap and flow.

Run this read-only integrity check:

```powershell
Get-FileHash -Algorithm SHA256 'C:\Users\stewark2\Downloads\HelioFlux_CleanFlow_Color.png'
```

**Implementation:** None. This is a mandatory visual baseline and must happen before editing any launcher resource. Record any mismatch from the expected hash or visible composition and stop rather than substituting another asset.

**Verify:** The image was actually opened, its composition matches the checklist, and its hash is `5DB22FB32AAF184FDA84C705BF7AD5EC2D7B196E0C516E32191F2AFF017D2CD8`.

---

### Task 2: Write the bitmap foreground regression contract

**Files:** `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingTest.kt`

**Test first:** Remove the old `launcherForegroundUsesCleanFlowBranding` color-string test. Add `import javax.imageio.ImageIO` and this replacement test:

```kotlin
@Test fun launcherForegroundUsesSuppliedTransparentPng() {
    val discardedVector = File("src/main/res/drawable/ic_launcher_foreground.xml")
    val foreground = File("src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png")

    assertFalse(discardedVector.exists())
    assertTrue(foreground.isFile)

    val image = ImageIO.read(foreground)
    assertEquals(432, image.width)
    assertEquals(432, image.height)
    assertTrue(image.colorModel.hasAlpha())

    var transparentPixels = 0
    var visiblePixels = 0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            if (image.getRGB(x, y) ushr 24 == 0) transparentPixels++ else visiblePixels++
        }
    }

    val pixelCount = image.width * image.height
    assertTrue(transparentPixels > pixelCount / 2)
    assertTrue(visiblePixels > pixelCount / 5)
}
```

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests ca.stewark.helioflux.branding.LauncherBrandingTest
```

The new test must fail because the PNG is absent and the discarded vector still exists.

**Implementation:** Test only; do not change resources in this task.

**Verify:** The focused test fails for the expected foreground-resource assertions, not because of a Kotlin compilation error. Gradle requires JDK 17; select/install it before treating an environment startup failure as a test result.

---

### Task 3: Install the exact color foreground

**Files:**

- Add: `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png`
- Delete: `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Test first:** Re-run the focused test from Task 2 and retain its failing result before changing resources.

**Implementation:** Create `app/src/main/res/drawable-xxxhdpi` and copy the supplied PNG byte-for-byte to `ic_launcher_foreground.png`. Remove the discarded vector XML so `@drawable/ic_launcher_foreground` resolves to the bitmap. Do not crop, recolor, flatten, stretch, sharpen, or otherwise transform the source at this stage.

Confirm copy integrity:

```powershell
Get-FileHash -Algorithm SHA256 `
  'C:\Users\stewark2\Downloads\HelioFlux_CleanFlow_Color.png', `
  'app\src\main\res\drawable-xxxhdpi\ic_launcher_foreground.png'
```

Both hashes must be identical.

**Verify:** Run the focused `LauncherBrandingTest`; `launcherForegroundUsesSuppliedTransparentPng` now passes. Open the repository copy with the image-viewing tool and compare it directly with the source, rather than assuming a successful file copy is sufficient visual review.

---

### Task 4: Write a failing monochrome topology contract

**Files:** `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingTest.kt`

**Test first:** Replace `monochromeLauncherUsesThreeCleanFlowRibbons` with the structural test below:

```kotlin
@Test fun monochromeLauncherUsesOneColorAndExplicitCutouts() {
    val monochrome = File("src/main/res/drawable/ic_launcher_monochrome.xml").readText()

    assertEquals(1, Regex("<path\\b").findAll(monochrome).count())
    assertEquals(1, Regex("android:fillColor=\"#FFFFFFFF\"").findAll(monochrome).count())
    assertTrue(monochrome.contains("android:fillType=\"evenOdd\""))
    assertFalse(monochrome.contains("<gradient"))
    assertFalse(monochrome.contains("fillAlpha"))

    val pathData = Regex("android:pathData=\"([^\"]+)\"")
        .find(monochrome)!!
        .groupValues[1]
    assertTrue(Regex("[Mm]").findAll(pathData).count() >= 4)
}
```

Add these imports:

```kotlin
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import ca.stewark.helioflux.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
```

Then add this second test class in the same file. The transparent probes correspond to the three dark valleys observed directly in the mockup; the opaque probes correspond to the visible colored lobes:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LauncherMonochromeRenderTest {
    @Test fun monochromeKeepsFlowChannelsOpenAtLauncherSize() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawable = context.getDrawable(R.drawable.ic_launcher_monochrome)!!
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 108, 108)
        drawable.draw(Canvas(bitmap))

        fun alphaAt(x: Int, y: Int) = Color.alpha(bitmap.getPixel(x, y))

        listOf(50 to 39, 40 to 60, 60 to 67).forEach { (x, y) ->
            assertEquals("Expected an open flow channel at ($x,$y)", 0, alphaAt(x, y))
        }
        listOf(54 to 20, 33 to 45, 54 to 54, 79 to 52, 33 to 73, 65 to 84)
            .forEach { (x, y) ->
                assertTrue("Expected ribbon artwork at ($x,$y)", alphaAt(x, y) > 0)
            }
    }
}
```

**Implementation:** Test only. Do not edit the monochrome vector yet.

**Verify:** Run both focused classes and confirm the current three-path approximation fails the new structural/topology contract:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests 'ca.stewark.helioflux.branding.Launcher*'
```

---

### Task 5: Trace the monochrome silhouette from the image

**Files:** `app/src/main/res/drawable/ic_launcher_monochrome.xml`

**Test first:** Keep the failures from Task 4 visible while tracing. Reopen the original PNG at full resolution before editing; do not consult the old path data as a shape reference.

**Implementation:** Replace the file completely with a 108×108 vector containing one white compound path with `android:fillType="evenOdd"`.

The compound path must contain:

1. An outer contour following the actual alpha silhouette, with major anchors near the upper point `(54,17)`, upper-left arc `(27,45)`, far-left arc `(18,61)`, lower-left sweep `(31,76)`, downward point `(62,91)`, far-right arc `(91,58)`, and return around the upper curl.
2. An upper/central crescent cutout around `(50,39)`, following the dark valley beneath the green curl.
3. A left/middle crescent cutout around `(40,60)`, following the dark valley between the two cyan sweeps.
4. A lower/right crescent cutout around `(60,67)`, following the dark valley inside the blue downward curl.

Use smooth cubic curves and preserve at least a small visible transparent channel at 108 px. Use no gradients, partial alpha, black fill, strokes, enclosing disk, or geometry copied from the discarded XML.

**Verify:** Run the two focused launcher test classes. Both the structural test and every rendered alpha probe must pass.

---

### Task 6: Add reproducible adaptive-mask previews

**Files:** Add `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingPreviewTest.kt`

**Test first:** Add a Robolectric test configured for SDK 33 that renders `R.drawable.ic_launcher_foreground` on black at 432×432 through three `android.graphics.Path` clipping masks:

- `circle`: a circle centered at `(216,216)` with radius `216`;
- `rounded-square`: `RectF(0,0,432,432)` with x/y radii `96`;
- `squircle`: a symmetric cubic path through `(216,0)`, `(432,216)`, `(216,432)`, and `(0,216)`, using control points about 65 px from each corner.

Write the three PNGs to:

```kotlin
val outputDir = File(System.getProperty("java.io.tmpdir"), "helioflux-launcher-preview")
```

Name them `color-circle.png`, `color-rounded-square.png`, and `color-squircle.png`. In the same test, render `R.drawable.ic_launcher_monochrome` in white on black at both 432×432 and 108×108 as `monochrome-432.png` and `monochrome-108.png`. Use `Bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)` and assert that all five files exist and have non-zero length.

**Implementation:** This test is verification tooling only; it must not modify production resources. Print `outputDir.absolutePath` so the preview location is visible in test output.

**Verify:** Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests ca.stewark.helioflux.branding.LauncherBrandingPreviewTest --info
```

Confirm all five preview files were produced in the reported temporary directory.

---

### Task 7: Perform the mandatory direct visual comparison

**Files:**

- Source: `C:\Users\stewark2\Downloads\HelioFlux_CleanFlow_Color.png`
- Resource: `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png`
- Generated previews: `%TEMP%\helioflux-launcher-preview\*.png`
- Modify only if comparison fails: `app/src/main/res/drawable/ic_launcher_monochrome.xml`

**Test first:** Open the source, all three color mask previews, and both monochrome previews with the image-viewing tool. Merely checking dimensions, XML, or test results does not complete this task.

**Implementation:** Check the following visually:

- Color: same silhouette, crossing order, gradients, glow, center, and apparent scale as the supplied mockup.
- Masks: no important pointed tip or lobe is clipped under circle, squircle, or rounded square.
- Monochrome: three flows remain distinguishable at 108 px; the three valleys are visibly open; the result is not a solid blob.

If color clipping occurs, stop and revise the plan before transforming the authoritative bitmap. If only monochrome readability fails, adjust the three cutout curves minimally, rerun the topology tests, regenerate previews, and reopen them.

**Verify:** Record that the actual images—not just documentation—were inspected. All five previews satisfy the checklist, and focused launcher tests remain green.

---

### Task 8: Strengthen launcher wiring coverage

**Files:** `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingTest.kt`

**Test first:** Expand `adaptiveAndMonochromeLauncherResourcesExist` so it checks both normal and round variants for each API tier:

```kotlin
@Test fun adaptiveAndMonochromeLauncherResourcesUseExpectedLayers() {
    listOf(
        "src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
        "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    ).forEach { path ->
        val xml = File(path).readText()
        assertTrue(xml.contains("@color/launcher_background"))
        assertTrue(xml.contains("@drawable/ic_launcher_foreground"))
        assertFalse(xml.contains("<monochrome"))
    }

    listOf(
        "src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
        "src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
    ).forEach { path ->
        val xml = File(path).readText()
        assertTrue(xml.contains("@color/launcher_background"))
        assertTrue(xml.contains("@drawable/ic_launcher_foreground"))
        assertTrue(xml.contains("@drawable/ic_launcher_monochrome"))
    }

    listOf(
        "src/main/res/mipmap-anydpi/ic_launcher.xml",
        "src/main/res/mipmap-anydpi/ic_launcher_round.xml",
    ).forEach { path ->
        val xml = File(path).readText()
        assertTrue(xml.contains("@color/launcher_background"))
        assertTrue(xml.contains("@drawable/ic_launcher_foreground"))
    }
}
```

**Implementation:** Test only. Keep the existing manifest, black-background, and Android 12+ splash tests. Do not change launcher XML unless this regression test reveals an actual wiring defect.

**Verify:** Run all tests in `LauncherBrandingTest` and `LauncherMonochromeRenderTest`; they pass.

---

### Task 9: Compile all launcher resource variants

**Files:** No planned changes

**Test first:** Run the focused launcher tests once more.

**Implementation:** Compile the resource variants and debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Do not “fix” unrelated warnings or refactor application code.

**Verify:** Android resource linking succeeds for API 24 legacy layer lists, API 26 adaptive icons, API 33 monochrome icons, and the Android 12+ splash references.

---

### Task 10: Run unit and instrumentation compilation checks

**Files:** No planned changes

**Test first / Verify:** Run the CI-equivalent test and instrumentation-compilation commands separately:

```powershell
.\gradlew.bat projects
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
```

All commands must pass. If a connected Android device is available, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

**Implementation:** No production changes unless a failure is directly caused by the planned launcher resources or tests.

---

### Task 11: Run lint and final build

**Files:** No planned changes

**Test first / Verify:** Run:

```powershell
.\gradlew.bat lintDebug assembleDebug
```

Review the lint report for launcher-resource issues. The command must pass without suppressing new findings.

**Implementation:** Only correct findings caused by this icon rebuild.

---

### Task 12: Review scope and physical-device acceptance

**Files:** Review only

**Test first:** Run `git status --short` and `git diff --stat`, then inspect the complete diff. Planned tracked changes are limited to:

- `app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png`
- deletion of `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingTest.kt`
- `app/src/test/java/ca/stewark/helioflux/branding/LauncherBrandingPreviewTest.kt`
- this design and plan documentation

**Implementation:** Install the debug APK on the Pixel when available. Check the normal icon, round/masked presentation, themed icon, and cold-launch system splash. Confirm the themed icon retains its internal flow channels and the system splash remains pure black with no added delay.

**Verify:** No unrelated tracked files changed. Automated checks are green, direct preview inspection is recorded, and physical-device aesthetic acceptance is either complete or explicitly listed as the only remaining manual step.

## Definition of Done

- [ ] All tasks completed in order
- [ ] The original mockup was opened and inspected before asset edits
- [ ] The repository color foreground is the supplied transparent PNG
- [ ] The monochrome icon remains detailed and recognizable at launcher size
- [ ] Circle, squircle, rounded-square, and monochrome previews were opened and compared directly
- [ ] Focused and full tests pass
- [ ] Android resource compilation, instrumentation-test compilation, lint, and debug build pass
- [ ] No unplanned tracked files were modified
- [ ] Physical-device verification is complete or clearly identified as pending
- [ ] The feature behaves as described in the approved design document
