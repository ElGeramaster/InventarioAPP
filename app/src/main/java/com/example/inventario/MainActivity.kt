package com.example.inventario

import android.content.Intent
import android.os.Bundle
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val fab = findViewById<FloatingActionButton>(R.id.fabAgregar)
        fab.setOnClickListener {
            startActivity(Intent(this, AgregarProductoActivity::class.java))
        }

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also { buscar(query ?: "") }
            override fun onQueryTextChange(newText: String?) = true.also { buscar(newText ?: "") }
        })
    }

    override fun onResume() {
        super.onResume()
        cargarProductos()
    }

    private fun cargarProductos() {
        val productos = db.productoDao().obtenerTodos()
        if (!::adapter.isInitialized) {
            adapter = ProductoAdapter(productos) { producto ->
                val intent = Intent(this, DetalleProductoActivity::class.java)
                intent.putExtra("PRODUCTO_ID", producto.id)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
        } else {
            adapter.actualizarLista(productos)
        }
    }

    private fun buscar(query: String) {
        val resultados = db.productoDao().buscar(query)
        adapter.actualizarLista(resultados)
    }
}