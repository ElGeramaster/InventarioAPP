package com.example.inventario.HistorialVentas

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.inventario.AppDatabase
import com.example.inventario.BaseActivity
import com.example.inventario.ProductoVendidoResumen
import com.example.inventario.R
import com.example.inventario.Venta
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class HistorialVentasActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var ventaAdapter: VentaAdapter

    private lateinit var tvConteoVentas: TextView
    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalGanancia: TextView
    private lateinit var tvTituloGrafica: TextView
    private lateinit var barChart: BarChartView
    private lateinit var tvSinDatosGrafica: TextView
    private lateinit var layoutMasVendidos: LinearLayout
    private lateinit var layoutMenosVendidos: LinearLayout
    private lateinit var tvSinVentas: TextView
    private lateinit var rvVentas: RecyclerView

    private lateinit var btnSemana: Button
    private lateinit var btnMes: Button

    private enum class Filtro { SEMANA, MES }
    private var filtroActual = Filtro.SEMANA

    companion object {
        private const val MENU_LIMPIAR_TODO = 1
        private const val UN_DIA_MS = 24L * 60 * 60 * 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_ventas)

        db = AppDatabase.getInstance(this)

        tvConteoVentas = findViewById(R.id.tvConteoVentas)
        tvTotalIngresos = findViewById(R.id.tvTotalIngresos)
        tvTotalGanancia = findViewById(R.id.tvTotalGanancia)
        tvTituloGrafica = findViewById(R.id.tvTituloGrafica)
        barChart = findViewById(R.id.barChartVentas)
        tvSinDatosGrafica = findViewById(R.id.tvSinDatosGrafica)
        layoutMasVendidos = findViewById(R.id.layoutMasVendidos)
        layoutMenosVendidos = findViewById(R.id.layoutMenosVendidos)
        tvSinVentas = findViewById(R.id.tvSinVentas)
        rvVentas = findViewById(R.id.rvVentas)
        btnSemana = findViewById(R.id.btnSemana)
        btnMes = findViewById(R.id.btnMes)

        ventaAdapter = VentaAdapter(emptyList())
        rvVentas.layoutManager = LinearLayoutManager(this)
        rvVentas.adapter = ventaAdapter
        rvVentas.isNestedScrollingEnabled = false

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnMenuHistorial).setOnClickListener { vista ->
            val popup = PopupMenu(this, vista)
            popup.menu.add(0, MENU_LIMPIAR_TODO, 0, "Limpiar Todo")
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == MENU_LIMPIAR_TODO) {
                    confirmarEliminarTodo()
                    true
                } else {
                    false
                }
            }
            popup.show()
        }

        btnSemana.setOnClickListener { cambiarFiltro(Filtro.SEMANA) }
        btnMes.setOnClickListener { cambiarFiltro(Filtro.MES) }

        cargarDatos()
    }

    private fun cambiarFiltro(filtro: Filtro) {
        filtroActual = filtro
        actualizarBotonesFiltro()
        cargarDatos()
    }

    private fun actualizarBotonesFiltro() {
        val verde = "#43A047"
        val verdeClaro = "#E8F5E9"
        val blanco = "#FFFFFF"

        listOf(
            btnSemana to Filtro.SEMANA,
            btnMes to Filtro.MES
        ).forEach { (btn, filtro) ->
            if (filtro == filtroActual) {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(verde)
                )
                btn.setTextColor(android.graphics.Color.parseColor(blanco))
            } else {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(verdeClaro)
                )
                btn.setTextColor(android.graphics.Color.parseColor(verde))
            }
        }
    }

    private fun timestampDesde(): Long {
        val cal = Calendar.getInstance()
        when (filtroActual) {
            Filtro.SEMANA -> cal.add(Calendar.DAY_OF_YEAR, -6)
            Filtro.MES -> cal.add(Calendar.DAY_OF_YEAR, -29)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun cargarDatos() {
        val desde = timestampDesde()

        val conteo = db.ventaDao().obtenerConteoVentas(desde)
        val totalIngresos = db.ventaDao().obtenerTotalIngresos(desde)
        val totalGanancia = db.ventaDao().obtenerTotalGanancia(desde)
        val ventas = db.ventaDao().obtenerVentasDesde(desde)
        val masVendidos = db.ventaDao().obtenerMasVendidos(desde, 3)
        val menosVendidos = db.ventaDao().obtenerMenosVendidos(desde, 3)
        val ventasGrafica = db.ventaDao().obtenerVentasParaGrafica(desde)

        tvConteoVentas.text = conteo.toString()
        tvTotalIngresos.text = "$${"%.0f".format(totalIngresos)}"
        tvTotalGanancia.text = "$${"%.0f".format(totalGanancia)}"

        actualizarGrafica(ventasGrafica)
        actualizarProductosDestacados(masVendidos, menosVendidos)
        actualizarListaVentas(ventas)
    }

    private fun actualizarGrafica(ventas: List<Venta>) {
        if (ventas.isEmpty()) {
            barChart.visibility = View.GONE
            tvSinDatosGrafica.visibility = View.VISIBLE
            return
        }

        barChart.visibility = View.VISIBLE
        tvSinDatosGrafica.visibility = View.GONE

        val entries: List<BarChartView.BarEntry> = when (filtroActual) {
            Filtro.SEMANA -> generarEntradasDiarias(ventas, 7)
            Filtro.MES -> generarEntradasSemanales(ventas, 5)
        }

        val titulo = when (filtroActual) {
            Filtro.SEMANA -> "Ingresos por día (última semana)"
            Filtro.MES -> "Ingresos por semana (último mes)"
        }
        tvTituloGrafica.text = titulo
        barChart.setData(entries)
    }

    private fun generarEntradasDiarias(ventas: List<Venta>, dias: Int): List<BarChartView.BarEntry> {
        val entradas = mutableListOf<BarChartView.BarEntry>()
        val fmtDia = java.text.SimpleDateFormat("dd/MM", Locale("es", "MX"))
        val fmtClave = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val ventasPorDia = ventas.groupBy { venta ->
            val c = Calendar.getInstance()
            c.timeInMillis = venta.timestamp
            fmtClave.format(c.time)
        }

        for (i in (dias - 1) downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val clave = fmtClave.format(dayCal.time)
            val label = fmtDia.format(dayCal.time)
            val total = ventasPorDia[clave]?.sumOf { it.total }?.toFloat() ?: 0f
            entradas.add(BarChartView.BarEntry(label, total))
        }
        return entradas
    }

    /**
     * Agrupa las ventas del mes en bloques de 7 días (semanas) terminando hoy.
     * Así la gráfica mensual muestra pocas barras bien separadas en lugar de
     * 30 columnas amontonadas. Cada barra se etiqueta con la fecha de inicio
     * de esa semana.
     */
    private fun generarEntradasSemanales(ventas: List<Venta>, semanas: Int): List<BarChartView.BarEntry> {
        val fmtLabel = java.text.SimpleDateFormat("d/M", Locale("es", "MX"))

        val hoy = Calendar.getInstance()
        hoy.set(Calendar.HOUR_OF_DAY, 0)
        hoy.set(Calendar.MINUTE, 0)
        hoy.set(Calendar.SECOND, 0)
        hoy.set(Calendar.MILLISECOND, 0)
        val hoyMedianoche = hoy.timeInMillis

        val totales = DoubleArray(semanas)
        for (venta in ventas) {
            val c = Calendar.getInstance()
            c.timeInMillis = venta.timestamp
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val diasAtras = ((hoyMedianoche - c.timeInMillis) / UN_DIA_MS).toInt()
            if (diasAtras < 0) continue
            val indice = diasAtras / 7
            if (indice in 0 until semanas) {
                totales[indice] += venta.total
            }
        }

        val entradas = mutableListOf<BarChartView.BarEntry>()
        for (i in (semanas - 1) downTo 0) {
            val inicio = Calendar.getInstance()
            inicio.add(Calendar.DAY_OF_YEAR, -(i * 7 + 6))
            val label = fmtLabel.format(inicio.time)
            entradas.add(BarChartView.BarEntry(label, totales[i].toFloat()))
        }
        return entradas
    }

    private fun actualizarProductosDestacados(
        mas: List<ProductoVendidoResumen>,
        menos: List<ProductoVendidoResumen>
    ) {
        layoutMasVendidos.removeAllViews()
        layoutMenosVendidos.removeAllViews()

        fun agregarFila(layout: LinearLayout, item: ProductoVendidoResumen, rank: Int) {
            val row = layoutInflater.inflate(android.R.layout.simple_list_item_2, layout, false)
            val text1 = row.findViewById<TextView>(android.R.id.text1)
            val text2 = row.findViewById<TextView>(android.R.id.text2)
            text1.text = "$rank. ${item.productoNombre}"
            text2.text = "${item.totalCantidad} unidades vendidas"
            text1.textSize = 16f
            text2.textSize = 14f
            layout.addView(row)
        }

        if (mas.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Sin datos"
            tv.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            tv.textSize = 14f
            tv.setPadding(8, 4, 8, 4)
            layoutMasVendidos.addView(tv)
        } else {
            mas.forEachIndexed { i, item -> agregarFila(layoutMasVendidos, item, i + 1) }
        }

        if (menos.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Sin datos"
            tv.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            tv.textSize = 14f
            tv.setPadding(8, 4, 8, 4)
            layoutMenosVendidos.addView(tv)
        } else {
            menos.forEachIndexed { i, item -> agregarFila(layoutMenosVendidos, item, i + 1) }
        }
    }

    private fun actualizarListaVentas(ventas: List<Venta>) {
        ventaAdapter.actualizar(ventas)
        if (ventas.isEmpty()) {
            tvSinVentas.visibility = View.VISIBLE
            rvVentas.visibility = View.GONE
        } else {
            tvSinVentas.visibility = View.GONE
            rvVentas.visibility = View.VISIBLE
        }
    }

    private fun confirmarEliminarTodo() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar historial")
            .setMessage("¿Eliminar todo el historial de ventas? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar todo") { _, _ ->
                db.ventaDao().eliminarTodosDetalles()
                db.ventaDao().eliminarTodas()
                cargarDatos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
