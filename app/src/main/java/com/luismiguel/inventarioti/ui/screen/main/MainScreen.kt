package com.luismiguel.inventarioti.ui.screen.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.luismiguel.inventarioti.data.InventoryItem
import com.luismiguel.inventarioti.ui.screen.comprobantes.ComprobantesScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

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
    val menuOptions = listOf("Inicio")

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

    ModalNavigationDrawer(
        drawerState = drawerState, drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                val buttonWidth = 250.dp

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opciones", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        TextButton(
                            onClick = {
                                currentScreen = MainRoutes.HOME
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Black,
                                containerColor = Color.Transparent,
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text("Inicio")
                        }
                    }
                    Card(
                        modifier = Modifier
                            .width(buttonWidth)
                            .padding(vertical = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        TextButton(
                            onClick = {
                                currentScreen = MainRoutes.COMPROBANTES
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Black,
                                containerColor = Color.Transparent,
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text("Comprobantes de entrega")
                        }
                    }

                    Card(
                        modifier = Modifier
                            .width(buttonWidth)
                            .padding(vertical = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        TextButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Black,
                                containerColor = Color.Transparent,
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text(
                                if (isDarkMode) "Desactivar Modo Oscuro" else "Activar Modo Oscuro",
                                color = Color.Black
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        TextButton(
                            onClick = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Black,
                                containerColor = Color.Transparent,
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text("Cerrar Sesión", color = Color.Red)
                        }
                    }
                }
            }

        }) {
        Scaffold(containerColor = Color.White, topBar = {
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
                                .background(Color.White, shape = RoundedCornerShape(12.dp))
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier.shadow(4.dp)
                )
                Divider(thickness = 1.dp, color = Color.Black.copy(alpha = 0.2f))
            }
        }, floatingActionButton = {
            if (currentScreen == MainRoutes.HOME) {
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .background(Color.White, shape = RoundedCornerShape(16.dp))
                ) {
                    IconButton(
                        onClick = {
                            selectedItem = null
                            showDialog = true
                        }, modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar",
                            tint = Color.Black
                        )
                    }
                }
            }
        }) { padding ->
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
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = BorderStroke(1.dp, Color(0x22000000))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "¡Bienvenido al inventario!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
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
                                                inventory.filter { it.serialNumber != it.serialNumber }
                                        })
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

    if (showDialog) {
        AddItemDialog(item = selectedItem, onDismiss = { showDialog = false }, onSave = {
            upsertItem(it)
            showDialog = false
        }, onDelete = {
            it?.let {
                inventory = inventory.filter { inv -> inv.serialNumber != it.serialNumber }
            }
            showDialog = false
        })
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
            containerColor = Color.White,
            title = { Text("Eliminar Cantidad", color = Color.Black) },
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Aceptar", color = Color(0xFF6200EE))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF6200EE))
                }
            })
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
            })
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
            containerColor = Color.White,
            title = { Text("Confirmar eliminación", color = Color.Black) },
            text = { Text("¿Estás seguro que deseas eliminar este artículo?") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Sí", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color.Red)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0x22000000))) {
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
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Eliminar Cantidad")
                    }
                    TextButton(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Eliminar artículo")
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
    var showDeleteConfirmation by remember { mutableStateOf(false) } // 👈 NUEVO estado

    val textFieldModifier = Modifier
        .fillMaxWidth()
        .shadow(8.dp, shape = RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)

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
                    Text("Sí", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                }) {
                    Text("No")
                }
            })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                if (item == null) "Agregar Nuevo Artículo" else "Editar Artículo",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Artículo") },
                    modifier = textFieldModifier,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Número de Serie") },
                    modifier = textFieldModifier,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Estado") },
                    modifier = textFieldModifier,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    modifier = textFieldModifier,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
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
                        Text("Eliminar", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color.Black)
                }
            }
        })
}