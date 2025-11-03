package com.example.orbanadrive.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.navigation.Routes
import com.example.orbanadrive.ui.offers.OffersScreen
import com.example.orbanadrive.ui.vehicle.VehicleSelectScreen
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.orbanadrive.ui.offers.OfferBidScreen


@Composable
fun AppNavHost() {
    val nav   = rememberNavController()
    val app   = LocalAppGraph.current
    val scope = rememberCoroutineScope()
    val ctx   = LocalContext.current

    data class StartPayload(val start: String, val tenantId: Long, val driverId: Long)

    val payload by produceState<StartPayload?>(initialValue = null, app) {
        val token = app.tokenStore.getToken()
        if (token.isNullOrBlank()) {
            value = StartPayload(Routes.Login, 0L, 0L)
            return@produceState
        }

        val me = runCatching { app.driverRepo.me() }.getOrNull()

        val tenantId = (
                (me?.user?.tenant_id as? Number)?.toLong()
                    ?: 1L
                )

        val driverId = (
                (me?.driver?.id as? Number)?.toLong()
                    ?: (me?.currentShift?.let { it as? Map<*,*> }?.get("driver_id") as? Number)?.toLong()
                    ?: 0L
                )

        val start = if (me?.currentShift != null) Routes.Offers else Routes.Vehicle
        value = StartPayload(start, tenantId, driverId)
    }
    if (payload == null) return

    NavHost(navController = nav, startDestination = payload!!.start) {
        composable(Routes.Login)   { LoginScreen(nav) }

        composable(Routes.Vehicle) {
            DriverAppWithDrawer(
                title = "Seleccionar vehículo",
                onLogout = { scope.launch { performLogout(app, nav, ctx) } }
            ) { VehicleSelectScreen(nav) }
        }

        composable(Routes.Offers) {
            DriverAppWithDrawer(
                title = "Ofertas",
                onLogout = { scope.launch { performLogout(app, nav, ctx) } }
            ) {
                OffersScreen(
                    nav      = nav,
                    tenantId = payload!!.tenantId,
                    driverId = payload!!.driverId
                )
            }
        }

        composable(
            route = Routes.OfferBid,
            arguments = listOf(navArgument("offerId"){ type = NavType.LongType })
        ) { backStackEntry ->
            DriverAppWithDrawer(
                title = "Oferta",
                onLogout = { scope.launch { performLogout(app, nav, ctx) } }
            ) {
                val offerId = backStackEntry.arguments!!.getLong("offerId")
                OfferBidScreen(
                    nav     = nav,
                    offerId = offerId
                )
            }
        }
    }
}
