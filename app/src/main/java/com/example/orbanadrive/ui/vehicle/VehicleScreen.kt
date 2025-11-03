@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.orbanadrive.ui.vehicle

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.R
import com.example.orbanadrive.navigation.Routes
import com.example.orbanadrive.network.MeRes
import com.example.orbanadrive.network.VehicleItem
import com.example.orbanadrive.services.LocationService
import kotlinx.coroutines.launch

@Composable
fun VehicleSelectScreen(nav: NavHostController) {
    val TAG = "Orbana.Vehicle"
    val app   = LocalAppGraph.current
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }

    var me          by remember { mutableStateOf<MeRes?>(null) }
    var driverName  by remember { mutableStateOf<String?>(null) }
    var driverPhone by remember { mutableStateOf<String?>(null) }
    var shiftOpen   by remember { mutableStateOf(false) }

    var vehicles   by remember { mutableStateOf<List<VehicleItem>>(emptyList()) }
    var selectedId: Int? by rememberSaveable { mutableStateOf<Int?>(null) }

    fun hasLocPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun startShiftAndGo() {
        try {
            loading = true
            val vid = selectedId ?: run {
                error = "Selecciona un vehículo."
                return
            }
            val shiftId = app.driverRepo.startShift(vid.toLong())
            if (shiftId != null) {
                val token  = app.tokenStore.getToken()
                val tenant = app.tokenStore.getTenantId()?.toLongOrNull()
                if (!token.isNullOrBlank() && hasLocPermission()) {
                    LocationService.start(ctx, token, tenant)
                }
                nav.navigate(Routes.Offers) {
                    popUpTo(Routes.Vehicle) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                error = "No se pudo abrir el turno."
            }
        } catch (e: Exception) {
            error = e.message ?: "Error abriendo turno"
            Log.e(TAG, "startShiftAndGo()", e)
        } finally { loading = false }
    }

    val requestPermsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val granted = (res[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (res[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) scope.launch { startShiftAndGo() }
        else error = "Se requieren permisos de ubicación para iniciar turno."
    }

    // --- Carga inicial y NAVEGACIÓN inmediata si ya hay turno ---
    var navDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val m = app.driverRepo.me()
            me = m
            driverName  = m.driver?.name ?: m.user?.name
            driverPhone = m.driver?.phone
            shiftOpen   = m.currentShift != null    // <- ya mapea bien con @Json/@SerializedName

            val list = app.driverRepo.getVehicles()
            vehicles = list
            if (list.size == 1) selectedId = list.first().id

            if (shiftOpen && !navDone) {
                // Opcional: arranca FGS si puedes
                val token  = app.tokenStore.getToken()
                val tenant = app.tokenStore.getTenantId()?.toLongOrNull()
                if (!token.isNullOrBlank() && hasLocPermission() && !LocationService.isRunning) {
                    LocationService.start(ctx, token, tenant)
                }
                navDone = true
                nav.navigate(Routes.Offers) {
                    popUpTo(Routes.Vehicle) { inclusive = true }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
        } catch (e: Exception) {
            error = e.message ?: "Error cargando datos"
        } finally { loading = false }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Seleccionar vehículo") }) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.default_user),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RectangleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(driverName ?: "Conductor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(driverPhone ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                        AssistChip(onClick = {}, label = { Text(if (shiftOpen) "Shift Open" else "Offline") })
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(vehicles) { v ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedId = v.id },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            Modifier
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(v.economico ?: "Vehículo ${v.id}", style = MaterialTheme.typography.titleMedium)
                                val plate = v.plate?.takeIf { it.isNotBlank() } ?: "—"
                                Text("${v.brand ?: ""} ${v.model ?: ""} · $plate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                            RadioButton(
                                selected = (selectedId == v.id),
                                onClick  = { selectedId = v.id }
                            )
                        }
                    }
                }
            }

            val canStart = selectedId != null && !loading

            Button(
                onClick = {
                    if (!canStart) return@Button
                    if (!hasLocPermission()) {
                        requestPermsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@Button
                    }
                    scope.launch { startShiftAndGo() }
                },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Iniciar turno") }
        }
    }
}
