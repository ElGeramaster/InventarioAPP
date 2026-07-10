package com.example.inventario

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Pequeña "libreta" de fiados: registra cuánto te debe cada cliente y permite
 * abonar, aumentar la deuda o liquidarla. Muestra el total por cobrar arriba.
 */
class FiadosActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rvFiados: RecyclerView
    private lateinit var adapter: FiadoAdapter
    private lateinit var tvSinFiados: TextView
    private lateinit var tvTotalDeuda: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fiados)

        db = AppDatabase.getInstance(this)

        rvFiados     = findViewById(R.id.rvFiados)
        tvSinFiados  = findViewById(R.id.tvSinFiados)
        tvTotalDeuda = findViewById(R.id.tvTotalDeuda)

        adapter = FiadoAdapter(emptyList()) { fiado -> mostrarOpciones(fiado) }
        rvFiados.layoutManager = LinearLayoutManager(this)
        rvFiados.adapter = adapter

        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.fabAgregarFiado).setOnClickListener {
            mostrarDialogoFiado(null)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarFiados()
    }

    private fun cargarFiados() {
        val lista = db.fiadoDao().obtenerTodos()
        adapter.actualizar(lista)

        val total = db.fiadoDao().obtenerTotalDeuda()
        tvTotalDeuda.text = "$${"%.2f".format(total)}"

        if (lista.isEmpty()) {
            tvSinFiados.visibility = View.VISIBLE
            rvFiados.visibility = View.GONE
        } else {
            tvSinFiados.visibility = View.GONE
            rvFiados.visibility = View.VISIBLE
        }
    }

    /**
     * Formulario de alta/edición. Si [fiado] es null se crea uno nuevo; si no,
     * se editan sus datos.
     */
    private fun mostrarDialogoFiado(fiado: Fiado?) {
        val vista = layoutInflater.inflate(R.layout.dialog_fiado, null)
        val etNombre = vista.findViewById<TextInputEditText>(R.id.etNombreFiado)
        val etMonto = vista.findViewById<TextInputEditText>(R.id.etMontoFiado)
        val etTelefono = vista.findViewById<TextInputEditText>(R.id.etTelefonoFiado)
        val etDescripcion = vista.findViewById<TextInputEditText>(R.id.etDescripcionFiado)

        if (fiado != null) {
            etNombre.setText(fiado.nombre)
            etMonto.setText("%.2f".format(fiado.monto))
            etTelefono.setText(fiado.telefono ?: "")
            etDescripcion.setText(fiado.descripcion ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle(if (fiado == null) "Nuevo fiado" else "Editar fiado")
            .setView(vista)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val monto = etMonto.text.toString().trim().toDoubleOrNull()
                if (nombre.isEmpty()) {
                    Toast.makeText(this, "Escribe el nombre del cliente", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (monto == null || monto <= 0) {
                    Toast.makeText(this, "Escribe cuánto debe", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val telefono = etTelefono.text.toString().trim().ifEmpty { null }
                val descripcion = etDescripcion.text.toString().trim().ifEmpty { null }

                if (fiado == null) {
                    db.fiadoDao().insertar(
                        Fiado(nombre = nombre, telefono = telefono, monto = monto, descripcion = descripcion)
                    )
                    Toast.makeText(this, "Fiado registrado", Toast.LENGTH_SHORT).show()
                } else {
                    db.fiadoDao().actualizar(
                        fiado.copy(nombre = nombre, telefono = telefono, monto = monto, descripcion = descripcion)
                    )
                    Toast.makeText(this, "Fiado actualizado", Toast.LENGTH_SHORT).show()
                }
                cargarFiados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpciones(fiado: Fiado) {
        val opciones = arrayOf(
            "Abonar (pagó una parte)",
            "Aumentar deuda",
            "Liquidar (pagó todo)",
            "Editar datos",
            "Eliminar"
        )
        AlertDialog.Builder(this)
            .setTitle("${fiado.nombre} · debe $${"%.2f".format(fiado.monto)}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> dialogoMonto("Abonar", "¿Cuánto abonó?") { abono -> abonar(fiado, abono) }
                    1 -> dialogoMonto("Aumentar deuda", "¿Cuánto más se llevó?") { extra ->
                        db.fiadoDao().actualizar(fiado.copy(monto = fiado.monto + extra))
                        cargarFiados()
                    }
                    2 -> confirmarLiquidar(fiado)
                    3 -> mostrarDialogoFiado(fiado)
                    4 -> confirmarEliminar(fiado)
                }
            }
            .show()
    }

    private fun abonar(fiado: Fiado, abono: Double) {
        val restante = fiado.monto - abono
        if (restante <= 0.009) {
            db.fiadoDao().eliminar(fiado)
            Toast.makeText(this, "Deuda saldada 🎉", Toast.LENGTH_SHORT).show()
        } else {
            db.fiadoDao().actualizar(fiado.copy(monto = restante))
            Toast.makeText(this, "Abono registrado. Queda $${"%.2f".format(restante)}", Toast.LENGTH_SHORT).show()
        }
        cargarFiados()
    }

    /** Diálogo con un solo campo numérico para abonos/aumentos. */
    private fun dialogoMonto(titulo: String, hint: String, onValor: (Double) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            this.hint = hint
        }
        val margen = (20 * resources.displayMetrics.density).toInt()
        val contenedor = FrameLayout(this).apply {
            setPadding(margen, margen / 2, margen, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(contenedor)
            .setPositiveButton("Aceptar") { _, _ ->
                val valor = input.text.toString().trim().toDoubleOrNull()
                if (valor == null || valor <= 0) {
                    Toast.makeText(this, "Escribe una cantidad válida", Toast.LENGTH_SHORT).show()
                } else {
                    onValor(valor)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarLiquidar(fiado: Fiado) {
        AlertDialog.Builder(this)
            .setTitle("Liquidar deuda")
            .setMessage("¿${fiado.nombre} pagó todo lo que debía ($${"%.2f".format(fiado.monto)})?")
            .setPositiveButton("Sí, pagó todo") { _, _ ->
                db.fiadoDao().eliminar(fiado)
                Toast.makeText(this, "Deuda saldada 🎉", Toast.LENGTH_SHORT).show()
                cargarFiados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(fiado: Fiado) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar fiado")
            .setMessage("¿Eliminar el registro de ${fiado.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                db.fiadoDao().eliminar(fiado)
                cargarFiados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
