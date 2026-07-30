package com.example.inventario

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Carga las fotos de productos y proveedores de forma segura.
 *
 * Las fotos se guardan tal cual las entrega la cámara o la galería, así que
 * suelen tener varios megapíxeles. Decodificarlas completas para pintarlas en
 * una miniatura de 50dp gastaba decenas de MB de memoria por imagen y la app se
 * cerraba con OutOfMemoryError al desplazar las listas.
 *
 * Aquí se lee primero solo el tamaño del archivo y luego se decodifica reducido
 * al tamaño que de verdad se va a mostrar.
 */
object ImagenUtils {

    /** Para miniaturas de listas (celdas pequeñas). */
    const val MINIATURA_PX = 320

    /** Para fotos grandes: detalle del producto, diálogos y logo de la tienda. */
    const val GRANDE_PX = 1024

    /** Miniatura para listas. Devuelve null si no hay imagen o no se pudo leer. */
    fun miniatura(ruta: String?): Bitmap? = decodificar(ruta, MINIATURA_PX)

    /** Foto grande para pantallas de detalle. Devuelve null si no se pudo leer. */
    fun grande(ruta: String?): Bitmap? = decodificar(ruta, GRANDE_PX)

    /**
     * Decodifica [ruta] reducida para que su lado mayor quede cerca de
     * [maxLadoPx]. Devuelve null si la ruta está vacía, el archivo no existe o
     * la imagen está dañada, para que quien llame muestre el placeholder.
     */
    fun decodificar(ruta: String?, maxLadoPx: Int): Bitmap? {
        if (ruta.isNullOrEmpty()) return null

        val archivo = File(ruta)
        if (!archivo.exists() || archivo.length() == 0L) return null

        return try {
            val medida = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(ruta, medida)
            if (medida.outWidth <= 0 || medida.outHeight <= 0) return null

            val opciones = BitmapFactory.Options().apply {
                inSampleSize = calcularEscala(medida.outWidth, medida.outHeight, maxLadoPx)
            }
            BitmapFactory.decodeFile(ruta, opciones)
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Potencia de 2 que deja la imagen justo por encima de [maxLadoPx]. */
    private fun calcularEscala(ancho: Int, alto: Int, maxLadoPx: Int): Int {
        if (maxLadoPx <= 0) return 1
        var escala = 1
        var ladoMayor = maxOf(ancho, alto)
        while (ladoMayor / 2 >= maxLadoPx) {
            ladoMayor /= 2
            escala *= 2
        }
        return escala
    }
}
