package com.example.inventario

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "stock_bajo_channel"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_stock_bajo_ids"

    // --- Aviso de pago mensual ---
    private const val CHANNEL_PAGO_ID = "pago_mensual_channel"
    private const val NOTIFICATION_PAGO_ID = 2001
    private const val KEY_ULTIMA_FECHA_AVISO_PAGO = "ultima_fecha_aviso_pago"
    /** Solo se avisa cuando faltan estos días o menos para que venza la licencia. */
    private const val DIAS_PARA_AVISAR = 5L

    fun crearCanal(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de stock bajo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones cuando productos alcanzan su stock mínimo"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun verificarYNotificarStockBajo(context: Context) {
        crearCanal(context)
        val db = AppDatabase.getInstance(context)
        val productosStockBajo = db.productoDao().obtenerStockBajo()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idsNotificados = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()

        val idsActuales = productosStockBajo.map { it.id.toString() }.toSet()

        if (productosStockBajo.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            prefs.edit().putStringSet(KEY_NOTIFIED_IDS, emptySet()).apply()
            return
        }

        val nuevosIds = idsActuales - idsNotificados
        if (nuevosIds.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val cantidad = productosStockBajo.size
        val titulo = if (cantidad == 1) {
            "1 producto con stock bajo"
        } else {
            "$cantidad productos con stock bajo"
        }

        val detalles = productosStockBajo.take(5).joinToString("\n") { producto ->
            "• ${producto.nombre}: ${producto.cantidad}/${producto.stockMinimo} unidades"
        }
        val textoCompleto = if (productosStockBajo.size > 5) {
            "$detalles\n...y ${productosStockBajo.size - 5} más"
        } else {
            detalles
        }

        val intent = Intent(context, ReportesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText("Hay productos que necesitan resurtido")
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoCompleto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, idsActuales).apply()
    }

    /** Crea el canal para los avisos del pago mensual (Android 8+). */
    fun crearCanalPago(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_PAGO_ID,
            "Recordatorio de pago",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando se acerca la fecha del pago mensual"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Avisa que se acerca la fecha del pago mensual.
     *
     * Solo muestra la notificación durante los últimos [DIAS_PARA_AVISAR] días de
     * uso (cuando faltan 5, 4, 3, 2 o 1 día para vencer) y como máximo una vez por
     * día de uso. Debe llamarse al abrir la app.
     */
    fun verificarYNotificarPagoMensual(context: Context) {
        crearCanalPago(context)

        val dias = LicenseManager.diasRestantes(context)
        // Solo avisar en los últimos días de uso (5, 4, 3, 2, 1). Si ya venció
        // (0) la app pide reactivación por su cuenta, así que no avisamos aquí.
        if (dias < 1L || dias > DIAS_PARA_AVISAR) return

        // Una sola vez por día: si ya avisamos hoy, no repetir.
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (prefs.getString(KEY_ULTIMA_FECHA_AVISO_PAGO, null) == hoy) return

        // En Android 13+ se necesita permiso para mostrar notificaciones.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val titulo = if (dias == 1L) {
            "Tu acceso vence mañana"
        } else {
            "Tu acceso vence en $dias días"
        }
        val texto = "Recuerda realizar tu pago mensual para seguir usando la app sin interrupciones."

        val intent = Intent(context, LicenseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PAGO_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_PAGO_ID, notification)

        prefs.edit().putString(KEY_ULTIMA_FECHA_AVISO_PAGO, hoy).apply()
    }
}