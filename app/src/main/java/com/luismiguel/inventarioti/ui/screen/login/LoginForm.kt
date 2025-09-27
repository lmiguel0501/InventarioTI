package com.luismiguel.inventarioti.ui.screen.login

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.luismiguel.inventarioti.R
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private val BANNER_HEIGHT = 240.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginForm(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val signInClient = remember { GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN) }

    val signInLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }

    // ====== Onda expansiva lenta con degradado (con salvaguardas) ======
    var rootSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var waveCenter by remember { mutableStateOf(Offset.Unspecified) }
    val waveProgress = remember { Animatable(0f) }     // 0..1
    val overlayAlpha = remember { Animatable(0f) }     // 0..1
    var isWaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Botón (mismo estilo)
    val themeButtonContainer: Color = MaterialTheme.colorScheme.background
    val themeButtonContent: Color = MaterialTheme.colorScheme.onBackground
    val themeButtonBorder: Color =
        if (isDarkMode) Color(0xFF1F67D2).copy(alpha = 0.85f) else Color(0xFF000000).copy(alpha = 0.30f)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { rootSize = it.toSize() }
        ) {

            // ---------- CONTENIDO ----------
            Column(modifier = Modifier.fillMaxSize()) {

                // 1) Banner con degradado inferior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BANNER_HEIGHT)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.inventario),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }

                // 2) Botón de tema
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            val hasSize = rootSize.width > 0f && rootSize.height > 0f
                            val hasCenter = waveCenter.isSpecified
                            if (!hasSize || !hasCenter) {
                                onToggleDarkMode()
                                return@Button
                            }

                            isWaving = true
                            scope.launch {
                                waveProgress.snapTo(0.001f)
                                overlayAlpha.snapTo(1f)
                                waveProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                                )

                                onToggleDarkMode()

                                overlayAlpha.animateTo(0f, tween(500, easing = LinearEasing))
                                isWaving = false
                                waveProgress.snapTo(0f)
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp, pressedElevation = 0.dp,
                            focusedElevation = 0.dp, hoveredElevation = 0.dp, disabledElevation = 0.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeButtonContainer,
                            contentColor = themeButtonContent
                        ),
                        border = BorderStroke(1.25.dp, themeButtonBorder),
                        modifier = Modifier.onGloballyPositioned { coords ->
                            waveCenter = coords.boundsInRoot().center
                        }
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = "Cambiar tema",
                            tint = themeButtonContent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isDarkMode) "Modo oscuro" else "Modo claro", color = themeButtonContent)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "¡Bienvenido al inventario!",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Por favor, inicia sesión con tu cuenta de Google.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val signInIntent = signInClient.signInIntent
                                    signInLauncher.launch(signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Continuar con Google")
                            }
                        }
                    }
                }

                // 4) Invitado
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("is_guest", true).apply()
                            Toast.makeText(context, "Ingresando como invitado", Toast.LENGTH_SHORT).show()
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    ) {
                        Text(
                            text = "Iniciar sesión como invitado",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------- CAPA DE ONDA (lenta + degradado, con radio seguro) ----------
            if (isWaving) {
                val maxR = remember(rootSize) {
                    hypot(rootSize.width.toDouble(), rootSize.height.toDouble()).toFloat()
                }
                val currentR = max(maxR * waveProgress.value, 1f) // radio mínimo 1f

                val core = if (!isDarkMode) Color.Black else Color.White
                val brush = Brush.radialGradient(
                    colors = listOf(
                        core.copy(alpha = 0.75f),
                        core.copy(alpha = 0.30f),
                        core.copy(alpha = 0.08f),
                        core.copy(alpha = 0.0f)
                    ),
                    center = if (waveCenter.isSpecified) waveCenter else Offset(rootSize.width / 2f, rootSize.height / 2f),
                    radius = currentR
                )

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    drawRect(brush = brush, alpha = overlayAlpha.value)
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        isError = isError,
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors()
    )
}
