package ca.stewark.helioflux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SectionHeadingFontSize = 20.sp
internal val SectionHeadingFontWeight = FontWeight.SemiBold
internal val SectionHeadingLetterSpacing = 0.25.sp
internal val SectionHeadingTopSpacing = 20.dp
internal val SectionHeadingBottomSpacing = 8.dp
internal val SectionHeadingAccentWidth = 3.dp
internal val SectionHeadingAccentHeight = 20.dp

@Composable
fun HelioFluxSectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    topSpacing: Dp = SectionHeadingTopSpacing,
) {
    Row(
        modifier = modifier.padding(
            top = topSpacing,
            bottom = SectionHeadingBottomSpacing,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(SectionHeadingAccentWidth)
                .height(SectionHeadingAccentHeight)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                ),
        )
        Text(
            text = text,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = SectionHeadingFontSize,
                    fontWeight = SectionHeadingFontWeight,
                    letterSpacing = SectionHeadingLetterSpacing,
                ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
