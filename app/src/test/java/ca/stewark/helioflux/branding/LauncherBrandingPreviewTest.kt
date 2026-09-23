package ca.stewark.helioflux.branding

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.PathParser
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherBrandingPreviewTest {
    @Test fun writesLauncherPreviewsForVisualReview() {
        val outputDir = File(System.getProperty("java.io.tmpdir"), "helioflux-launcher-preview")
        assertTrue(outputDir.mkdirs() || outputDir.isDirectory)
        val foreground = BitmapFactory.decodeFile(
            File("src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png").absolutePath,
        )
        val monochromeXml = File("src/main/res/drawable/ic_launcher_monochrome.xml").readText()
        val pathData = Regex("android:pathData=\"([^\"]+)\"")
            .find(monochromeXml)!!
            .groupValues[1]
        val monochromePath = PathParser.createPathFromPathData(pathData).apply {
            fillType = Path.FillType.EVEN_ODD
        }
        val monochromePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val size = 432
        val masks = mapOf(
            "circle" to Path().apply {
                addCircle(216f, 216f, 216f, Path.Direction.CW)
            },
            "rounded-square" to Path().apply {
                addRoundRect(RectF(0f, 0f, 432f, 432f), 96f, 96f, Path.Direction.CW)
            },
            "squircle" to Path().apply {
                moveTo(216f, 0f)
                cubicTo(367f, 0f, 432f, 65f, 432f, 216f)
                cubicTo(432f, 367f, 367f, 432f, 216f, 432f)
                cubicTo(65f, 432f, 0f, 367f, 0f, 216f)
                cubicTo(0f, 65f, 65f, 0f, 216f, 0f)
                close()
            },
        )

        masks.forEach { (name, mask) ->
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.clipPath(mask)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(foreground, null, Rect(0, 0, size, size), null)
            writePng(bitmap, File(outputDir, "color-$name.png"))
        }

        listOf(432, 108).forEach { previewSize ->
            val bitmap = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC)
            canvas.save()
            canvas.scale(previewSize / 108f, previewSize / 108f)
            canvas.drawPath(monochromePath, monochromePaint)
            canvas.restore()
            writePng(bitmap, File(outputDir, "monochrome-$previewSize.png"))
        }

        val expectedFiles = masks.keys.map { "color-$it.png" } +
            listOf("monochrome-432.png", "monochrome-108.png")
        expectedFiles.forEach { name ->
            val preview = File(outputDir, name)
            assertTrue("Missing preview ${preview.absolutePath}", preview.isFile)
            assertTrue("Empty preview ${preview.absolutePath}", preview.length() > 0)
        }
        println("Launcher previews: ${outputDir.absolutePath}")
    }

    private fun writePng(bitmap: Bitmap, output: File) {
        FileOutputStream(output).use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
