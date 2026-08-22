package com.example.inventario

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductoCaducidadAdapter(
    private var listaProductos: List<Producto>,
    private val listener: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoCaducidadAdapter.ProductoCaducidadViewHolder>() {

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class ProductoCaducidadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCaducidad)
        val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoriaCaducidad)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFechaCaducidadItem)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoCaducidad)
        val viewIndicador: View = itemView.findViewById(R.id.viewIndicadorCaducidad)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnailCaducidad)
        val cardThumbnail: CardView = itemView.findViewById(R.id.cardThumbnailCaducidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoCaducidadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_caducidad, parent, false)
        return ProductoCaducidadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoCaducidadViewHolder, position: Int) {
        val producto = listaProductos[position]
        val fechaCaducidad = producto.fechaCaducidad ?: return

        holder.tvNombre.text = producto.nombre
        holder.tvCategoria.text = producto.categoria
        holder.tvFecha.text = formatoFecha.format(Date(fechaCaducidad))

        if (!producto.imagenUri.isNullOrEmpty()) {
            val bitmap = ImagenUtils.miniatura(producto.imagenUri)
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap)
                holder.cardThumbnail.visibility = View.VISIBLE
            } else {
                holder.cardThumbnail.visibility = View.GONE
            }
        } else {
            holder.cardThumbnail.visibility = View.GONE
        }

        val dias = (fechaCaducidad - NotificationHelper.inicioDeHoy()) / (24L * 60 * 60 * 1000)
        val (texto, color) = when {
            dias < 0 -> "Vencido hace ${-dias}d" to "#C62828"
            dias == 0L -> "Caduca hoy" to "#C62828"
            dias <= 3 -> "Caduca en ${dias}d" to "#E64A19"
            else -> "Caduca en ${dias}d" to "#F57C00"
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(Color.parseColor(color))
        holder.viewIndicador.setBackgroundColor(Color.parseColor(color))

        holder.itemView.setOnClickListener { listener(producto) }
    }

    override fun getItemCount() = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}