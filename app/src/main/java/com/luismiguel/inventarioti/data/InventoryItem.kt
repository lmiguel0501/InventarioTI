package com.luismiguel.inventarioti.data

import java.util.UUID

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serialNumber: Int,
    val status: String,
    val quantity: Int
)

data class Comprobante(
    val uri: String,
    val nombre: String,
    val fecha: String,
    val hora: String
)