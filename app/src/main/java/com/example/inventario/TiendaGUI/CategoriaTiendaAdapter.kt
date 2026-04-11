package com.example.inventario.TiendaGUI

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R

class CategoriaTiendaAdapter(
    private var categorias: List<String>,
    private var seleccionada: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriaTiendaAdapter.VH>() {

    // Paleta rotativa para las tarjetas de categoría (como en el mockup)
    private val coloresFondo = listOf(
        "#F3E5F5", // morado claro (Todos)
        "#BBDEFB", // azul claro
        "#FFCCBC", // naranja claro
        "#FFF59D", // amarillo claro
        "#C8E6C9", // verde claro
        "#FFCDD2"  // rojo claro
    )

    private val coloresTexto = listOf(
        "#6A1B9A",
        "#0D47A1",
        "#BF360C",
        "#F57F17",
        "#1B5E20",
        "#B71C1C"
    )

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardCategoria)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria_tienda, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val categoria = categorias[position]
        holder.tvNombre.text = categoria

        val colorFondo = coloresFondo[position % coloresFondo.size]
        val colorTexto = coloresTexto[position % coloresTexto.size]

        holder.card.setCardBackgroundColor(Color.parseColor(colorFondo))
        holder.tvNombre.setTextColor(Color.parseColor(colorTexto))

        // Resaltar la seleccionada con borde/elevación
        if (categoria == seleccionada) {
            holder.card.cardElevation = 10f
            holder.tvNombre.paint.isUnderlineText = true
        } else {
            holder.card.cardElevation = 2f
            holder.tvNombre.paint.isUnderlineText = false
        }

        holder.itemView.setOnClickListener {
            onClick(categoria)
        }
    }

    override fun getItemCount() = categorias.size

    fun actualizar(nuevas: List<String>, nuevaSeleccion: String) {
        categorias = nuevas
        seleccionada = nuevaSeleccion
        notifyDataSetChanged()
    }

    fun cambiarSeleccion(nuevaSeleccion: String) {
        seleccionada = nuevaSeleccion
        notifyDataSetChanged()
    }
}
