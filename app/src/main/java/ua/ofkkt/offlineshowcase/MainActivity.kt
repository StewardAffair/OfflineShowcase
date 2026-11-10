package ua.ofkkt.offlineshowcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ua.ofkkt.offlineshowcase.ui.AppRoot
import ua.ofkkt.offlineshowcase.ui.OfflineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineTheme { AppRoot() }
        }
    }
}
