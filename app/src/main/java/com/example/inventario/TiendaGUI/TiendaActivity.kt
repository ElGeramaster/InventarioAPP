package com.example.inventario.TiendaGUI

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.AppDatabase
import com.example.inventario.BaseActivity
import com.example.inventario.HistorialVentas.HistorialVentasActivity
import com.example.inventario.LogoManager
import com.example.inventario.MainActivity
import com.example.inventario.NotificationHelper
import com.example.inventario.Producto
import com.example.inventario.R
import com.example.inventario.ReportesActivity
import com.example.inventario.Venta
import com.example.inventario.VentaDetalle

class TiendaActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var rvCategorias: RecyclerView
    private lateinit var rvProductos: RecyclerView
    private lateinit var rvCarrito: RecyclerView
    private lateinit var cardBuscar: CardView
    private lateinit var etBuscar: EditText
    private lateinit var btnToggleBuscar: ImageButton
    private lateinit var btnRealizar: Button
    private lateinit var tvSinProductos: TextView
    private lateinit var tvCarritoVacio: TextView
    private lateinit var fabEscanear: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var dotMenuStock: View
    private lateinit var ivLogoTienda: ImageView

    private lateinit var categoriaAdapter: CategoriaTiendaAdapter
    private lateinit var productoAdapter: ProductoTiendaAdapter
    private lateinit var carritoAdapter: CarritoItemAdapter

    // Estado del carrito: clave ("idProducto-U" pieza / "idProducto-P" peso) -> CarritoItem
    private val carrito = linkedMapOf<String, CarritoItem>()

    private var categoriaSeleccionada: String = CATEGORIA_TODOS
    private var busquedaActual: String = ""

    private val escanearCodigoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val codigos = result.data?.getStringArrayListExtra(ContinuousScanActivity.EXTRA_CODIGOS)
            if (!codigos.isNullOrEmpty()) {
                procesarCodigosEscaneados(codigos)
            }
        }
    }

    private val seleccionarLogoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (LogoManager.guardarDesdeUri(this, uri)) {
                Toast.makeText(this, "Logo actualizado", Toast.LENGTH_SHORT).show()
                mostrarLogoTienda()
            } else {
                Toast.makeText(this, "No se pudo guardar el logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CATEGORIA_TODOS = "Todos los productos"
        private const val CATEGORIA_FAVORITOS = "Favoritos"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tienda)

        db = AppDatabase.getInstance(this)
        drawerLayout = findViewById(R.id.drawerLayout)

        rvCategorias    = findViewById(R.id.rvCategorias)
        rvProductos     = findViewById(R.id.rvProductosTienda)
        rvCarrito       = findViewById(R.id.rvCarrito)
        cardBuscar      = findViewById(R.id.cardBuscar)
        etBuscar        = findViewById(R.id.etBuscarTienda)
        btnToggleBuscar = findViewById(R.id.btnToggleBuscar)
        btnRealizar     = findViewById(R.id.btnRealizarVenta)
        tvSinProductos  = findViewById(R.id.tvSinProductos)
        tvCarritoVacio  = findViewById(R.id.tvCarritoVacio)
        fabEscanear     = findViewById(R.id.fabEscanear)
        dotMenuStock    = findViewById(R.id.dotMenuStock)
        ivLogoTienda    = findViewById(R.id.ivLogoTienda)

        configurarCategorias()
        configurarProductos()
        configurarCarrito()
        configurarBuscador()
        configurarMenuLateral()

        btnRealizar.setOnClickListener {
            realizarVenta()
        }

        fabEscanear.setOnClickListener {
            escanearCodigo()
        }

        findViewById<ImageButton>(R.id.btnMenuHamburger).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onResume() {
        super.onResume()
        recargarCategorias()
        filtrarProductos()
        actualizarIndicadorStock()
        mostrarLogoTienda()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) ->
                drawerLayout.closeDrawer(GravityCompat.START)
            carrito.isNotEmpty() ->
                confirmarSalida { finish() }
            else ->
                super.onBackPressed()
        }
    }

    /**
     * Si hay productos en el carrito, pide confirmación antes de salir para
     * no perder la venta en curso. Si el carrito está vacío, ejecuta la acción
     * directamente.
     */
    private fun confirmarSalida(accion: () -> Unit) {
        if (carrito.isEmpty()) {
            accion()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("¿Salir de la venta?")
            .setMessage(
                "Tienes productos en el carrito. Si sales ahora se perderá " +
                        "la venta en curso.\n\n¿Deseas salir?"
            )
            .setPositiveButton("Salir") { _, _ -> accion() }
            .setNegativeButton("Seguir en la venta", null)
            .show()
    }

    private fun configurarMenuLateral() {
        findViewById<TextView>(R.id.menuInventario).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            confirmarSalida { startActivity(Intent(this, MainActivity::class.java)) }
        }

        findViewById<TextView>(R.id.menuReportes).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            confirmarSalida { startActivity(Intent(this, ReportesActivity::class.java)) }
        }

        findViewById<TextView>(R.id.menuHistorial).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            confirmarSalida { startActivity(Intent(this, HistorialVentasActivity::class.java)) }
        }

        findViewById<TextView>(R.id.menuProveedores).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Proveedores - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.menuFiados).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this, "Fiados a clientes - Próximamente", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.menuAjustes).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            abrirAjustes()
        }
    }

    /**
     * Muestra un punto rojo en el botón de menú y en "Mi mercancía y reportes"
     * cuando hay productos con stock bajo o agotado.
     */
    private fun actualizarIndicadorStock() {
        val hayStockBajo = db.productoDao().obtenerStockBajo().isNotEmpty()

        dotMenuStock.visibility = if (hayStockBajo) View.VISIBLE else View.GONE

        val menuReportes = findViewById<TextView>(R.id.menuReportes)
        val icono = if (hayStockBajo) R.drawable.ic_dot_alerta else 0
        menuReportes.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, icono, 0)
        menuReportes.compoundDrawablePadding = 12
    }

    private fun mostrarLogoTienda() {
        val path = LogoManager.obtenerPath(this)
        if (path != null) {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                ivLogoTienda.setImageBitmap(bitmap)
                ivLogoTienda.visibility = View.VISIBLE
                return
            }
        }
        ivLogoTienda.visibility = View.GONE
    }

    private fun abrirAjustes() {
        val opciones = if (LogoManager.hayLogo(this)) {
            arrayOf("Cambiar logo (galería)", "Quitar logo")
        } else {
            arrayOf("Poner logo (galería)")
        }
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setItems(opciones) { _, which ->
                when {
                    which == 0 -> seleccionarLogoLauncher.launch("image/*")
                    else -> {
                        LogoManager.quitar(this)
                        mostrarLogoTienda()
                        Toast.makeText(this, "Logo quitado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        val lista = mutableListOf(CATEGORIA_TODOS, CATEGORIA_FAVORITOS)
        lista.addAll(db.productoDao().obtenerCategorias())
        return lista
    }

    private fun configurarProductos() {
        rvProductos.layoutManager = GridLayoutManager(this, 3)
        productoAdapter = ProductoTiendaAdapter(
            emptyList(),
            onAgregar = { producto ->
                if (producto.vendePorPeso) mostrarOpcionesVenta(producto)
                else agregarAlCarrito(producto)
            },
            onVerDetalle = { producto ->
                if (producto.vendePorPeso) mostrarOpcionesVenta(producto)
                else mostrarModalProducto(producto)
            }
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
            categoriaSeleccionada == CATEGORIA_FAVORITOS && busquedaActual.isEmpty() ->
                db.productoDao().obtenerFavoritos()
            categoriaSeleccionada == CATEGORIA_FAVORITOS ->
                db.productoDao().buscarFavoritos(busquedaActual)
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

    // --- Claves del carrito (un producto puede tener línea por pieza y por peso) ---
    private fun clavePieza(id: Int) = "$id-U"
    private fun clavePeso(id: Int) = "$id-P"
    private fun claveDe(item: CarritoItem) =
        if (item.porPeso) clavePeso(item.producto.id) else clavePieza(item.producto.id)

    private fun agregarAlCarrito(
        producto: Producto,
        cantidad: Int = 1,
        mostrarMensajeStock: Boolean = true
    ): Boolean {
        val clave = clavePieza(producto.id)
        val itemExistente = carrito[clave]
        val cantidadActual = itemExistente?.cantidad ?: 0

        if (cantidadActual + cantidad > producto.cantidad) {
            if (mostrarMensajeStock) {
                Toast.makeText(
                    this,
                    "No hay suficiente stock de ${producto.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return false
        }

        if (itemExistente != null) {
            itemExistente.cantidad += cantidad
        } else {
            carrito[clave] = CarritoItem(producto, cantidad = cantidad, porPeso = false)
        }
        actualizarCarritoUI()
        return true
    }

    /** Agrega [gramos] de un producto que se vende por peso (sin control de stock). */
    private fun agregarPesoAlCarrito(producto: Producto, gramos: Int) {
        if (gramos <= 0) return
        val clave = clavePeso(producto.id)
        val itemExistente = carrito[clave]
        if (itemExistente != null) {
            itemExistente.gramos += gramos
        } else {
            carrito[clave] = CarritoItem(producto, gramos = gramos, porPeso = true)
        }
        actualizarCarritoUI()
    }

    /**
     * Para frutas y verduras: si el producto también se vende por pieza pregunta
     * cómo venderlo; si solo es por peso, abre directo el selector de kilos.
     */
    private fun mostrarOpcionesVenta(producto: Producto) {
        if (producto.seVendePorPieza && producto.precio > 0) {
            AlertDialog.Builder(this)
                .setTitle(producto.nombre)
                .setItems(arrayOf("Por pieza", "Por peso (kg)")) { _, which ->
                    when (which) {
                        0 -> mostrarModalProducto(producto)
                        1 -> mostrarDialogoPeso(producto)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            mostrarDialogoPeso(producto)
        }
    }

    private fun mostrarDialogoPeso(producto: Producto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_peso_producto, null)

        val tvNombre = dialogView.findViewById<TextView>(R.id.tvPesoNombre)
        val tvPrecioKilo = dialogView.findViewById<TextView>(R.id.tvPesoPrecioKilo)
        val btnCuarto = dialogView.findViewById<Button>(R.id.btnCuartoKilo)
        val btnMedio = dialogView.findViewById<Button>(R.id.btnMedioKilo)
        val btnKilo = dialogView.findViewById<Button>(R.id.btnUnKilo)
        val etKg = dialogView.findViewById<EditText>(R.id.etPesoKg)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvPesoTotal)

        tvNombre.text = producto.nombre
        tvPrecioKilo.text = "$${"%.2f".format(producto.precioKilo)} / kg"

        fun gramosActuales(): Int {
            val kg = etKg.text.toString().trim().toDoubleOrNull() ?: 0.0
            return Math.round(kg * 1000).toInt()
        }

        fun actualizarTotal() {
            val total = producto.precioKilo * gramosActuales() / 1000.0
            tvTotal.text = "Total: $${"%.2f".format(total)} MXN"
        }

        etKg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarTotal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCuarto.setOnClickListener { etKg.setText("0.25") }
        btnMedio.setOnClickListener { etKg.setText("0.5") }
        btnKilo.setOnClickListener { etKg.setText("1") }

        actualizarTotal()

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Agregar a la venta") { _, _ ->
                val gramos = gramosActuales()
                if (gramos <= 0) {
                    Toast.makeText(this, "Indica una cantidad en kg", Toast.LENGTH_SHORT).show()
                } else {
                    agregarPesoAlCarrito(producto, gramos)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarModalProducto(producto: Producto) {
        val cantidadEnCarrito = carrito[clavePieza(producto.id)]?.cantidad ?: 0
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
        if (item.porPeso) {
            // Las líneas por peso solo se pueden quitar completas.
            AlertDialog.Builder(this)
                .setTitle("${item.producto.nombre} (${item.textoCantidad})")
                .setItems(arrayOf("Eliminar del carrito")) { _, _ ->
                    carrito.remove(claveDe(item))
                    actualizarCarritoUI()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        val opciones = arrayOf("Quitar uno", "Eliminar del carrito")
        AlertDialog.Builder(this)
            .setTitle(item.producto.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        item.cantidad -= 1
                        if (item.cantidad <= 0) {
                            carrito.remove(claveDe(item))
                        }
                        actualizarCarritoUI()
                    }
                    1 -> {
                        carrito.remove(claveDe(item))
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
        btnRealizar.text = "REALIZAR VENTA - $${"%.2f".format(total)} MXN"

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

    private fun escanearCodigo() {
        escanearCodigoLauncher.launch(Intent(this, ContinuousScanActivity::class.java))
    }

    private fun procesarCodigosEscaneados(codigos: List<String>) {
        if (codigos.isEmpty()) return

        var agregados = 0
        var noEncontrados = 0
        var sinStock = 0

        for (codigo in codigos) {
            val producto = db.productoDao().buscarPorCodigoBarras(codigo)
            when {
                producto == null -> noEncontrados++
                agregarAlCarrito(producto, mostrarMensajeStock = false) -> agregados++
                else -> sinStock++
            }
        }

        val mensaje = buildString {
            append(
                if (agregados > 0) "$agregados producto(s) agregado(s) al carrito"
                else "No se agregaron productos"
            )
            if (sinStock > 0) append(" · $sinStock sin stock suficiente")
            if (noEncontrados > 0) append(" · $noEncontrados sin coincidencia")
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun realizarVenta() {
        if (carrito.isEmpty()) return

        val total = carrito.values.sumOf { it.subtotal }
        // Cada línea por peso cuenta como 1 artículo; las de pieza cuentan sus unidades.
        val cantidadItems = carrito.values.sumOf { if (it.porPeso) 1 else it.cantidad }

        AlertDialog.Builder(this)
            .setTitle("Confirmar venta")
            .setMessage(
                "Se venderán $cantidadItems artículo(s) por un total de " +
                        "$${"%.2f".format(total)} MXN.\n\n¿Continuar?"
            )
            .setPositiveButton("Realizar") { _, _ ->
                val ganancia = carrito.values.sumOf { item ->
                    if (item.porPeso) {
                        (item.producto.precioKilo - item.producto.precioCompraKilo) * item.gramos / 1000.0
                    } else {
                        (item.producto.precio - item.producto.precioCompra) * item.cantidad
                    }
                }
                val venta = Venta(
                    total = total,
                    ganancia = ganancia,
                    totalArticulos = cantidadItems
                )
                val ventaId = db.ventaDao().insertarVenta(venta).toInt()
                val detalles = carrito.values.map { item ->
                    if (item.porPeso) {
                        VentaDetalle(
                            ventaId = ventaId,
                            productoId = item.producto.id,
                            productoNombre = "${item.producto.nombre} (${item.textoCantidad})",
                            cantidad = 1,
                            precioUnitario = item.producto.precioKilo,
                            precioCompra = item.producto.precioCompraKilo,
                            subtotal = item.subtotal
                        )
                    } else {
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
                }
                db.ventaDao().insertarDetalles(detalles)
                // Solo las ventas por pieza descuentan stock.
                for (item in carrito.values) {
                    if (item.porPeso) continue
                    val productoActualizado = item.producto.copy(
                        cantidad = item.producto.cantidad - item.cantidad
                    )
                    db.productoDao().actualizar(productoActualizado)
                }
                Toast.makeText(this, "Venta realizada con éxito", Toast.LENGTH_SHORT).show()
                NotificationHelper.verificarYNotificarStockBajo(this)
                carrito.clear()
                actualizarCarritoUI()
                filtrarProductos()
                actualizarIndicadorStock()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}