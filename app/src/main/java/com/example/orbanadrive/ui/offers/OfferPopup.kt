package com.example.orbanadrive.ui.offers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OfferPopup(
    model: OfferPopupModel,
    onAccept: (Double?) -> Unit,
    onReject: () -> Unit
) {
    var bidText by remember { mutableStateOf(model.quotedAmount?.toString() ?: "") }

    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
        Card(Modifier.fillMaxWidth().padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(if (model.direct) "Oferta directa" else "Solicitud de viaje",
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (model.showBidding) {
                    OutlinedTextField(
                        value = bidText,
                        onValueChange = { bidText = it },
                        label = { Text("Tu oferta") },
                        prefix = { Text("$") },
                        singleLine = true
                    )
                } else {
                    Text("Tarifa: $${model.quotedAmount ?: 0.0}",
                        style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onReject) { Text("Rechazar") }
                    Button(onClick = {
                        val bid = bidText.toDoubleOrNull()
                        onAccept(if (model.showBidding) bid else null)
                    }) { Text("Aceptar") }
                }
            }
        }
    }
}
