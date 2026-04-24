package com.example.inventario.HistorialVentas

data class EstadisticaProducto(
    val nombreProducto: String,
    val productoId: Int,
    val totalVendido: Int,
    val totalIngresos: Double,
    val totalGanancia: Double
)
