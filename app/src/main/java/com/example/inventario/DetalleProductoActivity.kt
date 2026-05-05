package com.example.inventario

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var tvPrecioCompra: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvCantidad: TextView
    private lateinit var tvStockMinimo: TextView
    private lateinit var tvAlertaStock: TextView
    private lateinit var ivDetalleProducto: ImageView
    private lateinit var ivQrCode: ImageView
    private lateinit var cardImagen: CardView
    private lateinit var db: AppDatabase

    private var producto: Producto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto)

        db = AppDatabase.getInstance(this)

        tvNombre          = findViewById(R.id.tvDetalleNombre)
        tvCategoria       = findViewById(R.id.tvDetalleCategoria)
        tvPrecioCompra    = findViewById(R.id.tvDetallePrecioCompra)
        tvPrecio          = findViewById(R.id.tvDetallePrecio)
        tvCantidad        = findViewById(R.id.tvDetalleCantidad)
        tvStockMinimo     = findViewById(R.id.tvDetalleStockMinimo)
        tvAlertaStock     = findViewById(R.id.tvAlertaStock)
        ivDetalleProducto = findViewById(R.id.ivDetalleProducto)
        ivQrCode          = findViewById(R.id.ivQrCode)
        cardImagen        = findViewById(R.id.cardImagen)

        val productoId = intent.getIntExtra("PRODUCTO_ID", -1)
        if (productoId == -1) {
            Toast.makeText(this, "Error al cargar el producto", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        producto = db.productoDao().obtenerPorId(productoId)
        producto?.let { mostrarDatos(it) } ?: run {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnEditar).setOnClickListener {
            val intent = Intent(this, AgregarProductoActivity::class.java)
            intent.putExtra("PRODUCTO_ID", producto!!.id)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnEliminar).setOnClickListener {
            confirmarEliminacion()
        }
    }

    override fun onResume() {
        super.onResume()
        producto?.let {
            producto = db.productoDao().obtenerPorId(it.id)
            producto?.let { p -> mostrarDatos(p) }
        }
    }

    private fun mostrarDatos(p: Producto) {
        title = p.nombre
        tvNombre.text = p.nombre
        tvCategoria.text = p.categoria
        tvPrecioCompra.text = "$${"%.2f".format(p.precioCompra)}"
        tvPrecio.text = "$${"%.2f".format(p.precio)}"
        tvCantidad.text = "${p.cantidad} unidades"
        tvStockMinimo.text = "${p.stockMinimo} unidades"

        if (!p.imagenUri.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(p.imagenUri)
            if (bitmap != null) {
                ivDetalleProducto.setImageBitmap(bitmap)
                cardImagen.visibility = View.VISIBLE
            } else {
                cardImagen.visibility = View.GONE
            }
        } else {
            cardImagen.visibility = View.GONE
        }

        if (p.cantidad <= p.stockMinimo) {
            tvAlertaStock.visibility = View.VISIBLE
        } else {
            tvAlertaStock.visibility = View.GONE
        }

        mostrarQr(p)
    }

    private fun mostrarQr(producto: Producto) {
        try {
            val bitmap = QrCodeHelper.generarQr(producto)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Estás seguro de que deseas eliminar ${producto!!.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.productoDao().eliminar(producto!!)
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
