package ca.stewark.helioflux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import ca.stewark.helioflux.ui.theme.HelioFluxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelioFluxTheme {
                Surface {
                    Text("HelioFlux")
                }
            }
        }
    }
}
