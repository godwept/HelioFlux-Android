package ca.stewark.helioflux.branding

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBrandingTest {
    @Test fun manifestUsesLauncherIconResources() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test fun adaptiveAndMonochromeLauncherResourcesExist() {
        assertTrue(File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml").isFile)
        assertTrue(File("src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml").isFile)
        assertTrue(File("src/main/res/mipmap-anydpi-v33/ic_launcher.xml").readText().contains("<monochrome"))
        assertTrue(File("src/main/res/drawable/ic_launcher_monochrome.xml").isFile)
    }

    @Test fun launcherForegroundUsesCleanFlowBranding() {
        val foreground = File("src/main/res/drawable/ic_launcher_foreground.xml").readText()

        assertTrue(foreground.contains("#5CFF6B"))
        assertTrue(foreground.contains("#00E5FF"))
        assertTrue(foreground.contains("#326BFF"))
        assertTrue(foreground.contains("#8A45FF"))
        assertFalse(foreground.contains("#061B43"))
        assertFalse(foreground.contains("A46,46"))
    }

    @Test fun monochromeLauncherUsesThreeCleanFlowRibbons() {
        val monochrome = File("src/main/res/drawable/ic_launcher_monochrome.xml").readText()

        assertEquals(3, Regex("<path\\b").findAll(monochrome).count())
        assertFalse(monochrome.contains("A42,42"))
        assertFalse(monochrome.contains("A46,46"))
    }

    @Test fun launcherBackgroundIsPureBlack() {
        val colors = File("src/main/res/values/colors.xml").readText()
        assertTrue(colors.contains("<color name=\"launcher_background\">#000000</color>"))
    }

    @Test fun android12SplashUsesBlackBackgroundAndLauncherIcon() {
        val style = File("src/main/res/values-v31/styles.xml").readText()
        assertTrue(style.contains("<item name=\"android:windowSplashScreenBackground\">#000000</item>"))
        assertTrue(style.contains("<item name=\"android:windowSplashScreenAnimatedIcon\">@mipmap/ic_launcher</item>"))
    }
}
