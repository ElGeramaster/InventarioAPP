package com.example.inventario

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venta_detalles")
data class VentaDetalle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ventaId: Int,
    val productoId: Int,
    val productoNombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val precioCompra: Double,
    val subtotal: Double
)
