package com.example.inventario.HistorialVentas

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.AppDatabase
import com.example.inventario.ProductoVendidoResumen
import com.example.inventario.R
import com.example.inventario.Venta
import java.util.*

class HistorialVentasActivity : AppCompatActivity() {

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

    private lateinit var btn7Dias: Button
    private lateinit var btnMes: Button
    private lateinit var btnAnio: Button
    private lateinit var btnTodo: Button

    private enum class Filtro { SIETE_DIAS, MES, ANIO, TODO }
    private var filtroActual = Filtro.SIETE_DIAS

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
        btn7Dias = findViewById(R.id.btn7Dias)
        btnMes = findViewById(R.id.btnMes)
        btnAnio = findViewById(R.id.btnAnio)
        btnTodo = findViewById(R.id.btnTodo)

        ventaAdapter = VentaAdapter(emptyList())
        rvVentas.layoutManager = LinearLayoutManager(this)
        rvVentas.adapter = ventaAdapter
        rvVentas.isNestedScrollingEnabled = false

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnEliminarHistorial).setOnClickListener {
            confirmarEliminarTodo()
        }

        btn7Dias.setOnClickListener { cambiarFiltro(Filtro.SIETE_DIAS) }
        btnMes.setOnClickListener { cambiarFiltro(Filtro.MES) }
        btnAnio.setOnClickListener { cambiarFiltro(Filtro.ANIO) }
        btnTodo.setOnClickListener { cambiarFiltro(Filtro.TODO) }

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
            btn7Dias to Filtro.SIETE_DIAS,
            btnMes to Filtro.MES,
            btnAnio to Filtro.ANIO,
            btnTodo to Filtro.TODO
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

    private fun timestampDesde(): Long? {
        val cal = Calendar.getInstance()
        return when (filtroActual) {
            Filtro.SIETE_DIAS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            Filtro.MES -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            Filtro.ANIO -> {
                cal.add(Calendar.DAY_OF_YEAR, -364)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            Filtro.TODO -> null
        }
    }

    private fun cargarDatos() {
        val desde = timestampDesde()

        val conteo: Int
        val totalIngresos: Double
        val totalGanancia: Double
        val ventas: List<Venta>
        val masVendidos: List<ProductoVendidoResumen>
        val menosVendidos: List<ProductoVendidoResumen>
        val ventasGrafica: List<Venta>

        if (desde != null) {
            conteo = db.ventaDao().obtenerConteoVentas(desde)
            totalIngresos = db.ventaDao().obtenerTotalIngresos(desde)
            totalGanancia = db.ventaDao().obtenerTotalGanancia(desde)
            ventas = db.ventaDao().obtenerVentasDesde(desde)
            masVendidos = db.ventaDao().obtenerMasVendidos(desde, 3)
            menosVendidos = db.ventaDao().obtenerMenosVendidos(desde, 3)
            ventasGrafica = db.ventaDao().obtenerVentasParaGrafica(desde)
        } else {
            conteo = db.ventaDao().obtenerConteoVentasGlobal()
            totalIngresos = db.ventaDao().obtenerTotalIngresosGlobal()
            totalGanancia = db.ventaDao().obtenerTotalGananciaGlobal()
            ventas = db.ventaDao().obtenerTodas()
            masVendidos = db.ventaDao().obtenerMasVendidosGlobal(3)
            menosVendidos = db.ventaDao().obtenerMenosVendidosGlobal(3)
            ventasGrafica = db.ventaDao().obtenerTodasParaGrafica()
        }

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
            Filtro.SIETE_DIAS -> generarEntradaDiarias(ventas, 7)
            Filtro.MES -> generarEntradaDiarias(ventas, 30)
            Filtro.ANIO -> generarEntradasMensuales(ventas, 12)
            Filtro.TODO -> generarEntradasMensuales(ventas, null)
        }

        val titulo = when (filtroActual) {
            Filtro.SIETE_DIAS -> "Ingresos por día (últimos 7 días)"
            Filtro.MES -> "Ingresos por día (último mes)"
            Filtro.ANIO -> "Ingresos por mes (último año)"
            Filtro.TODO -> "Ingresos por mes (histórico)"
        }
        tvTituloGrafica.text = titulo
        barChart.setData(entries)
    }

    private fun generarEntradaDiarias(ventas: List<Venta>, dias: Int): List<BarChartView.BarEntry> {
        val cal = Calendar.getInstance()
        val dias_entries = mutableListOf<BarChartView.BarEntry>()
        val fmtDia = java.text.SimpleDateFormat("dd/MM", Locale("es", "MX"))
        val fmtClave = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val groupByDay = ventas.groupBy { venta ->
            val c = Calendar.getInstance()
            c.timeInMillis = venta.timestamp
            fmtClave.format(c.time)
        }

        val limite = if (dias > 30) 30 else dias
        for (i in (limite - 1) downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val clave = fmtClave.format(dayCal.time)
            val label = fmtDia.format(dayCal.time)
            val total = groupByDay[clave]?.sumOf { it.total }?.toFloat() ?: 0f
            dias_entries.add(BarChartView.BarEntry(label, total))
        }
        return dias_entries
    }

    private fun generarEntradasMensuales(ventas: List<Venta>, meses: Int?): List<BarChartView.BarEntry> {
        val fmtMes = java.text.SimpleDateFormat("MMM", Locale("es", "MX"))
        val fmtClave = java.text.SimpleDateFormat("yyyyMM", Locale.getDefault())

        val groupByMonth = ventas.groupBy { venta ->
            val c = Calendar.getInstance()
            c.timeInMillis = venta.timestamp
            fmtClave.format(c.time)
        }

        val entries = mutableListOf<BarChartView.BarEntry>()
        if (meses != null) {
            for (i in (meses - 1) downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -i)
                val clave = fmtClave.format(c.time)
                val label = fmtMes.format(c.time).replaceFirstChar { it.uppercase() }
                val total = groupByMonth[clave]?.sumOf { it.total }?.toFloat() ?: 0f
                entries.add(BarChartView.BarEntry(label, total))
            }
        } else {
            val claves = groupByMonth.keys.sorted()
            claves.forEach { clave ->
                val year = clave.substring(0, 4).toInt()
                val month = clave.substring(4, 6).toInt() - 1
                val c = Calendar.getInstance()
                c.set(year, month, 1)
                val label = fmtMes.format(c.time).replaceFirstChar { it.uppercase() }
                val total = groupByMonth[clave]?.sumOf { it.total }?.toFloat() ?: 0f
                entries.add(BarChartView.BarEntry(label, total))
            }
        }
        return entries
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
            text1.textSize = 13f
            text2.textSize = 11f
            layout.addView(row)
        }

        if (mas.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Sin datos"
            tv.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            tv.textSize = 12f
            tv.setPadding(8, 4, 8, 4)
            layoutMasVendidos.addView(tv)
        } else {
            mas.forEachIndexed { i, item -> agregarFila(layoutMasVendidos, item, i + 1) }
        }

        if (menos.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Sin datos"
            tv.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            tv.textSize = 12f
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
