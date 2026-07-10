package com.example.inventario

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Deuda de un cliente al que se le "fió" mercancía. [monto] es el saldo que aún
 * debe: al abonar disminuye y al agregar más deuda aumenta.
 */
@Entity(tableName = "fiados")
data class Fiado(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val telefono: String? = null,
    val monto: Double,
    val descripcion: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
