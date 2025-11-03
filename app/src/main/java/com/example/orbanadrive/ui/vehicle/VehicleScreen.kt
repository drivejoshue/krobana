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

    // Datos del driver
    var me          by remember { mutableStateOf<MeRes?>(null) }
    var driverName  by remember { mutableStateOf<String?>(null) }
    var driverPhone by remember { mutableStateOf<String?>(null) }
    var shiftOpen   by remember { mutableStateOf(false) }

    // Vehículos
    var vehicles   by remember { mutableStateOf<List<VehicleItem>>(emptyList()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }

    // ===== Helpers permisos =====
    // ----- helpers de permisos -----
    fun hasLocPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // ===== Helpers permisos =====

    // ----- PRIMERO declara la función -----
    suspend fun startShiftAndGo() {
        try {
            loading = true
            val vid = selectedId
            if (vid == null) {
                error = "Selecciona un vehículo."
                Log.e("Orbana.Vehicle", "startShiftAndGo() sin vehículo seleccionado")
                return
            }
            Log.d("Orbana.Vehicle", "startShift() → vehicleId=$vid")
            val shiftId = app.driverRepo.startShift(vid)
            Log.d("Orbana.Vehicle", "startShift() OK shiftId=$shiftId")

            if (shiftId != null) {
                val token  = app.tokenStore.getToken()
                val tenant = app.tokenStore.getTenantId()?.toLongOrNull()
                Log.d("Orbana.Vehicle", "LocationService.start(tokenNull=${token.isNullOrBlank()}, tenant=$tenant)")

                if (!token.isNullOrBlank()) {
                    if (hasLocPermission()) {
                        LocationService.start(ctx, token, tenant)
                    } else {
                        Log.e("Orbana.Vehicle", "Sin permisos tras startShift(); no inicio FGS")
                        error = "Permisos de ubicación no concedidos."
                        return
                    }
                } else {
                    Log.e("Orbana.Vehicle", "NO TOKEN al iniciar LocationService")
                }

                Log.d("Orbana.Vehicle", "navigate -> ${Routes.Offers}")
                nav.navigate(Routes.Offers) {
                    popUpTo(Routes.Vehicle) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                error = "No se pudo abrir el turno."
                Log.e("Orbana.Vehicle", "startShift() devolvió null")
            }
        } catch (e: Exception) {
            error = e.message ?: "Error abriendo turno"
            Log.e("Orbana.Vehicle", "Excepción startShiftAndGo()", e)
        } finally { loading = false }
    }

// ----- LUEGO el launcher de permisos -----
    val requestPermsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        val fine = res[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fine || coarse
        Log.d("Orbana.Vehicle", "Permisos resultado → fine=$fine coarse=$coarse granted=$granted")
        if (granted) {
            scope.launch { startShiftAndGo() } // <-- Ahora SÍ existe
        } else {
            error = "Se requieren permisos de ubicación para iniciar turno."
        }
    }

    LaunchedEffect(shiftOpen) {
        if (shiftOpen) {
            android.util.Log.d("Orbana.Vehicle", "Shift YA abierto → navegar a Offers")
            // Arranca LocationService si no corre (revisa permisos antes en tu flujo)
            val token  = app.tokenStore.getToken()
            val tenant = app.tokenStore.getTenantId()?.toLongOrNull()
            if (!token.isNullOrBlank() && !com.example.orbanadrive.services.LocationService.isRunning) {
                com.example.orbanadrive.services.LocationService.start(ctx, token, tenant)
            }

            nav.navigate(Routes.Offers) {
                popUpTo(Routes.Vehicle) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // ===== Carga inicial =====
    LaunchedEffect(Unit) {
        try {
            val m = app.driverRepo.me()
            me = m
            driverName  = m.driver?.name ?: m.user.name
            driverPhone = m.driver?.phone
            shiftOpen   = m.current_shift != null

            val list = app.driverRepo.getVehicles()
            vehicles = list
            if (list.size == 1) selectedId = list.first().id
        } catch (e: Exception) {
            error = e.message ?: "Error cargando datos"
        } finally { loading = false }
    }

    val canStart = selectedId != null && !loading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Seleccionar vehículo") }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header con avatar / datos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar_default),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RectangleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            driverName ?: "Conductor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            driverPhone ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (shiftOpen) {
                            AssistChip(onClick = { }, label = { Text("Shift Open") })
                        } else {
                            AssistChip(onClick = { }, label = { Text("Offline") })
                        }
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            // Lista de vehículos
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
                                Text(
                                    "${v.brand ?: ""} ${v.model ?: ""} · $plate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            RadioButton(
                                selected = (selectedId == v.id),
                                onClick  = { selectedId = v.id }
                            )
                        }
                    }
                }
            }

            // Botón Iniciar turno (con permisos + logs)
            Button(
                onClick = {
                    if (!canStart) return@Button
                    if (!hasLocPermission()) {
                        Log.d(TAG, "No hay permisos → solicitando...")
                        requestPermsLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@Button
                    }
                    Log.d(TAG, "Permisos OK → iniciar flujo de turno")
                    scope.launch { startShiftAndGo() }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Iniciar turno")
            }
        }
    }
}

/* ---------- Helpers de Modifier (FORMA CORRECTA) ---------- */
private val Modifier.cardPadding: Modifier
    get() = this.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
