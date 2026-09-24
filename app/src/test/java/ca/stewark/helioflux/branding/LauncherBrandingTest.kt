package ca.stewark.helioflux.branding

import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBrandingTest {
    private val densitySizes = linkedMapOf(
        "ldpi" to 36,
        "mdpi" to 48,
        "hdpi" to 72,
        "xhdpi" to 96,
        "xxhdpi" to 144,
        "xxxhdpi" to 192,
    )

    private val suppliedAssetHashes = mapOf(
        "mipmap-hdpi/ic_launcher.png" to "9a801ab8a966ef7dfecc94f01404f6a84cac2d996e37806b621a198cc3431c07",
        "mipmap-hdpi/ic_launcher_foreground.png" to "8b8de00fe1699d8f3cb00f5549d77c7f0c87369487e22487781c08844ed3e15d",
        "mipmap-hdpi/ic_launcher_round.png" to "b810701c2a1beeb510088153d786c10efe4c40b4cdac5290f3ca0309237f881e",
        "mipmap-ldpi/ic_launcher.png" to "f9a508b4817d04dcb8f16ff4dc11686bbbed0410508495eb1f9fc6bd38ced6a5",
        "mipmap-ldpi/ic_launcher_foreground.png" to "5f7d5070f6993caeff541f2c3911c288dc2ecf4faeeb0bf41fbe6dd693f6b867",
        "mipmap-ldpi/ic_launcher_round.png" to "6fd0d4f5e84db8f3acfac6ba6045ad44d59f7a2b71fb8af9ee18fb5a442bd49c",
        "mipmap-mdpi/ic_launcher.png" to "0d9f0066c50145b022412b9a32a9fe58c4c9827edf508a8777b9749f7a9f2e8d",
        "mipmap-mdpi/ic_launcher_foreground.png" to "cc7daa5e6b21b4beddd1d6ff0446c9407439cb0a2df0d8deb2209a55358d42ef",
        "mipmap-mdpi/ic_launcher_round.png" to "b8bd04061fe8f9dc57881e85893b896f561cfe187b4b13ebf5d01ece96852963",
        "mipmap-xhdpi/ic_launcher.png" to "8925e9fc6719033dd1ec2230e41b34757fcaff3664408d167f5e42545ad743b3",
        "mipmap-xhdpi/ic_launcher_foreground.png" to "dcb3c264f027870a1d256fc37e5500c02b98cac3b301b47063ea70d8100e2ae7",
        "mipmap-xhdpi/ic_launcher_round.png" to "dd71e4476f1f7d1ce8fecbb9849a79d5eedffa2e57c19af2c3649af32d2735b1",
        "mipmap-xxhdpi/ic_launcher.png" to "f85006ce0e8c0b148cca35a0885da45f4d3a456187bfa3fdb3e3153c998a2f2d",
        "mipmap-xxhdpi/ic_launcher_foreground.png" to "3b2a7fbf7960af9605cb32501f30708a8a575c0d82c39b60331d5ff4d6b394ba",
        "mipmap-xxhdpi/ic_launcher_round.png" to "6af678c2094b9487ce88cfb2d2dd123fe03a917d1394b7e8b5e45813b818b528",
        "mipmap-xxxhdpi/ic_launcher.png" to "7bff14496c7dd4349bc136f9447684fd768afdce4c2b4486359ab9ff501db1bf",
        "mipmap-xxxhdpi/ic_launcher_foreground.png" to "4f2bfde8508d2184533728c29e9e97fb9d5b1e1082a2d509144b0d2ed09fdccf",
        "mipmap-xxxhdpi/ic_launcher_round.png" to "72eb331eae8d182f665f94d5d6fca1ed8b0706e728ac6173f0d99e79ebfa66e7",
    )

    @Test fun manifestUsesLauncherIconResources() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test fun legacyLauncherUsesExactSuppliedDensityPngs() {
        assertFalse(File("src/main/res/mipmap-anydpi/ic_launcher.xml").exists())
        assertFalse(File("src/main/res/mipmap-anydpi/ic_launcher_round.xml").exists())

        suppliedAssetHashes.forEach { (relativePath, expectedHash) ->
            val file = File("src/main/res/$relativePath")
            assertTrue("Missing supplied launcher asset ${file.path}", file.isFile)
            assertEquals("Unexpected bytes for ${file.path}", expectedHash, sha256(file))
        }

        densitySizes.forEach { (density, expectedSize) ->
            listOf("ic_launcher.png", "ic_launcher_foreground.png", "ic_launcher_round.png").forEach { name ->
                val image = ImageIO.read(File("src/main/res/mipmap-$density/$name"))
                assertEquals("Unexpected width for mipmap-$density/$name", expectedSize, image.width)
                assertEquals("Unexpected height for mipmap-$density/$name", expectedSize, image.height)
                assertTrue("Expected alpha channel for mipmap-$density/$name", image.colorModel.hasAlpha())
            }
        }
    }

    @Test fun adaptiveAndMonochromeLauncherResourcesUseSuppliedForeground() {
        listOf(
            "src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
            "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
        ).forEach { path ->
            val xml = File(path).readText()
            assertTrue(xml.contains("@color/launcher_background"))
            assertTrue(xml.contains("@mipmap/ic_launcher_foreground"))
            assertFalse(xml.contains("<monochrome"))
        }

        listOf(
            "src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
            "src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
        ).forEach { path ->
            val xml = File(path).readText()
            assertTrue(xml.contains("@color/launcher_background"))
            assertTrue(xml.contains("@mipmap/ic_launcher_foreground"))
            assertTrue(xml.contains("@mipmap/ic_launcher_monochrome"))
        }
    }

    @Test fun monochromeLauncherPreservesForegroundAlphaExactly() {
        densitySizes.forEach { (density, expectedSize) ->
            val foreground = ImageIO.read(File("src/main/res/mipmap-$density/ic_launcher_foreground.png"))
            val monochrome = ImageIO.read(File("src/main/res/mipmap-$density/ic_launcher_monochrome.png"))

            assertEquals(expectedSize, monochrome.width)
            assertEquals(expectedSize, monochrome.height)
            assertTrue(monochrome.colorModel.hasAlpha())

            for (y in 0 until expectedSize) {
                for (x in 0 until expectedSize) {
                    val foregroundAlpha = foreground.getRGB(x, y) ushr 24
                    val monochromeArgb = monochrome.getRGB(x, y)
                    assertEquals(
                        "Monochrome alpha must match the color foreground at mipmap-$density ($x,$y)",
                        foregroundAlpha,
                        monochromeArgb ushr 24,
                    )
                    if (foregroundAlpha > 0) {
                        assertEquals(
                            "Monochrome visible pixels must be white at mipmap-$density ($x,$y)",
                            0x00FFFFFF,
                            monochromeArgb and 0x00FFFFFF,
                        )
                    }
                }
            }
        }
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

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
