package ca.stewark.helioflux.branding

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

class LauncherBrandingTest {
    @Test fun manifestUsesLauncherIconResources() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherMonochromeRenderTest {
    @Test fun monochromeKeepsFlowChannelsOpenAtLauncherSize() {
        val monochrome = File("src/main/res/drawable/ic_launcher_monochrome.xml").readText()
        val pathData = Regex("android:pathData=\"([^\"]+)\"")
            .find(monochrome)!!
            .groupValues[1]
        val path = PathParser.createPathFromPathData(pathData).apply {
            fillType = Path.FillType.EVEN_ODD
        }
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })

        fun alphaAt(x: Int, y: Int) = Color.alpha(bitmap.getPixel(x, y))

        listOf(50 to 39, 40 to 60, 60 to 65).forEach { (x, y) ->
            assertEquals("Expected an open flow channel at ($x,$y)", 0, alphaAt(x, y))
        }
        listOf(54 to 20, 33 to 45, 54 to 54, 79 to 52, 33 to 73, 65 to 84)
            .forEach { (x, y) ->
                assertTrue("Expected ribbon artwork at ($x,$y)", alphaAt(x, y) > 0)
            }
    }
}
