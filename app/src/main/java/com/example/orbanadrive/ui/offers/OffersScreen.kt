package com.example.orbanadrive.ui.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.navigation.Routes
import com.example.orbanadrive.network.OfferItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun OffersScreen(
    nav: NavController,
    tenantId: Long,
    driverId: Long
) {
    val ctx  = LocalContext.current
    val app  = LocalAppGraph.current
    val vm   = remember(app, tenantId, driverId) { OffersViewModel(app.driverRepo) }
    val offers by vm.offers.collectAsState()
    val queue  by vm.queue.collectAsState()
    val popup  by vm.popup.collectAsState()
    val available by vm.available.collectAsState()

    val scope = rememberCoroutineScope()

    // Cargar settings una vez
    LaunchedEffect(Unit) { vm.loadSettingsOnce() }

    // Primer “busy” con last-known para evitar 422 + refresh
    LaunchedEffect(Unit) {
        val last = com.example.orbanadrive.services.LastKnown.getLatLng(ctx)
        if (last != null) vm.setBusy(busy = true, lat = last.first, lng = last.second)
        vm.refreshAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ofertas") },
                actions = {
                    AvailabilityPillInline(
                        available = available,
                        onChange = { wantAvailable ->
                            scope.launch {
                                val last = com.example.orbanadrive.services.LastKnown.getLatLng(ctx)
                                val ok = vm.setBusy(busy = !wantAvailable, lat = last?.first, lng = last?.second)
                                if (ok && wantAvailable) {
                                    // opcional: asegurar service si lo necesitas
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Cola: ${queue.size}", modifier = Modifier.padding(12.dp))
            HorizontalDivider()

            LazyColumn(Modifier.fillMaxSize()) {
                items(offers) { o: OfferItem ->
                    ListItem(
                        headlineContent = { Text("#${o.ride_id} • ${o.offer_status}") },
                        supportingContent = {
                            Text(if (o.is_direct == 1) "Directa" else "Wave r${o.round_no ?: 0}")
                        },
                        modifier = Modifier
                            .clickable { nav.navigate(Routes.offerBid(o.offer_id)) }
                            .padding(vertical = 2.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Popup directo (cuando el server pida abrirlo)
    popup?.let { p ->
        OfferPopup(
            model    = p,
            onAccept = { bid -> scope.launch { vm.accept(p.offerId, bid) } },
            onReject = { scope.launch { vm.reject(p.offerId) } }
        )
    }
}

/* ===== Pill toggle reutilizable ===== */
@Composable
fun AvailabilityPillInline(
    available: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val track  = MaterialTheme.colorScheme.surfaceVariant
    val knobOn = MaterialTheme.colorScheme.primary
    val knobOff= MaterialTheme.colorScheme.errorContainer
    val fgOn   = MaterialTheme.colorScheme.onPrimary
    val fgOff  = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(track)
    ) {
        Text(
            "Ocupado",
            modifier = Modifier
                .clickable { onChange(false) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (!available) fgOn else fgOff,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Disponible",
            modifier = Modifier
                .clickable { onChange(true) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (available) fgOn else fgOff,
            fontWeight = FontWeight.SemiBold
        )
    }
}
