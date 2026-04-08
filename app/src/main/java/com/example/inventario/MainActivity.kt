package com.example.inventario

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.SearchView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductoAdapter
    private lateinit var db: AppDatabase

    private val permisoNotificacionesLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Verificar stock bajo independientemente del resultado
        NotificationHelper.verificarYNotificarStockBajo(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        // Crear canal de notificaciones
        NotificationHelper.crearCanal(this)

        // Solicitar permiso de notificaciones en Android 13+
        solicitarPermisoNotificaciones()

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
    }

    private fun buscar(query: String) {
        val resultados = db.productoDao().buscar(query)
        adapter.actualizarLista(resultados)
    }
}