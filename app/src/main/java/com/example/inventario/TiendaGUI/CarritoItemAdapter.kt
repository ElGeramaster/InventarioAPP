package com.example.inventario.TiendaGUI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R

class CarritoItemAdapter(
    private var items: List<CarritoItem>,
    private val onClick: (CarritoItem) -> Unit
) : RecyclerView.Adapter<CarritoItemAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvCarritoNombre)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCarritoCantidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvNombre.text = item.producto.nombre
        holder.tvCantidad.text = "Cant: ${item.cantidad}"
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun actualizarLista(nuevaLista: List<CarritoItem>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}
