// ui/LogoutHelpers.kt
package com.example.orbanadrive.ui

import android.content.Context
import androidx.navigation.NavHostController
import com.example.orbanadrive.AppGraph
import com.example.orbanadrive.navigation.Routes
import com.example.orbanadrive.services.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

import kotlinx.coroutines.withContext

suspend fun performLogout(app: AppGraph, nav: NavHostController, ctx: Context) {
    withContext(Dispatchers.IO) {
        runCatching { LocationService.stop(ctx) }
        runCatching { app.authRepo.logout() }
    }
    withContext(Dispatchers.Main) {
        nav.navigate(Routes.Login) {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
            restoreState = false
        }
    }
}
