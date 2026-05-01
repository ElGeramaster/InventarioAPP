package com.example.inventario.HistorialVentas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R
import com.example.inventario.Venta
import java.text.SimpleDateFormat
import java.util.*

class VentaAdapter(
    private var ventas: List<Venta>
) : RecyclerView.Adapter<VentaAdapter.VH>() {

    private val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFecha: TextView = itemView.findViewById(R.id.tvFechaVenta)
        val tvArticulos: TextView = itemView.findViewById(R.id.tvArticulosVenta)
        val tvTotal: TextView = itemView.findViewById(R.id.tvTotalVenta)
        val tvGanancia: TextView = itemView.findViewById(R.id.tvGananciaVenta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val venta = ventas[position]
        holder.tvFecha.text = fmt.format(Date(venta.timestamp))
        holder.tvArticulos.text = "${venta.totalArticulos} artículo(s)"
        holder.tvTotal.text = "$${"%.2f".format(venta.total)}"
        holder.tvGanancia.text = "Gan: $${"%.2f".format(venta.ganancia)}"
    }

    override fun getItemCount() = ventas.size

    fun actualizar(nuevas: List<Venta>) {
        ventas = nuevas
        notifyDataSetChanged()
    }
}
