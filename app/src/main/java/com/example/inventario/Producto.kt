package com.example.inventario

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val precioCompra: Double = 0.0,
    val precio: Double,
    val cantidad: Int,
    val stockMinimo: Int,
    val imagenUri: String? = null
)