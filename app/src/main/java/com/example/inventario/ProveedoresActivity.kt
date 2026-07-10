package com.example.inventario

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProveedoresActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rvProveedores: RecyclerView
    private lateinit var adapter: ProveedorAdapter
    private lateinit var tvSinProveedores: TextView
    private lateinit var etBuscar: EditText

    private var busquedaActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proveedores)

        db = AppDatabase.getInstance(this)

        rvProveedores    = findViewById(R.id.rvProveedores)
        tvSinProveedores = findViewById(R.id.tvSinProveedores)
        etBuscar         = findViewById(R.id.etBuscarProveedor)

        adapter = ProveedorAdapter(emptyList()) { proveedor -> mostrarOpciones(proveedor) }
        rvProveedores.layoutManager = LinearLayoutManager(this)
        rvProveedores.adapter = adapter

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fabAgregarProveedor).setOnClickListener {
            startActivity(Intent(this, AgregarProveedorActivity::class.java))
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                busquedaActual = s?.toString()?.trim() ?: ""
                cargarProveedores()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        cargarProveedores()
    }

    private fun cargarProveedores() {
        val lista = if (busquedaActual.isEmpty()) {
            db.proveedorDao().obtenerTodos()
        } else {
            db.proveedorDao().buscar(busquedaActual)
        }
        adapter.actualizar(lista)
        if (lista.isEmpty()) {
            tvSinProveedores.visibility = View.VISIBLE
            rvProveedores.visibility = View.GONE
            tvSinProveedores.text = if (busquedaActual.isEmpty()) {
                "Aún no hay proveedores.\nToca + para agregar uno."
            } else {
                "Sin resultados para \"$busquedaActual\"."
            }
        } else {
            tvSinProveedores.visibility = View.GONE
            rvProveedores.visibility = View.VISIBLE
        }
    }

    private fun mostrarOpciones(proveedor: Proveedor) {
        val detalles = buildString {
            append(proveedor.nombre)
            if (!proveedor.producto.isNullOrBlank()) append("\n\nSurte: ${proveedor.producto}")
            if (!proveedor.telefono.isNullOrBlank()) append("\nTel: ${proveedor.telefono}")
            if (!proveedor.direccion.isNullOrBlank()) append("\nDirección: ${proveedor.direccion}")
            if (!proveedor.notas.isNullOrBlank()) append("\n\nNotas: ${proveedor.notas}")
        }

        AlertDialog.Builder(this)
            .setTitle("Proveedor")
            .setMessage(detalles)
            .setPositiveButton("Editar") { _, _ ->
                val intent = Intent(this, AgregarProveedorActivity::class.java)
                intent.putExtra("PROVEEDOR_ID", proveedor.id)
                startActivity(intent)
            }
            .setNegativeButton("Eliminar") { _, _ -> confirmarEliminar(proveedor) }
            .setNeutralButton("Cerrar", null)
            .show()
    }

    private fun confirmarEliminar(proveedor: Proveedor) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar proveedor")
            .setMessage("¿Eliminar a ${proveedor.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.proveedorDao().eliminar(proveedor)
                cargarProveedores()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
