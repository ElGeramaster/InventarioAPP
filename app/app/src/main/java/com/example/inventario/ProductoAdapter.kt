package com.example.inventario

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private var listaProductos: List<Producto>,
    private val onToggleFavorito: (Producto) -> Unit = {},
    private val listener: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val tvUnidades: TextView = itemView.findViewById(R.id.tvUnidades)
        val viewIndicador: View = itemView.findViewById(R.id.viewIndicador)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val cardThumbnail: CardView = itemView.findViewById(R.id.cardThumbnail)
        val ivFavorito: ImageView = itemView.findViewById(R.id.ivFavorito)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        holder.tvNombre.text = producto.nombre
        holder.tvCategoria.text = producto.categoria
        if (producto.vendePorPeso) {
            holder.tvPrecio.text = "$${"%.2f".format(producto.precioKilo)} /kg"
        } else {
            holder.tvPrecio.text = "$${"%.2f".format(producto.precio)}"
        }
        if (producto.vendePorPeso && producto.precio <= 0) {
            holder.tvCantidad.text = "—"
            holder.tvUnidades.text = "por kg"
        } else {
            holder.tvCantidad.text = producto.cantidad.toString()
            holder.tvUnidades.text = "unidades"
        }

        // Mostrar thumbnail si hay imagen
        if (!producto.imagenUri.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(producto.imagenUri)
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap)
                holder.cardThumbnail.visibility = View.VISIBLE
            } else {
                holder.cardThumbnail.visibility = View.GONE
            }
        } else {
            holder.cardThumbnail.visibility = View.GONE
        }

        val esStockBajo = producto.seVendePorPieza && producto.cantidad <= producto.stockMinimo
        if (esStockBajo) {
            holder.viewIndicador.setBackgroundColor(Color.parseColor("#E53935"))
            holder.tvCantidad.setTextColor(Color.parseColor("#C62828"))
        } else {
            holder.viewIndicador.setBackgroundColor(Color.parseColor("#43A047"))
            holder.tvCantidad.setTextColor(Color.parseColor("#212121"))
        }

        // Corazón de favorito
        holder.ivFavorito.setImageResource(
            if (producto.favorito) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.ivFavorito.setOnClickListener { onToggleFavorito(producto) }

        holder.itemView.setOnClickListener { listener(producto) }
    }

    override fun getItemCount() = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}