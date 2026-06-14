package com.example.inventario

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Guarda y recupera el logo de la tienda elegido por el usuario desde la galería.
 * La imagen se copia al almacenamiento interno y se recuerda su ruta.
 */
object LogoManager {

    private const val PREFS = "logo_prefs"
    private const val KEY_PATH = "logo_path"
    private const val NOMBRE_ARCHIVO = "logo.png"

    fun guardarDesdeUri(context: Context, uri: Uri): Boolean {
        return try {
            val dir = File(context.filesDir, "branding")
            if (!dir.exists()) dir.mkdirs()
            val archivo = File(dir, NOMBRE_ARCHIVO)
            context.contentResolver.openInputStream(uri)?.use { input ->
                archivo.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            prefs(context).edit().putString(KEY_PATH, archivo.absolutePath).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Ruta del logo si existe, o null si no hay logo configurado. */
    fun obtenerPath(context: Context): String? {
        val path = prefs(context).getString(KEY_PATH, null) ?: return null
        return if (File(path).exists()) path else null
    }

    fun hayLogo(context: Context): Boolean = obtenerPath(context) != null

    fun quitar(context: Context) {
        val path = prefs(context).getString(KEY_PATH, null)
        if (path != null) runCatching { File(path).delete() }
        prefs(context).edit().remove(KEY_PATH).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
