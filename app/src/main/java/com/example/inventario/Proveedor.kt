package com.example.inventario

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un proveedor que deja producto en la tienda. Guardamos sus datos de contacto,
 * qué producto surte y una foto opcional.
 */
@Entity(tableName = "proveedores")
data class Proveedor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val telefono: String? = null,
    val producto: String? = null,
    val direccion: String? = null,
    val notas: String? = null,
    val imagenUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
