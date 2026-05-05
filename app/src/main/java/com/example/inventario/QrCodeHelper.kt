package com.example.inventario

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeHelper {

    fun generarQr(producto: Producto, size: Int = 512): Bitmap {
        val contenido = buildString {
            if (producto.id > 0) append("ID: ${producto.id}\n")
            append("Nombre: ${producto.nombre}\n")
            append("Categoría: ${producto.categoria}\n")
            append("Precio: $${"%.2f".format(producto.precio)}")
        }

        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun generarQrPreview(nombre: String, categoria: String, precio: Double, size: Int = 512): Bitmap {
        val contenido = buildString {
            append("Nombre: $nombre\n")
            append("Categoría: $categoria\n")
            append("Precio: $${"%.2f".format(precio)}")
        }

        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(contenido, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
