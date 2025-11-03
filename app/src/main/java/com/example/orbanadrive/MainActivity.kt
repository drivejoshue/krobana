package com.example.orbanadrive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.orbanadrive.ui.OrbanaApp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.example.orbanadrive.ui.AppNavHost
import com.example.orbanadrive.ui.theme.OrbanaDriveTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // muestra el splash lo antes posible
        super.onCreate(savedInstanceState)




        setContent { OrbanaApp() }
        }

}
