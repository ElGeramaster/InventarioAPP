package com.example.inventario

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

/**
 * Control de activación de la app por código de seguridad.
 *
 * Funcionamiento:
 *  - En la primera apertura se pide un código de activación.
 *  - El código vuelve a pedirse cuando pasan [DIAS_VIGENCIA] días (30 por defecto).
 *  - El código también vuelve a pedirse en cada actualización de la app
 *    (cuando cambia el versionCode), para que las claves viejas dejen de servir.
 *
 * CÓMO CAMBIAR LA CLAVE EN CADA ACTUALIZACIÓN:
 *  1. Sube el `versionCode` en app/build.gradle.kts (1 -> 2 -> 3 ...).
 *  2. Reemplaza/añade el hash en [CODIGOS_VALIDOS] por el de tu nueva clave.
 *     El hash es el SHA-256 (en minúsculas) del código en texto.
 *     Por ejemplo, con la terminal:
 *         printf '%s' 'MI-NUEVA-CLAVE' | sha256sum
 *     o pídele a tu asistente que genere el hash de la clave que quieras.
 *
 * Nota: aquí se guardan solo los hashes, nunca el texto de la clave, para que
 * no aparezca a simple vista dentro del APK.
 */
object LicenseManager {

    private const val PREFS = "licencia_prefs"
    private const val KEY_FECHA_ACTIVACION = "fecha_activacion"
    private const val KEY_VERSION_ACTIVADA = "version_activada"

    /** Días que dura una activación antes de volver a pedir el código. */
    private const val DIAS_VIGENCIA = 30L
    private const val MS_VIGENCIA = DIAS_VIGENCIA * 24L * 60L * 60L * 1000L

    /**
     * Hashes SHA-256 (minúsculas) de los códigos de activación válidos.
     * Cambia estos valores en cada actualización para invalidar las claves anteriores.
     *
     * Claves de ejemplo incluidas por defecto:
     *   - "INVENTARIO2026"
     *   - "GERAM-2026"
     */
    private val CODIGOS_VALIDOS = setOf(
        "c168b468c4c066b30fda7608b85a058dbff55a8b8b20b061e1c289b31f75dd84", // INVENTARIO2026
        "a4fb7ab9a2fccf326cea535b937b4b8ac22039d7fe47db8b11db1d9401c54818"  // GERAM-2026
    )

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
     * Valida el [codigo] introducido. Si es correcto guarda la activación y
     * devuelve true; si no, devuelve false.
     */
    fun activar(context: Context, codigo: String): Boolean {
        val hash = sha256(codigo.trim())
        if (hash !in CODIGOS_VALIDOS) return false

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_FECHA_ACTIVACION, System.currentTimeMillis())
            .putLong(KEY_VERSION_ACTIVADA, versionActual(context))
            .apply()
        return true
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