package com.example.inventario.TiendaGUI

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.AppDatabase
import com.example.inventario.HistorialVentas.HistorialVentasActivity
import com.example.inventario.MainActivity
import com.example.inventario.Producto
import com.example.inventario.R
import com.example.inventario.Venta
import com.example.inventario.VentaDetalle

class TiendaActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var rvCategorias: RecyclerView
    private lateinit var rvProductos: RecyclerView
    private lateinit var rvCarrito: RecyclerView
    private lateinit var cardBuscar: CardView
    private lateinit var etBuscar: EditText
    private lateinit var btnToggleBuscar: ImageButton
    private lateinit var tvTotal: TextView
    private lateinit var btnRealizar: Button
    private lateinit var tvSinProductos: TextView
    private lateinit var tvCarritoVacio: TextView

    private lateinit var categoriaAdapter: CategoriaTiendaAdapter
    private lateinit var productoAdapter: ProductoTiendaAdapter
    private lateinit var carritoAdapter: CarritoItemAdapter

    // Estado del carrito: id producto -> CarritoItem
    private val carrito = linkedMapOf<Int, CarritoItem>()

    private var categoriaSeleccionada: String = CATEGORIA_TODOS
    private var busquedaActual: String = ""

    companion object {
        private const val CATEGORIA_TODOS = "Todos los productos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tienda)

        db = AppDatabase.getInstance(this)

        rvCategorias    = findViewById(R.id.rvCategorias)
        rvProductos     = findViewById(R.id.rvProductosTienda)
        rvCarrito       = findViewById(R.id.rvCarrito)
        cardBuscar      = findViewById(R.id.cardBuscar)
        etBuscar        = findViewById(R.id.etBuscarTienda)
        btnToggleBuscar = findViewById(R.id.btnToggleBuscar)
        tvTotal         = findViewById(R.id.tvTotalTienda)
        btnRealizar     = findViewById(R.id.btnRealizarVenta)
        tvSinProductos  = findViewById(R.id.tvSinProductos)
        tvCarritoVacio  = findViewById(R.id.tvCarritoVacio)

        configurarCategorias()
        configurarProductos()
        configurarCarrito()
        configurarBuscador()

        btnRealizar.setOnClickListener {
            realizarVenta()
        }

        findViewById<ImageButton>(R.id.btnIrInventario).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnIrHistorial).setOnClickListener {
            startActivity(Intent(this, HistorialVentasActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Al volver, recargar por si cambió el inventario
        recargarCategorias()
        filtrarProductos()
    }

    private fun configurarCategorias() {
        rvCategorias.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val categorias = obtenerListaCategorias()
        categoriaAdapter = CategoriaTiendaAdapter(
            categorias = categorias,
            seleccionada = categoriaSeleccionada
        ) { categoria ->
            categoriaSeleccionada = categoria
            categoriaAdapter.cambiarSeleccion(categoria)
            filtrarProductos()
        }
        rvCategorias.adapter = categoriaAdapter
    }

    private fun recargarCategorias() {
        val categorias = obtenerListaCategorias()
        if (categoriaSeleccionada != CATEGORIA_TODOS &&
            !categorias.contains(categoriaSeleccionada)
        ) {
            categoriaSeleccionada = CATEGORIA_TODOS
        }
        categoriaAdapter.actualizar(categorias, categoriaSeleccionada)
    }

    private fun obtenerListaCategorias(): List<String> {
        val lista = mutableListOf(CATEGORIA_TODOS)
        lista.addAll(db.productoDao().obtenerCategorias())
        return lista
    }

    private fun configurarProductos() {
        rvProductos.layoutManager = GridLayoutManager(this, 3)
        productoAdapter = ProductoTiendaAdapter(
            emptyList(),
            onAgregar = { producto -> agregarAlCarrito(producto) },
            onVerDetalle = { producto -> mostrarModalProducto(producto) }
        )
        rvProductos.adapter = productoAdapter
        filtrarProductos()
    }

    private fun configurarCarrito() {
        rvCarrito.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        carritoAdapter = CarritoItemAdapter(emptyList()) { item ->
            mostrarOpcionesItem(item)
        }
        rvCarrito.adapter = carritoAdapter
        actualizarCarritoUI()
    }

    private fun configurarBuscador() {
        btnToggleBuscar.setOnClickListener {
            if (cardBuscar.visibility == View.VISIBLE) {
                cardBuscar.visibility = View.GONE
                etBuscar.setText("")
                busquedaActual = ""
                filtrarProductos()
            } else {
                cardBuscar.visibility = View.VISIBLE
                etBuscar.requestFocus()
            }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaActual = s?.toString()?.trim() ?: ""
                filtrarProductos()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarProductos() {
        val productos: List<Producto> = when {
            categoriaSeleccionada == CATEGORIA_TODOS && busquedaActual.isEmpty() ->
                db.productoDao().obtenerTodos()
            categoriaSeleccionada == CATEGORIA_TODOS ->
                db.productoDao().buscar(busquedaActual)
            else ->
                db.productoDao().buscarPorCategoria(categoriaSeleccionada, busquedaActual)
        }

        productoAdapter.actualizarLista(productos)

        if (productos.isEmpty()) {
            tvSinProductos.visibility = View.VISIBLE
            rvProductos.visibility = View.GONE
        } else {
            tvSinProductos.visibility = View.GONE
            rvProductos.visibility = View.VISIBLE
        }
    }

    private fun agregarAlCarrito(producto: Producto, cantidad: Int = 1) {
        val itemExistente = carrito[producto.id]
        val cantidadActual = itemExistente?.cantidad ?: 0

        if (cantidadActual + cantidad > producto.cantidad) {
            Toast.makeText(
                this,
                "No hay suficiente stock de ${producto.nombre}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (itemExistente != null) {
            itemExistente.cantidad += cantidad
        } else {
            carrito[producto.id] = CarritoItem(producto, cantidad)
        }
        actualizarCarritoUI()
    }

    private fun mostrarModalProducto(producto: Producto) {
        val cantidadEnCarrito = carrito[producto.id]?.cantidad ?: 0
        val disponible = producto.cantidad - cantidadEnCarrito

        if (disponible <= 0) {
            Toast.makeText(this, "No hay stock disponible de ${producto.nombre}", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_cantidad_producto, null)

        val ivFoto = dialogView.findViewById<ImageView>(R.id.ivModalFoto)
        val tvSinFoto = dialogView.findViewById<TextView>(R.id.tvModalSinFoto)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvModalNombre)
        val tvPrecioUnitario = dialogView.findViewById<TextView>(R.id.tvModalPrecioUnitario)
        val tvStock = dialogView.findViewById<TextView>(R.id.tvModalStock)
        val btnMenos = dialogView.findViewById<Button>(R.id.btnModalMenos)
        val btnMas = dialogView.findViewById<Button>(R.id.btnModalMas)
        val tvCantidad = dialogView.findViewById<TextView>(R.id.tvModalCantidad)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvModalTotal)

        tvNombre.text = producto.nombre
        tvPrecioUnitario.text = "$${"%.2f".format(producto.precio)} MXN c/u"
        tvStock.text = "Disponibles: $disponible"

        if (!producto.imagenUri.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(producto.imagenUri)
            if (bitmap != null) {
                ivFoto.setImageBitmap(bitmap)
                ivFoto.visibility = View.VISIBLE
                tvSinFoto.visibility = View.GONE
            }
        }

        var cantidad = 1

        fun actualizarUI() {
            tvCantidad.text = cantidad.toString()
            tvTotal.text = "Total: $${"%.2f".format(cantidad * producto.precio)} MXN"
            btnMenos.isEnabled = cantidad > 1
            btnMas.isEnabled = cantidad < disponible
        }

        actualizarUI()

        btnMenos.setOnClickListener {
            if (cantidad > 1) {
                cantidad--
                actualizarUI()
            }
        }

        btnMas.setOnClickListener {
            if (cantidad < disponible) {
                cantidad++
                actualizarUI()
            } else {
                Toast.makeText(this, "No hay mas stock disponible", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Agregar a la venta") { _, _ ->
                agregarAlCarrito(producto, cantidad)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpcionesItem(item: CarritoItem) {
        val opciones = arrayOf("Quitar uno", "Eliminar del carrito")
        AlertDialog.Builder(this)
            .setTitle(item.producto.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        item.cantidad -= 1
                        if (item.cantidad <= 0) {
                            carrito.remove(item.producto.id)
                        }
                        actualizarCarritoUI()
                    }
                    1 -> {
                        carrito.remove(item.producto.id)
                        actualizarCarritoUI()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarCarritoUI() {
        val lista = carrito.values.toList()
        carritoAdapter.actualizarLista(lista)

        val total = lista.sumOf { it.subtotal }
        tvTotal.text = "TOTAL = $${"%.2f".format(total)} MXN"

        if (lista.isEmpty()) {
            tvCarritoVacio.visibility = View.VISIBLE
            rvCarrito.visibility = View.GONE
            btnRealizar.isEnabled = false
        } else {
            tvCarritoVacio.visibility = View.GONE
            rvCarrito.visibility = View.VISIBLE
            btnRealizar.isEnabled = true
        }
    }

    private fun realizarVenta() {
        if (carrito.isEmpty()) return

        val total = carrito.values.sumOf { it.subtotal }
        val cantidadItems = carrito.values.sumOf { it.cantidad }

        AlertDialog.Builder(this)
            .setTitle("Confirmar venta")
            .setMessage(
                "Se venderán $cantidadItems artículo(s) por un total de " +
                        "$${"%.2f".format(total)} MXN.\n\n¿Continuar?"
            )
            .setPositiveButton("Realizar") { _, _ ->
                val ganancia = carrito.values.sumOf {
                    (it.producto.precio - it.producto.precioCompra) * it.cantidad
                }
                val venta = Venta(
                    total = total,
                    ganancia = ganancia,
                    totalArticulos = cantidadItems
                )
                val ventaId = db.ventaDao().insertarVenta(venta).toInt()
                val detalles = carrito.values.map { item ->
                    VentaDetalle(
                        ventaId = ventaId,
                        productoId = item.producto.id,
                        productoNombre = item.producto.nombre,
                        cantidad = item.cantidad,
                        precioUnitario = item.producto.precio,
                        precioCompra = item.producto.precioCompra,
                        subtotal = item.subtotal
                    )
                }
                db.ventaDao().insertarDetalles(detalles)
                for (item in carrito.values) {
                    val productoActualizado = item.producto.copy(
                        cantidad = item.producto.cantidad - item.cantidad
                    )
                    db.productoDao().actualizar(productoActualizado)
                }
                Toast.makeText(this, "Venta realizada con éxito", Toast.LENGTH_SHORT).show()
                carrito.clear()
                actualizarCarritoUI()
                filtrarProductos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}