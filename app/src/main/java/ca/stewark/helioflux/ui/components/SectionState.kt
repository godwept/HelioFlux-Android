package ca.stewark.helioflux.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

sealed interface SectionState {
    data object Loading : SectionState
    data class Error(val message: String) : SectionState
    data class Empty(val message: String = "No data available") : SectionState
    data object Content : SectionState
}

@Composable
fun SectionStateContent(
    state: SectionState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when (state) {
            SectionState.Loading ->
                CircularProgressIndicator(modifier = Modifier.testTag("section-loading"))
            is SectionState.Error ->
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("section-error"),
                )
            is SectionState.Empty ->
                Text(
                    text = state.message,
                    modifier = Modifier.testTag("section-empty"),
                )
            SectionState.Content -> content()
        }
    }
}
