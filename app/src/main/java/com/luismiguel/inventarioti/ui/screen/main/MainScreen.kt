package com.luismiguel.inventarioti.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.luismiguel.inventarioti.data.InventoryItem
import com.luismiguel.inventarioti.ui.screen.comprobantes.ComprobantesScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max

enum class MainRoutes {
    HOME, COMPROBANTES
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var inventory by remember { mutableStateOf(listOf<InventoryItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quantityToDelete by remember { mutableStateOf(0) }
    var currentScreen by remember { mutableStateOf(MainRoutes.HOME) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)
    val displayName = account?.displayName ?: "Usuario"
    var showWelcome by remember { mutableStateOf(true) }

    val googleSignInClient = GoogleSignIn.getClient(
        context, GoogleSignInOptions.DEFAULT_SIGN_IN
    )

    LaunchedEffect(Unit) {
        delay(2000)
        showWelcome = false
    }

    fun upsertItem(item: InventoryItem) {
        inventory = inventory.toMutableList().apply {
            val index = indexOfFirst { it.id == item.id }
            if (index != -1) set(index, item) else add(item)
        }
    }

    fun removeItemQuantity(item: InventoryItem, amount: Int) {
        val current = inventory.find { it.serialNumber == item.serialNumber }
        if (current != null) {
            val remaining = current.quantity - amount
            if (remaining > 0) {
                upsertItem(current.copy(quantity = remaining))
            } else {
                inventory = inventory.filter { it.serialNumber != item.serialNumber }
            }
        }
    }

    var rootSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var waveCenter by remember { mutableStateOf(Offset.Unspecified) }
    val waveProgress = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(0f) }
    var isWaving by remember { mutableStateOf(false) }

    // Contenedor raíz para dibujar la onda
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it.toSize() }
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    val buttonWidth = 250.dp

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Opciones", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .width(buttonWidth)
                                .padding(vertical = 4.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            TextButton(
                                onClick = {
                                    currentScreen = MainRoutes.HOME
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Inicio") }
                        }
                        Card(
                            modifier = Modifier
                                .width(buttonWidth)
                                .padding(vertical = 4.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            TextButton(
                                onClick = {
                                    currentScreen = MainRoutes.COMPROBANTES
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Comprobantes de entrega") }
                        }

                        Card(
                            modifier = Modifier
                                .width(buttonWidth)
                                .padding(vertical = 4.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            TextButton(
                                onClick = {
                                    val hasSize = rootSize.width > 0f && rootSize.height > 0f
                                    val hasCenter = waveCenter.isSpecified
                                    if (!hasSize || !hasCenter) {
                                        onToggleDarkMode()
                                        return@TextButton
                                    }

                                    isWaving = true
                                    scope.launch {
                                        waveProgress.snapTo(0.001f)
                                        overlayAlpha.snapTo(1f)
                                        waveProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 900, easing = LinearEasing)
                                        )
                                        onToggleDarkMode()
                                        overlayAlpha.animateTo(0f, tween(500, easing = LinearEasing))
                                        isWaving = false
                                        waveProgress.snapTo(0f)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords ->
                                        waveCenter = coords.boundsInRoot().center
                                    },
                            ) {
                                Text(
                                    if (isDarkMode) "Desactivar Modo Oscuro" else "Activar Modo Oscuro"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Card(
                            modifier = Modifier
                                .width(buttonWidth)
                                .padding(vertical = 4.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            TextButton(
                                onClick = {
                                    GoogleSignIn.getClient(
                                        context, GoogleSignInOptions.DEFAULT_SIGN_IN
                                    ).signOut().addOnCompleteListener {
                                        FirebaseAuth.getInstance().signOut()
                                        navController.navigate("login") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            },
            content = {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (currentScreen) {
                                                MainRoutes.HOME -> "Soporte Técnico"
                                                MainRoutes.COMPROBANTES -> "Comprobantes de Entrega"
                                            }
                                        )
                                    }
                                },
                                navigationIcon = {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .shadow(4.dp, RoundedCornerShape(12.dp))
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                    ) {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Menú",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.shadow(4.dp)
                            )
                            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == MainRoutes.HOME) {
                            Box(
                                modifier = Modifier
                                    .shadow(8.dp, RoundedCornerShape(16.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedItem = null
                                        showDialog = true
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Agregar",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        AnimatedVisibility(
                            visible = showWelcome,
                            enter = fadeIn(animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(500))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "¡Bienvenido al inventario!",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        when (currentScreen) {
                            MainRoutes.HOME -> {
                                if (!showWelcome) {
                                    LazyColumn {
                                        items(inventory) { item ->
                                            InventoryItemCard(
                                                item = item,
                                                selected = item == selectedItem,
                                                onClick = {
                                                    selectedItem =
                                                        if (selectedItem == item) null else item
                                                },
                                                onEdit = {
                                                    selectedItem = item
                                                    showDialog = true
                                                },
                                                onDelete = {
                                                    selectedItem = item
                                                    showDeleteDialog = true
                                                },
                                                onDeleteItem = {
                                                    inventory =
                                                        inventory.filter { inv -> inv.serialNumber != it.serialNumber }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            MainRoutes.COMPROBANTES -> {
                                ComprobantesScreen()
                            }
                        }
                    }
                }
            }
        )

        if (isWaving) {
            val maxR = remember(rootSize) {
                hypot(rootSize.width.toDouble(), rootSize.height.toDouble()).toFloat()
            }
            val currentR = max(maxR * waveProgress.value, 1f)
            val core = if (!isDarkMode) Color.Black else Color.White
            val brush = Brush.radialGradient(
                colors = listOf(
                    core.copy(alpha = 0.75f),
                    core.copy(alpha = 0.30f),
                    core.copy(alpha = 0.08f),
                    core.copy(alpha = 0.0f)
                ),
                center = if (waveCenter.isSpecified) waveCenter
                else Offset(rootSize.width / 2f, rootSize.height / 2f),
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

    if (showDialog) {
        AddItemDialog(
            item = selectedItem,
            onDismiss = { showDialog = false },
            onSave = {
                upsertItem(it)
                showDialog = false
            },
            onDelete = {
                it?.let { toDelete ->
                    inventory = inventory.filter { inv -> inv.serialNumber != toDelete.serialNumber }
                }
                showDialog = false
            }
        )
    }

    @Composable
    fun StyledDeleteQuantityDialog(
        selectedItem: InventoryItem,
        quantityToDelete: Int,
        onValueChange: (Int) -> Unit,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Eliminar Cantidad") },
            text = {
                Column {
                    Text("Cantidad actual: ${selectedItem.quantity}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantityToDelete.toString(),
                        onValueChange = {
                            onValueChange(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0)
                        },
                        label = { Text("Cantidad a eliminar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Aceptar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (showDeleteDialog && selectedItem != null) {
        StyledDeleteQuantityDialog(
            selectedItem = selectedItem!!,
            quantityToDelete = quantityToDelete,
            onValueChange = { quantityToDelete = it },
            onConfirm = {
                if (quantityToDelete in 1..selectedItem!!.quantity) {
                    removeItemQuantity(selectedItem!!, quantityToDelete)
                }
                showDeleteDialog = false
                quantityToDelete = 0
            },
            onDismiss = {
                showDeleteDialog = false
                quantityToDelete = 0
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleteItem: (InventoryItem) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    @Composable
    fun StyledConfirmDeleteDialog(
        onConfirm: () -> Unit, onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro que deseas eliminar este artículo?") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Sí", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "N° de Serie: ${item.serialNumber}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = "Estado: ${item.status}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Cantidad: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)

            if (selected) {
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    TextButton(
                        onClick = onDelete
                    ) {
                        Text("Eliminar Cantidad", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = { showDialog = true }
                    ) {
                        Text("Eliminar artículo", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (showDialog) {
                    StyledConfirmDeleteDialog(onConfirm = {
                        showDialog = false
                        onDeleteItem(item)
                    }, onDismiss = {
                        showDialog = false
                    })
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    item: InventoryItem? = null,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onDelete: ((InventoryItem?) -> Unit)? = null
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var serialNumber by remember { mutableStateOf(item?.serialNumber?.toString() ?: "") }
    var status by remember { mutableStateOf(item?.status ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Estás seguro que deseas eliminar este artículo?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete?.invoke(item)
                }) {
                    Text("Sí", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("No")
                }
            })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                if (item == null) "Agregar Nuevo Artículo" else "Editar Artículo",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Artículo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Número de Serie") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Estado") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    InventoryItem(
                        id = item?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        serialNumber = serialNumber.toIntOrNull() ?: 0,
                        status = status,
                        quantity = quantity.toIntOrNull() ?: 0
                    )
                )
            }) {
                Text(if (item == null) "Agregar" else "Actualizar")
            }
        },
        dismissButton = {
            Row {
                if (item != null) {
                    TextButton(onClick = {
                        showDeleteConfirmation = true
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        })
}
