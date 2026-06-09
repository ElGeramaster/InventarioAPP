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
    val imagenUri: String? = null,
    val codigoBarras: String? = null,
    val favorito: Boolean = false,
    // --- Frutas y verduras (venta por peso) ---
    // Si es true, el producto también se puede vender por kilo (¼, ½, 1 kg...).
    // La venta por peso NO descuenta stock (solo se usa el precio por kilo).
    val vendePorPeso: Boolean = false,
    val precioKilo: Double = 0.0,
    val precioCompraKilo: Double = 0.0
) {
    /** El producto se puede vender por pieza (lleva control de stock). */
    val seVendePorPieza: Boolean
        get() = !vendePorPeso || precio > 0.0
}
