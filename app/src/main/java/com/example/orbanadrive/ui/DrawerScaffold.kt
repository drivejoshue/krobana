// ui/DrawerScaffold.kt
package com.example.orbanadrive.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.orbanadrive.LocalAppGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverAppWithDrawer(
    title: String? = null,
    onLogout: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Header con avatar + datos del driver
                DrawerHeader()

                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // Acción: Salir
                NavigationDrawerItem(
                    label = { Text("Salir") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onLogout() // <- lo ejecuta, sin saber de nav/app
                        }
                    },
                    icon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    }
                )
            }
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner)) {
                content()
            }
        }
    }
}

/* ---------------- Header del Drawer ---------------- */

@Composable
private fun DrawerHeader() {
    val app = LocalAppGraph.current

    var name   by remember { mutableStateOf("Conductor") }
    var phone  by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("offline") } // idle | busy | offline | abierto

    LaunchedEffect(Unit) {
        runCatching { app.driverRepo.me() }
            .onSuccess { me ->
                name   = me.driver?.name ?: me.user?.name ?: "Conductor"
                phone  = me.driver?.phone
                status = me.driver?.status ?: (me.current_shift?.status ?: "offline")
            }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (!phone.isNullOrBlank()) {
                    Text(
                        phone!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                StatusPill(status = status)
            }
        }
    }
}

@Composable
private fun StatusPill(status: String?) {
    val normalized = (status ?: "offline").lowercase()
    val (label, dot) = when (normalized) {
        "idle", "online", "abierto" -> "Online"  to MaterialTheme.colorScheme.primary
        "busy", "ocupado"           -> "Busy"    to MaterialTheme.colorScheme.tertiary
        else                        -> "Offline" to Color(0xFF9AA3B2)
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(10.dp))
            Canvas(Modifier.size(8.dp)) { drawCircle(color = dot) }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(10.dp))
        }
    }
}
