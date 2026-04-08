package com.example.inventario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReportesActivity : AppCompatActivity() {

    private lateinit var tvTotalProductos: TextView
    private lateinit var tvValorTotal: TextView
    private lateinit var tvStockBajoCount: TextView
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
        val valorTotal     = productos.sumOf { it.precio * it.cantidad }
        val countStockBajo = stockBajo.size

        tvTotalProductos.text = totalProductos.toString()
        tvValorTotal.text     = "$${"%.0f".format(valorTotal)}"
        tvStockBajoCount.text = countStockBajo.toString()
    }
}