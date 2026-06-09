package com.example.inventario.TiendaGUI

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.inventario.AppDatabase
import com.example.inventario.Producto
import com.example.inventario.R
import com.google.android.material.button.MaterialButton
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * Escáner continuo: la cámara permanece abierta y permite escanear varios
 * productos seguidos sin tener que volver a pulsar el botón de escanear.
 * Cada código leído se acumula y se devuelve a la tienda al terminar, donde
 * se reflejan todos los productos en el carrito.
 *
 * Si se escanea un producto que ya no tiene stock se avisa al usuario:
 *  - "Entendido": el producto agotado no se cuenta.
 *  - "Sí, tengo más productos": se suma 1 al inventario para poder venderlo
 *    y el escaneo sí se cuenta.
 */
class ContinuousScanActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var tvContador: TextView
    private lateinit var btnListo: MaterialButton
    private lateinit var beepManager: BeepManager
    private lateinit var db: AppDatabase

    private val codigosEscaneados = ArrayList<String>()

    // Cuántas unidades de cada producto se han escaneado en esta sesión.
    private val conteoPorProducto = HashMap<Int, Int>()

    // Evita que un mismo código se cuente muchas veces por los fotogramas seguidos.
    private var ultimoCodigo: String? = null
    private var ultimoTiempo: Long = 0L
    private var permisoSolicitado = false
    private var dialogoAbierto = false

    companion object {
        const val EXTRA_CODIGOS = "codigos_escaneados"
        private const val DEBOUNCE_MS = 1500L
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (dialogoAbierto) return
            val texto = result.text ?: return
            val ahora = System.currentTimeMillis()
            if (texto == ultimoCodigo && ahora - ultimoTiempo < DEBOUNCE_MS) {
                return
            }
            ultimoCodigo = texto
            ultimoTiempo = ahora

            val producto = db.productoDao().buscarPorCodigoBarras(texto)
            // Solo controlamos stock de productos que se venden por pieza.
            if (producto != null && !producto.vendePorPeso) {
                val yaEscaneados = conteoPorProducto[producto.id] ?: 0
                if (yaEscaneados >= producto.cantidad) {
                    mostrarDialogoSinStock(producto, texto)
                    return
                }
                conteoPorProducto[producto.id] = yaEscaneados + 1
            }

            registrarCodigo(texto)
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    private val permisoCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (!concedido) {
                Toast.makeText(
                    this,
                    "Se necesita permiso de cámara para escanear",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuous_scan)

        db = AppDatabase.getInstance(this)

        barcodeView = findViewById(R.id.barcodeScanner)
        tvContador = findViewById(R.id.tvContadorEscaneo)
        btnListo = findViewById(R.id.btnListoEscaneo)

        beepManager = BeepManager(this)

        barcodeView.setStatusText("")
        barcodeView.decodeContinuous(callback)

        btnListo.setOnClickListener { finalizar() }
        actualizarContador()
    }

    private fun registrarCodigo(texto: String) {
        codigosEscaneados.add(texto)
        beepManager.playBeepSoundAndVibrate()
        actualizarContador()
    }

    private fun mostrarDialogoSinStock(producto: Producto, texto: String) {
        dialogoAbierto = true
        barcodeView.pause()

        AlertDialog.Builder(this)
            .setTitle("Sin stock")
            .setMessage("No hay artículos disponibles de ${producto.nombre}.")
            .setCancelable(false)
            .setPositiveButton("Sí, tengo más productos") { _, _ ->
                // Sumamos 1 al inventario para poder concretar la venta.
                val actualizado = producto.copy(cantidad = producto.cantidad + 1)
                db.productoDao().actualizar(actualizado)
                conteoPorProducto[producto.id] = (conteoPorProducto[producto.id] ?: 0) + 1
                registrarCodigo(texto)
                reanudarEscaneo()
            }
            .setNegativeButton("Entendido") { _, _ ->
                // El producto agotado no se cuenta.
                reanudarEscaneo()
            }
            .show()
    }

    private fun reanudarEscaneo() {
        ultimoCodigo = null
        ultimoTiempo = 0L
        dialogoAbierto = false
        if (tienePermisoCamara()) {
            barcodeView.resume()
        }
    }

    private fun actualizarContador() {
        val total = codigosEscaneados.size
        tvContador.text = if (total == 0) {
            "Escanea los códigos de los productos"
        } else {
            "Productos escaneados: $total"
        }
        btnListo.text = if (total == 0) "Listo" else "Listo ($total)"
    }

    private fun finalizar() {
        val data = Intent().putStringArrayListExtra(EXTRA_CODIGOS, codigosEscaneados)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (dialogoAbierto) return
        if (tienePermisoCamara()) {
            barcodeView.resume()
        } else if (!permisoSolicitado) {
            permisoSolicitado = true
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }

    private fun tienePermisoCamara(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Volver también refleja en el carrito los productos ya escaneados.
        finalizar()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}