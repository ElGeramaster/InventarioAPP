package com.example.inventario

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class AgregarProductoActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecio: TextInputEditText
    private lateinit var etCantidad: TextInputEditText
    private lateinit var etStockMinimo: TextInputEditText
    private lateinit var db: AppDatabase

    private var productoExistente: Producto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        db = AppDatabase.getInstance(this)

        etNombre     = findViewById(R.id.etNombre)
        etCategoria  = findViewById(R.id.etCategoria)
        etPrecio     = findViewById(R.id.etPrecio)
        etCantidad   = findViewById(R.id.etCantidad)
        etStockMinimo = findViewById(R.id.etStockMinimo)

        // Si viene un ID, es modo edición
        val productoId = intent.getIntExtra("PRODUCTO_ID", -1)
        if (productoId != -1) {
            productoExistente = db.productoDao().obtenerPorId(productoId)
            productoExistente?.let { llenarCampos(it) }
            title = "Editar producto"
        } else {
            title = "Agregar producto"
        }

        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarProducto()
        }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            finish()
        }
    }

    private fun llenarCampos(producto: Producto) {
        etNombre.setText(producto.nombre)
        etCategoria.setText(producto.categoria)
        etPrecio.setText(producto.precio.toString())
        etCantidad.setText(producto.cantidad.toString())
        etStockMinimo.setText(producto.stockMinimo.toString())
    }

    private fun guardarProducto() {
        val nombre     = etNombre.text.toString().trim()
        val categoria  = etCategoria.text.toString().trim()
        val precioStr  = etPrecio.text.toString().trim()
        val cantidadStr = etCantidad.text.toString().trim()
        val stockMinimoStr = etStockMinimo.text.toString().trim()

        // Validaciones
        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            return
        }
        if (categoria.isEmpty()) {
            etCategoria.error = "La categoría es obligatoria"
            return
        }
        if (precioStr.isEmpty()) {
            etPrecio.error = "El precio es obligatorio"
            return
        }
        if (cantidadStr.isEmpty()) {
            etCantidad.error = "La cantidad es obligatoria"
            return
        }
        if (stockMinimoStr.isEmpty()) {
            etStockMinimo.error = "El stock mínimo es obligatorio"
            return
        }

        val precio     = precioStr.toDouble()
        val cantidad   = cantidadStr.toInt()
        val stockMinimo = stockMinimoStr.toInt()

        if (productoExistente != null) {
            // Modo edición — conservamos el mismo ID
            val actualizado = productoExistente!!.copy(
                nombre = nombre,
                categoria = categoria,
                precio = precio,
                cantidad = cantidad,
                stockMinimo = stockMinimo
            )
            db.productoDao().actualizar(actualizado)
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
        } else {
            // Modo agregar
            val nuevo = Producto(
                nombre = nombre,
                categoria = categoria,
                precio = precio,
                cantidad = cantidad,
                stockMinimo = stockMinimo
            )
            db.productoDao().insertar(nuevo)
            Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}