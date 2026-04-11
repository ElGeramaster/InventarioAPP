package com.example.inventario.TiendaGUI

import com.example.inventario.Producto

data class CarritoItem(
    val producto: Producto,
    var cantidad: Int
) {
    val subtotal: Double
        get() = producto.precio * cantidad
}
