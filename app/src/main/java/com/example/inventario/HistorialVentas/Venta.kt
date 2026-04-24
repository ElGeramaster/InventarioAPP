package com.example.inventario.HistorialVentas

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ventas")
data class Venta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: Long,
    val total: Double,
    val totalItems: Int
)
