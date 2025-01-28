package com.luismiguel.inventarioti.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var inventory by remember { mutableStateOf(listOf<InventoryItem>()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val menuOptions = listOf("Inicio")

    // Función para agregar o actualizar un artículo
    fun upsertItem(item: InventoryItem) {
        inventory = inventory.toMutableList().apply {
            val index = indexOfFirst { it.serialNumber == item.serialNumber }
            if (index != -1) {
                set(index, item) // Actualiza el artículo existente
            } else {
                add(item) // Agrega nuevo si no existe
            }
        }
    }

    // Función para eliminar un artículo
    fun removeItem(item: InventoryItem) {
        inventory = inventory.filter { it != item }
    }

    fun logout() {
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Opciones",
                        style = MaterialTheme.typography.titleLarge
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
                        logout()
                        scope.launch { drawerState.close() }
                    }) {
                        Text(text = "Cerrar Sesión", color = Color.Red)
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
                            Text(
                                "Soporte Técnico",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
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
                    Text(
                        text = "¡Bienvenido al inventario!",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    LazyColumn {
                        items(inventory) { item ->
                            InventoryItemCard(
                                item = item,
                                selected = item == selectedItem,
                                onClick = { selectedItem = if (selectedItem == item) null else item },
                                onEdit = {
                                    selectedItem = item
                                    showDialog = true
                                },
                                onDelete = { removeItem(item) }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddItemDialog(
                item = selectedItem,
                onDismiss = { showDialog = false },
                onSave = { item ->
                    upsertItem(item)
                    showDialog = false
                },
                onDelete = {
                    if (it != null) removeItem(it)
                    showDialog = false
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
            if (selected) {
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
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
    var serialNumber by remember { mutableStateOf(item?.serialNumber ?: "") }
    var status by remember { mutableStateOf(item?.status ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Agregar Nuevo Artículo" else "Editar Artículo") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Artículo") },
                    modifier = Modifier.fillMaxWidth(),
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(InventoryItem(name, serialNumber, status)) }) {
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
