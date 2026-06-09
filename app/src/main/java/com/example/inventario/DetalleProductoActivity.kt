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
    private lateinit var tvCodigoBarras: TextView
    private lateinit var labelCodigoBarras: TextView
    private lateinit var ivDetalleProducto: ImageView
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
        tvCodigoBarras    = findViewById(R.id.tvDetalleCodigoBarras)
        labelCodigoBarras = findViewById(R.id.labelCodigoBarras)
        ivDetalleProducto = findViewById(R.id.ivDetalleProducto)
        cardImagen        = findViewById(R.id.cardImagen)

        // Obtener el producto de la base de datos
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
        // Recargar datos al volver de edición
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
        if (p.vendePorPeso) {
            val pieza = if (p.precio > 0) "  ·  pieza $${"%.2f".format(p.precio)}" else ""
            tvPrecio.text = "$${"%.2f".format(p.precioKilo)} / kg$pieza"
            tvCantidad.text = if (p.precio > 0) "${p.cantidad} unidades (por pieza)" else "Se vende por kilo"
        } else {
            tvPrecio.text = "$${"%.2f".format(p.precio)}"
            tvCantidad.text = "${p.cantidad} unidades"
        }
        tvStockMinimo.text = "${p.stockMinimo} unidades"

        // Mostrar imagen si existe
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

        if (!p.codigoBarras.isNullOrEmpty()) {
            tvCodigoBarras.text = p.codigoBarras
            tvCodigoBarras.visibility = View.VISIBLE
            labelCodigoBarras.visibility = View.VISIBLE
        } else {
            tvCodigoBarras.visibility = View.GONE
            labelCodigoBarras.visibility = View.GONE
        }

        // Mostrar alerta si el stock es bajo (solo productos que se venden por pieza)
        if (p.seVendePorPieza && p.cantidad <= p.stockMinimo) {
            tvAlertaStock.visibility = View.VISIBLE
        } else {
            tvAlertaStock.visibility = View.GONE
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