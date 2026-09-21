package ca.stewark.helioflux.ui.components

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelioFluxSectionHeadingTest {
    @Test
    fun headingUsesApprovedTypographyAndSpacing() {
        assertEquals(20.sp, SectionHeadingFontSize)
        assertEquals(FontWeight.SemiBold, SectionHeadingFontWeight)
        assertEquals(0.25.sp, SectionHeadingLetterSpacing)
        assertEquals(20.dp, SectionHeadingTopSpacing)
        assertEquals(8.dp, SectionHeadingBottomSpacing)
        assertEquals(3.dp, SectionHeadingAccentWidth)
        assertEquals(20.dp, SectionHeadingAccentHeight)
    }

    @Test
    fun headingUsesThemeSolarAccentWithoutDecorativeEffects() {
        val source = File(
            "src/main/java/ca/stewark/helioflux/ui/components/HelioFluxSectionHeading.kt",
        ).readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.primary"))
        assertTrue(source.contains("MaterialTheme.colorScheme.onBackground"))
        assertTrue(source.contains("RoundedCornerShape"))
    }
}
