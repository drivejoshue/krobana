package com.example.orbanadrive.ui.offers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.network.OfferDetails
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferBidScreen(
    nav: NavController,
    offerId: Long
) {
    val app   = LocalAppGraph.current
    val repo  = app.driverRepo
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }
    var ui      by remember { mutableStateOf<OfferDetails?>(null) }

    // Bidding
    var biddingOn by remember { mutableStateOf(false) }
    var bidValue  by remember { mutableStateOf(0.0) }
    var bidMin    by remember { mutableStateOf(0.0) }
    var bidMax    by remember { mutableStateOf(0.0) }

    LaunchedEffect(offerId) {
        loading = true; error = null
        runCatching { repo.getOffer(offerId) }
            .onSuccess { d ->

            }
            .onFailure { error = it.message }
        loading = false
    }

    fun doAccept() {
        scope.launch {
            loading = true; error = null
            val bidToSend = if (biddingOn) round(bidValue * 100) / 100.0 else null
            val ok = runCatching { repo.accept(offerId, bidToSend) }
                .onFailure { error = it.message }
                .isSuccess
            loading = false
            if (ok) nav.popBackStack()
        }
    }
    fun doReject() {
        scope.launch {
            loading = true; error = null
            val ok = runCatching { repo.reject(offerId) }
                .onFailure { error = it.message }
                .isSuccess
            loading = false
            if (ok) nav.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oferta #$offerId") },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) { Text("Atrás") }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = ::doReject, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Text("Rechazar")
                }
                Button(onClick = ::doAccept, enabled = !loading, modifier = Modifier.weight(1f)) {
                    Text("Aceptar")
                }
            }
        }
    ){ pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

            ui?.let { d ->
                Text("#${d.ride_id} · ${if (d.is_direct==1) "Directa" else "Wave r${d.round_no ?: 0}"}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Text("Origen: ${d.origin_label ?: "—"}")
                Text("Destino: ${d.dest_label ?: "—"}")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Dist: ${fmtKm(d.ride_distance_m)}")
                    Text("ETA: ${fmtMin(d.ride_duration_s)}")
                }

                Spacer(Modifier.height(6.dp))
                if (biddingOn) {
                    Text("Ofrece tu tarifa", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = bidValue.toFloat(),
                        onValueChange = { bidValue = it.toDouble() },
                        valueRange = bidMin.toFloat()..bidMax.toFloat(),
                        steps = 10
                    )
                    Text("MXN ${"%.2f".format(bidValue)}  (min ${"%.0f".format(bidMin)} · max ${"%.0f".format(bidMax)})")
                } else {
                    Text("Tarifa sugerida", fontWeight = FontWeight.SemiBold)
                    val q = d.total_amount ?: d.quoted_amount
                    Text(if (q != null) "MXN ${"%.2f".format(q)}" else "—")
                }
            }
        }
    }
}

private fun fmtKm(m: Int?): String = if (m==null) "—" else "%.1f km".format(m/1000.0)
private fun fmtMin(s: Int?): String = if (s==null) "—" else "%d min".format(ceil((s)/60.0).toInt())
