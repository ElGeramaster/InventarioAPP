package com.example.inventario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PorCaducarFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutVacio: View
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_por_caducar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())

        recyclerView = view.findViewById(R.id.recyclerPorCaducar)
        layoutVacio  = view.findViewById(R.id.layoutVacioCaducidad)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        cargarPorCaducar()
    }

    private fun cargarPorCaducar() {
        val limite = NotificationHelper.inicioDeHoy() +
                NotificationHelper.DIAS_ALERTA_CADUCIDAD * 24L * 60 * 60 * 1000
        val productos = db.productoDao().obtenerPorCaducar(limite)

        if (productos.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutVacio.visibility  = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutVacio.visibility  = View.GONE

            val adapter = ProductoCaducidadAdapter(productos) { producto ->
                val intent = android.content.Intent(requireContext(), DetalleProductoActivity::class.java)
                intent.putExtra("PRODUCTO_ID", producto.id)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
        }
    }

    // Método público para recargar desde ReportesActivity
    fun recargar() {
        cargarPorCaducar()
    }
}