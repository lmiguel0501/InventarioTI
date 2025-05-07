package com.luismiguel.inventarioti.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var inventory by remember { mutableStateOf(listOf<InventoryItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var quantityToDelete by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)
    val displayName = account?.displayName ?: "Usuario"
    var showWelcome by remember { mutableStateOf(true) }
    val menuOptions = listOf("Inicio")

    val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.DEFAULT_SIGN_IN
    )

    LaunchedEffect(Unit) {
        delay(3000)
        showWelcome = false
    }

    fun upsertItem(item: InventoryItem) {
        inventory = inventory.toMutableList().apply {
            val index = indexOfFirst { it.serialNumber == item.serialNumber }
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
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Opciones", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    menuOptions.forEach { option ->
                        TextButton(onClick = {
                            println("Seleccionaste: $option")
                            scope.launch { drawerState.close() }
                        }) {
                            Text(text = option)
                        }
                        TextButton(onClick = {
                            isDarkMode = !isDarkMode
                        }) {
                            Text(text = if (isDarkMode) "Desactivar Modo Oscuro" else "Activar Modo Oscuro")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        scope.launch { drawerState.close() }
                    }) {
                        Text("Cerrar Sesión", color = Color.Red)
                    }
                }
            }
        }
    ) {
        MaterialTheme(
            colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Soporte Técnico")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = {
                        selectedItem = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(Color(0xFFEDE7F6), shape = RoundedCornerShape(16.dp))
                                    .padding(24.dp)
                            ) {
                                Text("¡Bienvenido al inventario!", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (!showWelcome) {
                        LazyColumn {
                            items(inventory) { item ->
                                InventoryItemCard(
                                    item = item,
                                    selected = item == selectedItem,
                                    onClick = {
                                        selectedItem = if (selectedItem == item) null else item
                                    },
                                    onEdit = {
                                        selectedItem = item
                                        showDialog = true
                                    },
                                    onDelete = {
                                        selectedItem = item
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
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
                    it?.let { inventory = inventory.filter { inv -> inv.serialNumber != it.serialNumber } }
                    showDialog = false
                }
            )
        }

        if (showDeleteDialog && selectedItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar Cantidad") },
                text = {
                    Column {
                        Text("Cantidad actual: ${selectedItem!!.quantity}")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = quantityToDelete.toString(),
                            onValueChange = {
                                quantityToDelete = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text("Cantidad a eliminar") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (quantityToDelete in 1..selectedItem!!.quantity) {
                            removeItemQuantity(selectedItem!!, quantityToDelete)
                        }
                        showDeleteDialog = false
                        quantityToDelete = 0
                    }) {
                        Text("Aceptar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        quantityToDelete = 0
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}





@Composable
fun InventoryItemCard(
    item: InventoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "N° de Serie: ${item.serialNumber}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Estado: ${item.status}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Cantidad: ${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (selected) {
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Eliminar")
                    }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Agregar Nuevo Artículo" else "Editar Artículo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Artículo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Número de Serie") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Estado") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(
                InventoryItem(
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
            if (item != null) {
                TextButton(onClick = { onDelete?.invoke(item) }) {
                    Text("Eliminar", color = Color.Red)
                }
            }
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
