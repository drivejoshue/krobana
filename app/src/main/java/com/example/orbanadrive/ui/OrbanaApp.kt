package com.example.orbanadrive.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.orbanadrive.AppGraph
import com.example.orbanadrive.LocalAppGraph

@Composable
fun OrbanaApp() {

    val appCtx = LocalContext.current.applicationContext
    val graph  = remember { AppGraph(appCtx) }

    CompositionLocalProvider(LocalAppGraph provides graph) {
        // <-- agrega el tema aquí
        com.example.orbanadrive.ui.theme.OrbanaDriveTheme(darkTheme = true) {
            AppNavHost()
        }
    }
}
