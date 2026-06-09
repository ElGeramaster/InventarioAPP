package com.example.inventario.TiendaGUI

import com.example.inventario.Producto

/**
 * Artículo dentro del carrito.
 *  - Si [porPeso] es false: se vende por pieza y se usa [cantidad] (unidades), con control de stock.
 *  - Si [porPeso] es true: se vende por kilo y se usa [gramos]; no descuenta stock.
 */
data class CarritoItem(
    val producto: Producto,
    var cantidad: Int = 0,
    var gramos: Int = 0,
    val porPeso: Boolean = false
) {
    val subtotal: Double
        get() = if (porPeso) {
            producto.precioKilo * gramos / 1000.0
        } else {
            producto.precio * cantidad
        }

    /** Texto para mostrar la cantidad en el carrito ("Cant: 2" o "0.50 kg"). */
    val textoCantidad: String
        get() = if (porPeso) formatearKg(gramos) else "Cant: $cantidad"

    companion object {
        fun formatearKg(gramos: Int): String {
            val kg = gramos / 1000.0
            return "${"%.3f".format(kg).trimEnd('0').trimEnd('.')} kg"
        }
    }
}
