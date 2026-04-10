package com.example.inventario

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private lateinit var spinnerCategoria: Spinner
    private lateinit var db: AppDatabase

    private var categoriaSeleccionada: String = ""
    private var busquedaActual: String = ""

    private val permisoNotificacionesLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        NotificationHelper.verificarYNotificarStockBajo(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        NotificationHelper.crearCanal(this)
        solicitarPermisoNotificaciones()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                categoriaSeleccionada = if (position == 0) "" else parent?.getItemAtPosition(position) as? String ?: ""
                filtrarProductos()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val fab = findViewById<FloatingActionButton>(R.id.fabAgregar)
        fab.setOnClickListener {
            startActivity(Intent(this, AgregarProductoActivity::class.java))
        }

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also {
                busquedaActual = query ?: ""
                filtrarProductos()
            }
            override fun onQueryTextChange(newText: String?) = true.also {
                busquedaActual = newText ?: ""
                filtrarProductos()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        cargarProductos()
        NotificationHelper.verificarYNotificarStockBajo(this)
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permisoNotificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
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
        actualizarCategorias()
    }

    private fun actualizarCategorias() {
        val categorias = mutableListOf("Todas")
        categorias.addAll(db.productoDao().obtenerCategorias())

        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val seleccionActual = categoriaSeleccionada
        spinnerCategoria.adapter = adapterSpinner

        val index = categorias.indexOf(seleccionActual)
        if (index >= 0) {
            spinnerCategoria.setSelection(index)
        }
    }

    private fun filtrarProductos() {
        if (!::adapter.isInitialized) return

        val resultados = if (categoriaSeleccionada.isEmpty()) {
            if (busquedaActual.isEmpty()) {
                db.productoDao().obtenerTodos()
            } else {
                db.productoDao().buscar(busquedaActual)
            }
        } else {
            db.productoDao().buscarPorCategoria(categoriaSeleccionada, busquedaActual)
        }
        adapter.actualizarLista(resultados)
    }
}