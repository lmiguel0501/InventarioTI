package com.luismiguel.inventarioti.ui.screen.comprobantes

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.*
import com.google.gson.Gson
import com.luismiguel.inventarioti.data.Comprobante
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComprobantesScreen() {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var comprobantes by remember { mutableStateOf(loadComprobantes(context)) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showDialog by remember { mutableStateOf(false) }
    var nombreComprobante by remember { mutableStateOf("") }
    var selectedComprobante by remember { mutableStateOf<Comprobante?>(null) }
    var showOptionsBottomSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var uriParaVer by remember { mutableStateOf<Uri?>(null) }
    var estaEditando by remember { mutableStateOf(false) }
    var showEditChoiceDialog by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempImageUri != null && nombreComprobante.isNotBlank()) {
                val savedUri = saveImageToInternalStorage(context, tempImageUri!!)
                if (savedUri != null) {
                    val fecha = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                    val hora = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val comprobante =
                        Comprobante(savedUri.toString(), nombreComprobante, fecha, hora)

                    if (estaEditando && selectedComprobante != null) {
                        val list = loadComprobantes(context).toMutableList()
                        list.remove(selectedComprobante)
                        list.add(comprobante)
                        saveComprobantesList(context, list)
                        estaEditando = false
                    } else {
                        saveComprobante(context, comprobante)
                    }

                    comprobantes = loadComprobantes(context)
                    Toast.makeText(context, "Guardado exitosamente", Toast.LENGTH_SHORT).show()
                }
                nombreComprobante = ""
                selectedComprobante = null
            }
        }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uriParaVer != null) {
            AlertDialog(
                onDismissRequest = { uriParaVer = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White,
                title = { Text("Vista previa", color = Color.Black) },
                text = {
                    Image(
                        painter = rememberAsyncImagePainter(uriParaVer),
                        contentDescription = "Vista previa",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { uriParaVer = null }) {
                        Text("Cerrar", color = Color.Red)
                    }
                }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            if (!isSearching) {
                Button(
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            showDialog = true
                            estaEditando = false
                            nombreComprobante = ""
                        } else {
                            Toast.makeText(
                                context,
                                "Permiso de cámara denegado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, Color(0x22000000)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Tomar la imagen", color = Color.Black)
                }
            } else {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 48.dp),
                    placeholder = { Text("Buscar comprobante...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
            IconButton(
                onClick = { isSearching = !isSearching },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Buscar"
                )
            }
        }

        val filteredComprobantes =
            if (searchQuery.isBlank()) comprobantes else comprobantes.filter {
                it.nombre.contains(searchQuery, ignoreCase = true)
            }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredComprobantes) { comp ->
                ComprobanteCard(
                    comprobante = comp,
                    selected = comp == selectedComprobante,
                    onClick = {
                        selectedComprobante = if (selectedComprobante == comp) null else comp
                    },
                    onEdit = {
                        selectedComprobante = comp
                        showEditChoiceDialog = true
                    },
                    onDelete = {
                        val updated = comprobantes.toMutableList().apply { remove(comp) }
                        saveComprobantesList(context, updated)
                        comprobantes = updated
                        selectedComprobante = null
                    },
                    onView = { uriParaVer = it }
                )
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("Nombre del comprobante", color = Color.Black) },
            text = {
                TextField(
                    value = nombreComprobante,
                    onValueChange = { nombreComprobante = it },
                    placeholder = { Text("Escribe el nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false

                    if (estaEditando && selectedComprobante != null) {
                        val updatedList = loadComprobantes(context).toMutableList()
                        updatedList.remove(selectedComprobante)
                        val uriToUse = selectedComprobante!!.uri
                        val fecha = selectedComprobante!!.fecha
                        val hora = selectedComprobante!!.hora
                        val nuevo = Comprobante(uriToUse, nombreComprobante, fecha, hora)
                        updatedList.add(nuevo)
                        saveComprobantesList(context, updatedList)
                        comprobantes = updatedList
                        Toast.makeText(context, "Comprobante actualizado", Toast.LENGTH_SHORT)
                            .show()
                        selectedComprobante = null
                        estaEditando = false
                        nombreComprobante = ""
                    } else {
                        val uri = createTempUri(context)
                        tempImageUri = uri
                        launcher.launch(uri)
                    }
                }) {
                    Text("Aceptar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    selectedComprobante = null
                    estaEditando = false
                }) {
                    Text("Cancelar", color = Color.Red)
                }
            }
        )
    }
    if (showOptionsBottomSheet && selectedComprobante != null) {
        ModalBottomSheet(onDismissRequest = { showOptionsBottomSheet = false }) {
            ListItem(
                headlineContent = { Text("Editar comprobante") },
                modifier = Modifier.clickable {
                    showOptionsBottomSheet = false
                    showDialog = true
                }
            )
            ListItem(
                headlineContent = { Text("Eliminar comprobante") },
                modifier = Modifier.clickable {
                    showOptionsBottomSheet = false
                    showDeleteConfirm = true
                }
            )
        }
    }
    if (showDeleteConfirm && selectedComprobante != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Estás seguro que deseas eliminar?") },
            confirmButton = {
                TextButton(onClick = {
                    val list = loadComprobantes(context).toMutableList()
                    list.remove(selectedComprobante)
                    saveComprobantesList(context, list)
                    comprobantes = list
                    showDeleteConfirm = false
                    selectedComprobante = null
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("No")
                }
            }
        )
    }
    if (showEditChoiceDialog && selectedComprobante != null) {
        AlertDialog(
            onDismissRequest = { showEditChoiceDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = { Text("¿Qué deseas modificar?", color = Color.Black) },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        nombreComprobante = selectedComprobante!!.nombre
                        estaEditando = true
                        showDialog = true
                        showEditChoiceDialog = false
                    }) {
                        Text("Modificar nombre", color = Color.Black)
                    }
                    TextButton(onClick = {
                        nombreComprobante = selectedComprobante!!.nombre
                        val uri = createTempUri(context)
                        tempImageUri = uri
                        estaEditando = true
                        showEditChoiceDialog = false
                        launcher.launch(uri)
                    }) {
                        Text("Modificar foto", color = Color.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditChoiceDialog = false }) {
                    Text("Cancelar", color = Color.Red)
                }
            }
        )
    }
}

@Composable
fun ComprobanteCard(
    comprobante: Comprobante,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: (Uri) -> Unit
) {
    val uri = Uri.parse(comprobante.uri)
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0x22000000))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comprobante.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Agregado el: ${comprobante.fecha}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Hora: ${comprobante.hora ?: "No disponible"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (selected) {
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                    TextButton(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Eliminar")
                    }
                    TextButton(
                        onClick = {
                            val bitmap = bitmapFromUri(context, uri)
                            if (bitmap != null) {
                                val rotatedBitmap = rotateImageIfRequired(context, bitmap, uri)
                                saveToGallery(context, rotatedBitmap, comprobante.nombre)
                                Toast.makeText(
                                    context,
                                    "Imagen guardada en la galería",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error al cargar imagen",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text("Descargar")
                    }
                    TextButton(
                        onClick = { onView(uri) }
                    ) {
                        Text("Ver")
                    }
                }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = Color.White,
                        title = { Text("Confirmar eliminación", color = Color.Black) },
                        text = { Text("¿Estás seguro que deseas eliminar este comprobante?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDialog = false
                                onDelete()
                            }) {
                                Text("Sí", color = Color.Red)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancelar", color = Color.Red)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun createTempUri(context: Context): Uri {
    val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val fileName = "comprobante_${UUID.randomUUID()}.jpg"
        val dir = File(context.filesDir, "comprobantes")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveComprobante(context: Context, comprobante: Comprobante) {
    val prefs = context.getSharedPreferences("comprobantes_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val current = prefs.getString("comprobantes", "[]")
    val list = gson.fromJson(current, Array<Comprobante>::class.java).toMutableList()
    list.add(comprobante)
    prefs.edit().putString("comprobantes", gson.toJson(list)).apply()
}

fun saveComprobantesList(context: Context, list: List<Comprobante>) {
    val prefs = context.getSharedPreferences("comprobantes_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    prefs.edit().putString("comprobantes", gson.toJson(list)).apply()
}

fun loadComprobantes(context: Context): List<Comprobante> {
    val prefs = context.getSharedPreferences("comprobantes_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("comprobantes", "[]")
    val gson = Gson()

    return gson.fromJson(json, Array<Comprobante>::class.java).map {
        if (it.hora == null) {
            val uri = Uri.parse(it.uri)
            val horaExtraida = obtenerHoraDeArchivo(context, uri)
            it.copy(hora = horaExtraida)
        } else {
            it
        }
    }
}

fun obtenerHoraDeArchivo(context: Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = ExifInterface(inputStream!!)
        val datetime = exif.getAttribute(ExifInterface.TAG_DATETIME)
        inputStream.close()

        datetime?.let {
            val originalFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val date = originalFormat.parse(it)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date!!)
        } ?: "Hora no disponible"
    } catch (e: Exception) {
        "Hora no disponible"
    }
}

fun bitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveToGallery(context: Context, bitmap: Bitmap, name: String) {
    val resolver = context.contentResolver
    val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Comprobantes")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(imageCollection, contentValues)
    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(it, contentValues, null, null)
    }
}

fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
    val exif = androidx.exifinterface.media.ExifInterface(inputStream)

    val orientation = exif.getAttributeInt(
        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
    )

    val matrix = Matrix()
    when (orientation) {
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}