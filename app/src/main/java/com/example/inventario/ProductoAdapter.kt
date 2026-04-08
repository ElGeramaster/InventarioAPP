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
    private val listener: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        val viewIndicador: View = itemView.findViewById(R.id.viewIndicador)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val cardThumbnail: CardView = itemView.findViewById(R.id.cardThumbnail)
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
        holder.tvPrecio.text = "$${"%.2f".format(producto.precio)}"
        holder.tvCantidad.text = producto.cantidad.toString()

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

        if (producto.cantidad <= producto.stockMinimo) {
            holder.viewIndicador.setBackgroundColor(Color.parseColor("#E53935"))
            holder.tvCantidad.setTextColor(Color.parseColor("#C62828"))
        } else {
            holder.viewIndicador.setBackgroundColor(Color.parseColor("#43A047"))
            holder.tvCantidad.setTextColor(Color.parseColor("#212121"))
        }

        holder.itemView.setOnClickListener { listener(producto) }
    }

    override fun getItemCount() = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}