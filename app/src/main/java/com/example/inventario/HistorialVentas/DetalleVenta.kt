package com.example.inventario.HistorialVentas

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detalle_ventas")
data class DetalleVenta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ventaId: Int,
    val productoId: Int,
    val nombreProducto: String,
    val precioUnitario: Double,
    val precioCompra: Double,
    val cantidad: Int
)
