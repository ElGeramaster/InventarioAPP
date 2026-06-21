package com.example.inventario

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

object LicenseManager {

    private const val PREFS = "licencia_prefs"
    private const val KEY_FECHA_ACTIVACION = "fecha_activacion"
    private const val KEY_VERSION_ACTIVADA = "version_activada"
    private const val KEY_CLAVES_USADAS = "claves_usadas"
    private const val KEY_ULTIMA_CLAVE = "ultima_clave"

    /** Días que dura una activación antes de volver a pedir usuario y clave. */
    private const val DIAS_VIGENCIA = 30 L
    private const val MS_VIGENCIA = DIAS_VIGENCIA * 24L * 60L * 60L * 1000L


    private const val USUARIO_VALIDO =
        "d3199ed7bff4334b7226070ea9b915ab477c2d04ebac4c44f922d86aa379e99d" // MiNegocioApp

    
    private val CODIGOS_VALIDOS = setOf(
        "83d12fd61df6945d5db2288f24f4066f9efd6f63ee9746b001d95f239c554dbb", // Mi_MERCANCIA06012
        "878205bfe7ad2d1e5adf7b4ad6451336f57947e37fc6c181c4c841b8ea315bba", // PR0DUCT0S_1207?
        "d9b0bc330bb18b56b9eeeaafef4be6ee958115d98821d895479f7975696558b6", // ger12345?
        "d170488fd82ae842d1f6431def71e9d1bfd4dffeef3f9271cf9f6e2e4e8fb55a", // T1EnD1TA_DK130W*
        "4a4b1585ec0e93bbfabaac530a88b7c5e4d4142a6726776d2c4e9950d741b445", // F1C2G3D4R5?
        "0f7ae578a666b74f6aafda9cf9e1013a613388d9804d30d816631154e2a3be8b"  // INVENTARIO-F6
    )

    /** Resultado de un intento de activación, para mostrar el mensaje correcto. */
    enum class ResultadoActivacion {
        OK,
        USUARIO_INCORRECTO,
        CLAVE_INCORRECTA,
        CLAVE_REPETIDA
    }

    /** Devuelve true si hay que mostrar la pantalla de activación. */
    fun requiereActivacion(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fechaActivacion = prefs.getLong(KEY_FECHA_ACTIVACION, 0L)

        // Nunca se ha activado.
        if (fechaActivacion == 0L) return true

        // Se actualizó la app: forzar nueva activación.
        val versionActivada = prefs.getLong(KEY_VERSION_ACTIVADA, -1L)
        if (versionActivada != versionActual(context)) return true

        // Venció el periodo de 30 días.
        val transcurrido = System.currentTimeMillis() - fechaActivacion
        return transcurrido < 0 || transcurrido >= MS_VIGENCIA
    }

    /**
     * Valida el [usuario] y la [contrasena] introducidos.
     *  - El usuario debe coincidir con [USUARIO_VALIDO].
     *  - La contraseña debe ser una de [CODIGOS_VALIDOS] y NO haberse usado ya
     *    en este ciclo ni ser igual a la última usada (rotación sin repetir).
     * Si todo es correcto guarda la activación y devuelve [ResultadoActivacion.OK].
     */
    fun activar(context: Context, usuario: String, contrasena: String): ResultadoActivacion {
        if (sha256(usuario.trim()) != USUARIO_VALIDO) {
            return ResultadoActivacion.USUARIO_INCORRECTO
        }

        val hashClave = sha256(contrasena.trim())
        if (hashClave !in CODIGOS_VALIDOS) {
            return ResultadoActivacion.CLAVE_INCORRECTA
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Copia mutable: nunca se debe modificar el set que devuelve getStringSet.
        var usadas = prefs.getStringSet(KEY_CLAVES_USADAS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val ultima = prefs.getString(KEY_ULTIMA_CLAVE, null)

        // Si ya se usaron las 6 claves, empieza un ciclo nuevo.
        if (usadas.containsAll(CODIGOS_VALIDOS)) {
            usadas = mutableSetOf()
        }

        // No se puede repetir la clave anterior ni una ya usada en este ciclo.
        if (hashClave == ultima || hashClave in usadas) {
            return ResultadoActivacion.CLAVE_REPETIDA
        }

        usadas.add(hashClave)
        prefs.edit()
            .putLong(KEY_FECHA_ACTIVACION, System.currentTimeMillis())
            .putLong(KEY_VERSION_ACTIVADA, versionActual(context))
            .putStringSet(KEY_CLAVES_USADAS, usadas)
            .putString(KEY_ULTIMA_CLAVE, hashClave)
            .apply()
        return ResultadoActivacion.OK
    }

    /** Días que faltan para que venza la activación actual (0 si ya venció). */
    fun diasRestantes(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fechaActivacion = prefs.getLong(KEY_FECHA_ACTIVACION, 0L)
        if (fechaActivacion == 0L) return 0L
        val transcurrido = System.currentTimeMillis() - fechaActivacion
        val restanteMs = MS_VIGENCIA - transcurrido
        return if (restanteMs <= 0) 0L else (restanteMs / (24L * 60L * 60L * 1000L)) + 1
    }

    private fun versionActual(context: Context): Long {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info)
        } catch (e: Exception) {
            0L
        }
    }

    private fun sha256(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}