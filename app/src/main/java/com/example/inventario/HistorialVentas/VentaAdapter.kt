package com.example.inventario.HistorialVentas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VentaAdapter(
    private var ventas: List<Venta>,
    private val onVentaClick: (Venta) -> Unit
) : RecyclerView.Adapter<VentaAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale("es", "MX"))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvVentaId: TextView = itemView.findViewById(R.id.tvVentaId)
        val tvFecha: TextView = itemView.findViewById(R.id.tvVentaFecha)
        val tvTotal: TextView = itemView.findViewById(R.id.tvVentaTotal)
        val tvItems: TextView = itemView.findViewById(R.id.tvVentaItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val venta = ventas[position]
        holder.tvVentaId.text = "Venta #${venta.id}"
        holder.tvFecha.text = dateFormat.format(Date(venta.fecha))
        holder.tvTotal.text = "$${"%.2f".format(venta.total)} MXN"
        val plural = if (venta.totalItems == 1) "artículo" else "artículos"
        holder.tvItems.text = "${venta.totalItems} $plural"
        holder.itemView.setOnClickListener { onVentaClick(venta) }
    }

    override fun getItemCount() = ventas.size

    fun actualizarLista(nuevaLista: List<Venta>) {
        ventas = nuevaLista
        notifyDataSetChanged()
    }
}
