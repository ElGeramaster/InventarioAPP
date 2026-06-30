package com.example.inventario

import android.content.Context

/**
 * Guarda y recupera el nombre que se muestra para la tienda (el texto que por
 * defecto dice "Tienda"). El usuario puede cambiarlo desde Ajustes por cualquier
 * texto y se recuerda entre sesiones.
 */
object NombreTiendaManager {

    private const val PREFS = "tienda_prefs"
    private const val KEY_NOMBRE = "nombre_tienda"
    const val NOMBRE_POR_DEFECTO = "Tienda"

    /** Nombre actual de la tienda, o el valor por defecto si no se ha configurado. */
    fun obtenerNombre(context: Context): String =
        prefs(context).getString(KEY_NOMBRE, NOMBRE_POR_DEFECTO)
            ?.takeIf { it.isNotBlank() }
            ?: NOMBRE_POR_DEFECTO

    /**
     * Guarda el nombre indicado. Si está vacío vuelve al valor por defecto.
     */
    fun guardarNombre(context: Context, nombre: String) {
        val limpio = nombre.trim()
        val editor = prefs(context).edit()
        if (limpio.isEmpty()) {
            editor.remove(KEY_NOMBRE)
        } else {
            editor.putString(KEY_NOMBRE, limpio)
        }
        editor.apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
