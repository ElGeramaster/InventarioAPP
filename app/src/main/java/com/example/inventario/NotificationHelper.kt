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

object NotificationHelper {

    private const val CHANNEL_ID = "stock_bajo_channel"
    private const val NOTIFICATION_ID = 1001

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

        if (productosStockBajo.isEmpty()) {
            // Si no hay stock bajo, cancelar notificación existente
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }

        // Verificar permiso de notificaciones (Android 13+)
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

        // Crear lista de nombres de productos para el detalle
        val detalles = productosStockBajo.take(5).joinToString("\n") { producto ->
            "• ${producto.nombre}: ${producto.cantidad}/${producto.stockMinimo} unidades"
        }
        val textoCompleto = if (productosStockBajo.size > 5) {
            "$detalles\n...y ${productosStockBajo.size - 5} más"
        } else {
            detalles
        }

        // Intent para abrir ReportesActivity al tocar la notificación
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
    }
}