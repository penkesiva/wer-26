package com.golfcues.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.golfcues.app.ui.navigation.GolfCuesNavHost
import com.golfcues.app.ui.theme.GolfCuesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GolfCuesTheme {
                GolfCuesNavHost()
            }
        }
    }
}
