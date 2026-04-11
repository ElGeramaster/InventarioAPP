package com.example.inventario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReportesActivity : AppCompatActivity() {

    private lateinit var tvTotalProductos: TextView
    private lateinit var tvValorTotal: TextView
    private lateinit var tvStockBajoCount: TextView
    private lateinit var tvValorCompra: TextView
    private lateinit var tvGanancia: TextView
    private lateinit var db: AppDatabase
    private lateinit var stockBajoFragment: StockBajoFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        title = "Reportes"

        db = AppDatabase.getInstance(this)

        tvTotalProductos = findViewById(R.id.tvTotalProductos)
        tvValorTotal     = findViewById(R.id.tvValorTotal)
        tvStockBajoCount = findViewById(R.id.tvStockBajoCount)
        tvValorCompra    = findViewById(R.id.tvValorCompra)
        tvGanancia       = findViewById(R.id.tvGanancia)

        cargarFragment()
        cargarResumen()
    }

    override fun onResume() {
        super.onResume()
        cargarResumen()
        stockBajoFragment.recargar()
    }

    private fun cargarFragment() {
        stockBajoFragment = StockBajoFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, stockBajoFragment)
            .commit()
    }

    private fun cargarResumen() {
        val productos    = db.productoDao().obtenerTodos()
        val stockBajo    = db.productoDao().obtenerStockBajo()

        val totalProductos = productos.size
        val valorVenta     = productos.sumOf { it.precio * it.cantidad }
        val valorCompra    = productos.sumOf { it.precioCompra * it.cantidad }
        val ganancia       = valorVenta - valorCompra
        val countStockBajo = stockBajo.size

        tvTotalProductos.text = totalProductos.toString()
        tvValorTotal.text     = "$${"%.0f".format(valorVenta)}"
        tvStockBajoCount.text = countStockBajo.toString()
        tvValorCompra.text    = "$${"%.0f".format(valorCompra)}"
        tvGanancia.text       = "$${"%.0f".format(ganancia)}"
    }
}