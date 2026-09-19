package ca.stewark.helioflux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.stewark.helioflux.ui.HelioFluxApp
import ca.stewark.helioflux.ui.theme.HelioFluxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelioFluxTheme {
                HelioFluxApp()
            }
        }
    }
}
