package ca.stewark.helioflux.branding

import java.io.File
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

    @Test fun android12SplashUsesBlackBackgroundAndLauncherIcon() {
        val style = File("src/main/res/values-v31/styles.xml").readText()
        assertTrue(style.contains("<item name=\"android:windowSplashScreenBackground\">#000000</item>"))
        assertTrue(style.contains("<item name=\"android:windowSplashScreenAnimatedIcon\">@mipmap/ic_launcher</item>"))
    }
}
