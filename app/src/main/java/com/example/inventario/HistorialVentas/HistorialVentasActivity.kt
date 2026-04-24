package com.example.inventario.HistorialVentas

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
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
import com.example.inventario.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistorialVentasActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var rvVentas: RecyclerView
    private lateinit var ventaAdapter: VentaAdapter
    private lateinit var tvConteoVentas: TextView
    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalGanancia: TextView
    private lateinit var layoutMasVendidos: LinearLayout
    private lateinit var layoutMenosVendidos: LinearLayout
    private lateinit var tvSinVentas: TextView

    private lateinit var btn7Dias: Button
    private lateinit var btnMes: Button
    private lateinit var btnAnio: Button
    private lateinit var btnTodo: Button

    private enum class Filtro { SIETE_DIAS, MES, ANIO, TODO }
    private var filtroActual = Filtro.SIETE_DIAS

    private val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale("es", "MX"))

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_ventas)

        db = AppDatabase.getInstance(this)

        tvConteoVentas   = findViewById(R.id.tvConteoVentas)
        tvTotalIngresos  = findViewById(R.id.tvTotalIngresos)
        tvTotalGanancia  = findViewById(R.id.tvTotalGanancia)
        layoutMasVendidos   = findViewById(R.id.layoutMasVendidos)
        layoutMenosVendidos = findViewById(R.id.layoutMenosVendidos)
        tvSinVentas      = findViewById(R.id.tvSinVentas)
        btn7Dias = findViewById(R.id.btn7Dias)
        btnMes   = findViewById(R.id.btnMes)
        btnAnio  = findViewById(R.id.btnAnio)
        btnTodo  = findViewById(R.id.btnTodo)

        rvVentas = findViewById(R.id.rvVentas)
        rvVentas.layoutManager = LinearLayoutManager(this)
        ventaAdapter = VentaAdapter(emptyList()) { venta -> mostrarDetalleVenta(venta) }
        rvVentas.adapter = ventaAdapter
        rvVentas.isNestedScrollingEnabled = false

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        btn7Dias.setOnClickListener { seleccionarFiltro(Filtro.SIETE_DIAS) }
        btnMes.setOnClickListener   { seleccionarFiltro(Filtro.MES) }
        btnAnio.setOnClickListener  { seleccionarFiltro(Filtro.ANIO) }
        btnTodo.setOnClickListener  { seleccionarFiltro(Filtro.TODO) }

        seleccionarFiltro(Filtro.SIETE_DIAS)
    }

    private fun seleccionarFiltro(filtro: Filtro) {
        filtroActual = filtro
        val botones = listOf(btn7Dias, btnMes, btnAnio, btnTodo)
        val filtros = listOf(Filtro.SIETE_DIAS, Filtro.MES, Filtro.ANIO, Filtro.TODO)
        botones.forEachIndexed { i, btn ->
            val sel = filtros[i] == filtro
            btn.backgroundTintList = ColorStateList.valueOf(if (sel) 0xFF43A047.toInt() else 0xFFE8F5E9.toInt())
            btn.setTextColor(if (sel) 0xFFFFFFFF.toInt() else 0xFF43A047.toInt())
        }
        actualizarUI()
    }

    private fun timestampDesde(): Long {
        val cal = Calendar.getInstance()
        return when (filtroActual) {
            Filtro.SIETE_DIAS -> { cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis }
            Filtro.MES        -> { cal.add(Calendar.MONTH, -1);        cal.timeInMillis }
            Filtro.ANIO       -> { cal.add(Calendar.YEAR, -1);         cal.timeInMillis }
            Filtro.TODO       -> 0L
        }
    }

    private fun actualizarUI() {
        val desde = timestampDesde()
        val dao = db.ventaDao()

        val ventas   = dao.obtenerVentas(desde)
        val ingresos = dao.totalIngresos(desde)
        val ganancia = dao.totalGanancia(desde)
        val stats    = dao.estadisticasProductos(desde)

        tvConteoVentas.text  = ventas.size.toString()
        tvTotalIngresos.text = "$${"%.2f".format(ingresos)}"
        tvTotalGanancia.text = "$${"%.2f".format(ganancia)}"

        ventaAdapter.actualizarLista(ventas)
        tvSinVentas.visibility = if (ventas.isEmpty()) View.VISIBLE else View.GONE
        rvVentas.visibility    = if (ventas.isEmpty()) View.GONE    else View.VISIBLE

        actualizarEstadisticasProductos(stats)
    }

    private fun actualizarEstadisticasProductos(stats: List<EstadisticaProducto>) {
        layoutMasVendidos.removeAllViews()
        layoutMenosVendidos.removeAllViews()

        if (stats.isEmpty()) {
            agregarFilaVacia(layoutMasVendidos)
            agregarFilaVacia(layoutMenosVendidos)
            return
        }

        val masVendidos   = stats.take(5)
        val menosVendidos = if (stats.size > 5) stats.takeLast(minOf(5, stats.size - 5)).reversed()
                            else stats.reversed()

        masVendidos.forEachIndexed { i, stat ->
            layoutMasVendidos.addView(inflateStatRow(stat, i + 1))
        }
        menosVendidos.forEach { stat ->
            layoutMenosVendidos.addView(inflateStatRow(stat, null))
        }
    }

    private fun agregarFilaVacia(parent: LinearLayout) {
        val tv = TextView(this).apply {
            text = "Sin datos en este período"
            setTextColor(0xFF9E9E9E.toInt())
            textSize = 13f
            setPadding(8, 8, 8, 8)
        }
        parent.addView(tv)
    }

    private fun inflateStatRow(stat: EstadisticaProducto, posicion: Int?): View {
        val row = LayoutInflater.from(this).inflate(R.layout.item_producto_stats, layoutMasVendidos, false)
        val nombre = if (posicion != null) "$posicion. ${stat.nombreProducto}" else stat.nombreProducto
        row.findViewById<TextView>(R.id.tvProductoNombre).text = nombre
        row.findViewById<TextView>(R.id.tvProductoUnidades).text = "${stat.totalVendido} uds"
        val color = if (stat.totalGanancia >= 0) 0xFF2E7D32.toInt() else 0xFFE53935.toInt()
        val tvGanancia = row.findViewById<TextView>(R.id.tvProductoGanancia)
        tvGanancia.text = "$${"%.2f".format(stat.totalGanancia)}"
        tvGanancia.setTextColor(color)
        return row
    }

    private fun mostrarDetalleVenta(venta: Venta) {
        val detalles = db.ventaDao().obtenerDetalles(venta.id)
        val sb = StringBuilder()
        detalles.forEach { d ->
            sb.appendLine("• ${d.nombreProducto}")
            sb.appendLine("  ${d.cantidad} x $${"%.2f".format(d.precioUnitario)} = $${"%.2f".format(d.cantidad * d.precioUnitario)} MXN")
        }
        sb.append("\nTotal: $${"%.2f".format(venta.total)} MXN")

        AlertDialog.Builder(this)
            .setTitle("Venta #${venta.id} — ${dateFormat.format(Date(venta.fecha))}")
            .setMessage(sb.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
