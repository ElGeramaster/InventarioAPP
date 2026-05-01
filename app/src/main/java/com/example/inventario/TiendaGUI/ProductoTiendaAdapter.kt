package com.example.inventario.TiendaGUI

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.Producto
import com.example.inventario.R

class ProductoTiendaAdapter(
    private var productos: List<Producto>,
    private val onAgregar: (Producto) -> Unit,
    private val onVerDetalle: (Producto) -> Unit = {}
) : RecyclerView.Adapter<ProductoTiendaAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioProducto)
        val tvStock: TextView = itemView.findViewById(R.id.tvStockProducto)
        val ivFoto: ImageView = itemView.findViewById(R.id.ivFotoProducto)
        val tvSinFoto: TextView = itemView.findViewById(R.id.tvSinFoto)
        val btnAgregar: Button = itemView.findViewById(R.id.btnAgregarTienda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_tienda, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val producto = productos[position]

        holder.tvNombre.text = producto.nombre
        holder.tvPrecio.text = "$${"%.2f".format(producto.precio)} MXN"
        holder.tvStock.text = "Stock: ${producto.cantidad}"

        // Mostrar imagen si existe
        if (!producto.imagenUri.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(producto.imagenUri)
            if (bitmap != null) {
                holder.ivFoto.setImageBitmap(bitmap)
                holder.ivFoto.visibility = View.VISIBLE
                holder.tvSinFoto.visibility = View.GONE
            } else {
                holder.ivFoto.visibility = View.GONE
                holder.tvSinFoto.visibility = View.VISIBLE
            }
        } else {
            holder.ivFoto.visibility = View.GONE
            holder.tvSinFoto.visibility = View.VISIBLE
        }

        if (producto.cantidad <= 0) {
            holder.btnAgregar.isEnabled = false
            holder.btnAgregar.text = "Sin stock"
            holder.btnAgregar.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            )
            holder.tvStock.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_light)
            )
        } else {
            holder.btnAgregar.isEnabled = true
            holder.btnAgregar.text = "Agregar"
            holder.btnAgregar.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.verde_primario)
            )
            holder.tvStock.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gris_texto)
            )
        }

        holder.btnAgregar.setOnClickListener {
            onAgregar(producto)
        }

        holder.itemView.setOnClickListener {
            onVerDetalle(producto)
        }
    }

    override fun getItemCount() = productos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        productos = nuevaLista
        notifyDataSetChanged()
    }
}