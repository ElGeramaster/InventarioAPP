package com.example.inventario

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView

class ReportesActivity : BaseActivity() {

    private lateinit var tvTotalProductos: TextView
    private lateinit var tvValorTotal: TextView
    private lateinit var tvValorCompra: TextView
    private lateinit var tvGanancia: TextView
    private lateinit var btnVerStockBajo: TextView
    private lateinit var btnVerPorCaducar: TextView
    private lateinit var db: AppDatabase
    private lateinit var stockBajoFragment: StockBajoFragment
    private lateinit var porCaducarFragment: PorCaducarFragment

    private enum class Vista { STOCK_BAJO, POR_CADUCAR }
    private var vistaActual = Vista.STOCK_BAJO

    private companion object {
        const val TAG_STOCK_BAJO = "fragment_stock_bajo"
        const val TAG_POR_CADUCAR = "fragment_por_caducar"
        const val ESTADO_VISTA = "vista_actual"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        // Recuperar la pestaña elegida si la actividad se recreó (girar pantalla,
        // cambio de tema...) para no volver siempre a "bajo stock".
        savedInstanceState?.getString(ESTADO_VISTA)?.let { guardada ->
            vistaActual = runCatching { Vista.valueOf(guardada) }.getOrDefault(Vista.STOCK_BAJO)
        }

        title = "Reportes"

        db = AppDatabase.getInstance(this)

        tvTotalProductos = findViewById(R.id.tvTotalProductos)
        tvValorTotal     = findViewById(R.id.tvValorTotal)
        tvValorCompra    = findViewById(R.id.tvValorCompra)
        tvGanancia       = findViewById(R.id.tvGanancia)
        btnVerStockBajo  = findViewById(R.id.btnVerStockBajo)
        btnVerPorCaducar = findViewById(R.id.btnVerPorCaducar)

        btnVerStockBajo.setOnClickListener { cambiarVista(Vista.STOCK_BAJO) }
        btnVerPorCaducar.setOnClickListener { cambiarVista(Vista.POR_CADUCAR) }

        cargarFragmentos()
        cargarResumen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_VISTA, vistaActual.name)
    }

    override fun onResume() {
        super.onResume()
        cargarResumen()
        stockBajoFragment.recargar()
        porCaducarFragment.recargar()
    }

    /**
     * Deja los dos fragments dentro del contenedor mostrando solo el de la
     * pestaña activa.
     *
     * Si la actividad se recrea, el FragmentManager ya restauró los fragments
     * anteriores, así que hay que reutilizarlos: crear otros nuevos los apilaba
     * en el mismo contenedor y las dos listas se dibujaban una encima de otra.
     */
    private fun cargarFragmentos() {
        val fm = supportFragmentManager
        val restauradoStock = fm.findFragmentByTag(TAG_STOCK_BAJO) as? StockBajoFragment
        val restauradoCaducar = fm.findFragmentByTag(TAG_POR_CADUCAR) as? PorCaducarFragment

        stockBajoFragment = restauradoStock ?: StockBajoFragment()
        porCaducarFragment = restauradoCaducar ?: PorCaducarFragment()

        val transaction = fm.beginTransaction()
        if (restauradoStock == null) {
            transaction.add(R.id.fragmentContainer, stockBajoFragment, TAG_STOCK_BAJO)
        }
        if (restauradoCaducar == null) {
            transaction.add(R.id.fragmentContainer, porCaducarFragment, TAG_POR_CADUCAR)
        }
        if (vistaActual == Vista.STOCK_BAJO) {
            transaction.show(stockBajoFragment).hide(porCaducarFragment)
        } else {
            transaction.show(porCaducarFragment).hide(stockBajoFragment)
        }
        transaction.commit()

        actualizarBotonesToggle()
    }

    private fun cambiarVista(vista: Vista) {
        if (vista == vistaActual) return
        vistaActual = vista
        actualizarVistaVisible()
    }

    private fun actualizarVistaVisible() {
        val transaction = supportFragmentManager.beginTransaction()
        if (vistaActual == Vista.STOCK_BAJO) {
            transaction.show(stockBajoFragment).hide(porCaducarFragment)
        } else {
            transaction.show(porCaducarFragment).hide(stockBajoFragment)
        }
        transaction.commit()
        actualizarBotonesToggle()
    }

    private fun actualizarBotonesToggle() {
        val rojoActivo = "#C62828"
        val naranjaActivo = "#E65100"
        val inactivoFondo = "#F5F5F5"
        val inactivoTexto = "#757575"

        if (vistaActual == Vista.STOCK_BAJO) {
            btnVerStockBajo.backgroundTintList = ColorStateList.valueOf(Color.parseColor(rojoActivo))
            btnVerStockBajo.setTextColor(Color.WHITE)
            btnVerPorCaducar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(inactivoFondo))
            btnVerPorCaducar.setTextColor(Color.parseColor(inactivoTexto))
        } else {
            btnVerPorCaducar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(naranjaActivo))
            btnVerPorCaducar.setTextColor(Color.WHITE)
            btnVerStockBajo.backgroundTintList = ColorStateList.valueOf(Color.parseColor(inactivoFondo))
            btnVerStockBajo.setTextColor(Color.parseColor(inactivoTexto))
        }
    }

    private fun cargarResumen() {
        val productos    = db.productoDao().obtenerTodos()
        val stockBajo    = db.productoDao().obtenerStockBajo()
        val limiteCaducidad = NotificationHelper.inicioDeHoy() +
                NotificationHelper.DIAS_ALERTA_CADUCIDAD * 24L * 60 * 60 * 1000
        val porCaducar   = db.productoDao().obtenerPorCaducar(limiteCaducidad)

        val totalProductos = productos.size
        val valorVenta     = productos.sumOf { it.precio * it.cantidad }
        val valorCompra    = productos.sumOf { it.precioCompra * it.cantidad }
        val ganancia       = valorVenta - valorCompra

        tvTotalProductos.text = totalProductos.toString()
        tvValorTotal.text     = "$${"%.0f".format(valorVenta)}"
        tvValorCompra.text    = "$${"%.0f".format(valorCompra)}"
        tvGanancia.text       = "$${"%.0f".format(ganancia)}"

        btnVerStockBajo.text = "${stockBajo.size}\nBAJO STOCK"
        btnVerPorCaducar.text = "${porCaducar.size}\nPOR CADUCAR"
        actualizarBotonesToggle()
    }
}