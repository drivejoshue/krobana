package com.example.orbanadrive.ui
import android.util.Log

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.orbanadrive.LocalAppGraph
import com.example.orbanadrive.R
import com.example.orbanadrive.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(nav: NavHostController) {
    val app = LocalAppGraph.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // ===== State =====
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var showPass by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = email.isNotBlank() && pass.isNotBlank() && !loading

    // ===== Prefill + AutoLogin =====
    LaunchedEffect(Unit) {
        // precarga “recordarme” y email guardado
        rememberMe = app.tokenStore.getRemember()
        app.tokenStore.getSavedEmail()?.let { email = it }

        // si hay token y remember = true → entra directo
        val token = app.tokenStore.getToken()
        if (rememberMe && !token.isNullOrBlank()) {
            nav.navigate(Routes.Vehicle) {
                popUpTo(Routes.Login) { inclusive = true }
            }
        }
    }

    // ===== Toast de error =====
    LaunchedEffect(error) {
        error?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    // ===== Doble BACK para salir =====
    var backPressedOnce by remember { mutableStateOf(false) }
    BackHandler {
        val activity = ctx as? Activity ?: return@BackHandler
        if (backPressedOnce) {
            activity.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(ctx, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2000)
                backPressedOnce = false
            }
        }
    }

    // ===== UI =====
    Scaffold { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(padding)
        ) {
            // Header con logo y títulos
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo), // tu PNG/SVG importado
                    contentDescription = "Orbana",
                    modifier = Modifier.size(96.dp)
                )
                Text("Driver", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Inicia sesión para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // “Bottom sheet” visual
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Email, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (canSubmit) {
                                    scope.launch {
                                        doLogin(
                                            app, email.trim(), pass,
                                            rememberMe,
                                            { loading = it }, { error = it }, nav
                                        )
                                    }
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ==== Recordarme ====
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Recordarme (auto-login)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = {
                            if (!canSubmit) return@Button
                            scope.launch {
                                doLogin(
                                    app, email.trim(), pass,
                                    rememberMe,
                                    { loading = it }, { error = it }, nav
                                )
                            }
                        },
                        enabled = canSubmit,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text("Ingresar")
                        }
                    }

                    TextButton(onClick = { /* TODO: recuperar contraseña */ }) {
                        Text("¿Olvidaste tu contraseña?")
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private suspend fun doLogin(
    app: com.example.orbanadrive.AppGraph,
    email: String,
    pass: String,
    rememberMe: Boolean,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    nav: NavHostController
) {
    setLoading(true)
    setError(null)
    try {
        val ok = app.authRepo.login(email, pass)
        if (ok) {
            // persistencia según “Recordarme”
            app.tokenStore.setRemember(rememberMe)
            if (rememberMe) {
                app.tokenStore.setSavedEmail(email)
            } else {
                app.tokenStore.setSavedEmail(null)
            }
            nav.navigate(Routes.Vehicle) {
                popUpTo(Routes.Login) { inclusive = true }
            }
        } else {
            setError("Credenciales inválidas.")
        }
    } catch (e: Exception) {
        setError(e.message ?: "Error de red.")
    } finally {
        setLoading(false)
    }
}
