package com.example.orbanadrive.ui.offers

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.R
import com.example.orbanadrive.navigation.Routes
import com.example.orbanadrive.network.OfferItem
import com.example.orbanadrive.repo.DriverRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OffersScreen(
    nav: NavController,
    tenantId: Long,
    driverId: Long
) {
    val app   = LocalAppGraph.current
    val repo  = app.driverRepo as DriverRepository
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---------- Estado UI ----------
    var isAvailable by remember { mutableStateOf(false) }
    var firstLoad   by remember { mutableStateOf(true) }
    var errorText   by remember { mutableStateOf<String?>(null) }

    // Permisos ubicación
    val locGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Seed de disponibilidad UNA SOLA VEZ
    LaunchedEffect(Unit) {
        val me = runCatching { repo.me() }.getOrNull()
        val st = (me?.driver?.status ?: me?.currentShift?.status)?.lowercase() ?: "offline"
        isAvailable = (st == "idle" || st == "available")
        Timber.d("seed status=$st avail=$isAvailable tenant=$tenantId driver=$driverId")
    }

    // Inicia poleo en el repo (loop IO) — NO recompones cada 3s
    LaunchedEffect(Unit) {
        repo.startOffersPolling()
    }

    // Ubicación cada 10s (no fuerza recomposición)
    LaunchedEffect(isAvailable, locGranted) {
        if (!locGranted) return@LaunchedEffect
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        while (true) {
            runCatching {
                val loc: Location? = fused.awaitLastLocation()
                if (loc != null) {
                    val speedKmh = (loc.speed * 3.6).takeIf { v -> v.isFinite() }
                    repo.sendLocation(loc.latitude, loc.longitude, busy = !isAvailable, speedKmh = speedKmh)
                }
            }
            delay(10_000)
        }
    }

    // Ofertas desde el flow del repo (solo recompones cuando cambian)
    val offers by repo.offersFlow.collectAsState()

    fun manualRefresh() {
        scope.launch {
            runCatching {
                // Llamada directa única para forzar refresh inmediato
                val now = repo.getFreshOffers()
                // El repo actualizará el flow en el próximo tick; no hacemos set local aquí
            }.onFailure { errorText = it.message }
        }
    }

    // ---------- UI ----------

    // Barra superior compacta con botón refresh
    Surface(tonalElevation = 2.dp, shadowElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logonf),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text("Solicitudes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { manualRefresh() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {

        // Switch full-width rojo/verde
        AvailabilityTabsFullWidth(
            available = isAvailable,
            onChange = { wantAvail ->
                val prev = isAvailable
                isAvailable = wantAvail   // optimista
                scope.launch {
                    val ok = runCatching { repo.setBusy(!wantAvail) }.getOrDefault(false)
                    if (!ok) isAvailable = prev
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Lista
        Box(Modifier.fillMaxSize()) {
            if (offers.isEmpty()) {
                // Mensaje contextual
                val title = if (isAvailable) "Sin ofertas por ahora" else "Estás ocupado"
                val sub   = if (isAvailable) "Cuando llegue una ola, aparecerá aquí."
                else "Pon “Disponible” para entrar en olas automáticas."
                EmptyState(title, sub)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(offers, key = { it.offer_id }) { item ->
                        OfferFancyRow(
                            item = item,
                            onClick = { nav.navigate(Routes.offerBid(item.offer_id)) }
                        )
                    }
                }
            }

            errorText?.let {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(onClick = { errorText = null }, label = { Text(it.take(80)) })
                }
            }
        }
    }
}

/* ====== Switch full-width (rojo/verde) ====== */

@Composable
private fun AvailabilityTabsFullWidth(
    available: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val offBg = if (!available) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
        val onBg  = if (available)  MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        val offFg = if (!available) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
        val onFg  = if (available)  MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(offBg)
                .clickable { onChange(false) },
            contentAlignment = Alignment.Center
        ) { Text("Ocupado", color = offFg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.width(8.dp))

        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(onBg)
                .clickable { onChange(true) },
            contentAlignment = Alignment.Center
        ) { Text("Disponible", color = onFg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
    }
}

/* ====== Tarjeta oferta (igual a la tuya) ====== */

@Composable
private fun OfferFancyRow(item: OfferItem, onClick: () -> Unit) {
    val bg by animateColorAsState(MaterialTheme.colorScheme.surface)
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("∼ ${fmtKm(item.distance_m)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(money(item.quoted_amount),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
                Spacer(Modifier.width(10.dp))
                SuggestPill("Precio justo")
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.default_user),
                    contentDescription = "Pasajero",
                    modifier = Modifier.size(42.dp).clip(CircleShape),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.passenger_name ?: "Pasajero",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("★ 4.7", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                        Text("(213)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(fmtAgo(item.sent_at), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            item.origin_label?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("Viaje")
                Tag("📏 ${fmtKm(item.ride_distance_m)}")
                Tag("⏱ ${fmtMin(item.ride_duration_s)}")
                item.pax?.let { Tag("👥 $it") }
                item.requested_channel?.let { Tag("Canal: $it") }
            }

            item.dest_label?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            item.notes?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/* ====== Chips / Empty / Helpers ====== */

@Composable
private fun SuggestPill(text: String) {
    val bg = MaterialTheme.colorScheme.secondaryContainer
    val fg = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelLarge) }
}

@Composable private fun Tag(text: String) {
    AssistChip(onClick = {}, label = { Text(text, style = MaterialTheme.typography.labelSmall) })
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun money(v: Number?): String =
    v?.let { "MXN " + "%.0f".format(it.toDouble()) } ?: "MXN —"
private fun fmtKm(m: Int?): String = if (m == null) "— km" else "%.1f km".format(m / 1000.0)
private fun fmtMin(s: Int?): String = if (s == null) "— min" else "%d min".format(ceil(s / 60.0).toInt())

private fun fmtAgo(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return runCatching {
        val thenMs = parseServerTsMs(iso)
        val nowMs  = System.currentTimeMillis()
        val diffS  = ((nowMs - thenMs) / 1000.0).coerceAtLeast(0.0)
        when {
            diffS < 60   -> "${diffS.roundToInt()} seg"
            diffS < 3600 -> "${(diffS/60).roundToInt()} min"
            else         -> "${(diffS/3600).roundToInt()} h"
        }
    }.getOrElse { "—" }
}
private fun parseServerTsMs(s: String): Long {
    runCatching {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return f.parse(s)?.time ?: 0L
    }
    runCatching {
        val f2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return f2.parse(s)?.time ?: 0L
    }
    return 0L
}

@SuppressLint("MissingPermission")
private suspend fun com.google.android.gms.location.FusedLocationProviderClient.awaitLastLocation(): Location? =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val task = lastLocation
        task.addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
        task.addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
        task.addOnCanceledListener { if (cont.isActive) cont.cancel() }
    }
