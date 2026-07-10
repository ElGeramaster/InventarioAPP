package com.example.inventario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FiadoAdapter(
    private var fiados: List<Fiado>,
    private val onClick: (Fiado) -> Unit
) : RecyclerView.Adapter<FiadoAdapter.VH>() {

    private val fmt = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreFiado)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionFiado)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFechaFiado)
        val tvMonto: TextView = itemView.findViewById(R.id.tvMontoFiado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fiado, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = fiados[position]
        holder.tvNombre.text = f.nombre
        holder.tvMonto.text = "$${"%.2f".format(f.monto)}"
        holder.tvFecha.text = fmt.format(Date(f.timestamp))

        if (!f.descripcion.isNullOrBlank()) {
            holder.tvDescripcion.text = f.descripcion
            holder.tvDescripcion.visibility = View.VISIBLE
        } else {
            holder.tvDescripcion.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(f) }
    }

    override fun getItemCount() = fiados.size

    fun actualizar(nuevos: List<Fiado>) {
        fiados = nuevos
        notifyDataSetChanged()
    }
}
