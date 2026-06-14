package com.example.inventario

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import java.security.MessageDigest

/**
 * Control de activación de la app por usuario + contraseña (clave de seguridad).
 *
 * Funcionamiento:
 *  - Al entrar se pide un USUARIO y una CONTRASEÑA (clave).
 *  - El usuario es siempre el mismo (lo eliges tú).
 *  - Hay varias claves válidas (mínimo 6). Cada vez que se vuelve a pedir,
 *    la clave debe ser DISTINTA a las que ya se usaron: las 6 claves rotan
 *    una por una sin repetirse hasta agotarlas; cuando se usan todas, empieza
 *    un ciclo nuevo (y nunca se repite la última usada).
 *  - Se vuelve a pedir cuando pasan [DIAS_VIGENCIA] días (30 por defecto).
 *  - También se vuelve a pedir en cada actualización de la app (cuando cambia
 *    el versionCode), para que las claves viejas dejen de servir.
 *
 * CÓMO CAMBIAR EL USUARIO Y LAS CLAVES:
 *  - Aquí se guardan SOLO los hashes (SHA-256 en minúsculas), nunca el texto,
 *    para que las claves no aparezcan a simple vista dentro del APK.
 *  - Para obtener el hash de una palabra, en una terminal:
 *        printf '%s' 'MI-CLAVE' | sha256sum
 *    o pídele a tu asistente que genere el hash de la palabra que quieras.
 *  - Reemplaza el valor de [USUARIO_VALIDO] por el hash de tu usuario.
 *  - Reemplaza los valores de [CODIGOS_VALIDOS] por los hashes de tus claves
 *    (deja al menos 6).
 *
 * USUARIO Y CLAVES DE EJEMPLO ACTUALES (cámbialos por los tuyos):
 *   - Usuario:  GERAM
 *   - Claves:   INVENTARIO-A1, INVENTARIO-B2, INVENTARIO-C3,
 *               INVENTARIO-D4, INVENTARIO-E5, INVENTARIO-F6
 */
object LicenseManager {

    private const val PREFS = "licencia_prefs"
    private const val KEY_FECHA_ACTIVACION = "fecha_activacion"
    private const val KEY_VERSION_ACTIVADA = "version_activada"
    private const val KEY_CLAVES_USADAS = "claves_usadas"
    private const val KEY_ULTIMA_CLAVE = "ultima_clave"

    /** Días que dura una activación antes de volver a pedir usuario y clave. */
    private const val DIAS_VIGENCIA = 30L
    private const val MS_VIGENCIA = DIAS_VIGENCIA * 24L * 60L * 60L * 1000L

    /**
     * Hash SHA-256 (minúsculas) del USUARIO válido.
     * Ejemplo actual: "GERAM". Cámbialo por el hash de tu usuario.
     */
    private const val USUARIO_VALIDO =
        "a4047d7a201e0cf00199eedf91b79d3fc9883a240890ccbb2940dc6ac9c1aebd" // GERAM

    /**
     * Hashes SHA-256 (minúsculas) de las CLAVES válidas. Deja al menos 6.
     * Las claves rotan sin repetirse: cada renovación pide una clave distinta.
     *
     * Claves de ejemplo actuales (cámbialas por las tuyas):
     *   - "INVENTARIO-A1"
     *   - "INVENTARIO-B2"
     *   - "INVENTARIO-C3"
     *   - "INVENTARIO-D4"
     *   - "INVENTARIO-E5"
     *   - "INVENTARIO-F6"
     */
    private val CODIGOS_VALIDOS = setOf(
        "15e0d6fe8426803231a650ba365056f73a4a3b70c2ca937d7660c4399a29653e", // INVENTARIO-A1
        "1a2b2099085694812af0d6dec45ff2e2a240656b5aafa7abaacb5fc7d64392a2", // INVENTARIO-B2
        "3529bab44ace081d2b3f6e9f035f1556ba87d16ad4e5909b12be037ec35a9b72", // INVENTARIO-C3
        "683a729950b37ae49164927b72f6fe649e0b8fe0c90c265a467fa1fbe7810bde", // INVENTARIO-D4
        "e4276d512ad95796f827f8332533b99843496b64572003c11e6f9cdf5508fc4d", // INVENTARIO-E5
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
