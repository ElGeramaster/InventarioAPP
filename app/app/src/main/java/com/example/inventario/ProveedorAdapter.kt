package com.example.inventario

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ProveedorAdapter(
    private var proveedores: List<Proveedor>,
    private val onClick: (Proveedor) -> Unit
) : RecyclerView.Adapter<ProveedorAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProveedor: ImageView = itemView.findViewById(R.id.ivProveedor)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProveedor)
        val tvProducto: TextView = itemView.findViewById(R.id.tvProductoProveedor)
        val tvTelefono: TextView = itemView.findViewById(R.id.tvTelefonoProveedor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = proveedores[position]
        holder.tvNombre.text = p.nombre

        if (!p.producto.isNullOrBlank()) {
            holder.tvProducto.text = p.producto
            holder.tvProducto.visibility = View.VISIBLE
        } else {
            holder.tvProducto.visibility = View.GONE
        }

        if (!p.telefono.isNullOrBlank()) {
            holder.tvTelefono.text = "📞 ${p.telefono}"
            holder.tvTelefono.visibility = View.VISIBLE
        } else {
            holder.tvTelefono.visibility = View.GONE
        }

        val bitmap = p.imagenUri?.let { ruta ->
            if (File(ruta).exists()) BitmapFactory.decodeFile(ruta) else null
        }
        if (bitmap != null) {
            holder.ivProveedor.setImageBitmap(bitmap)
        } else {
            holder.ivProveedor.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = proveedores.size

    fun actualizar(nuevos: List<Proveedor>) {
        proveedores = nuevos
        notifyDataSetChanged()
    }
}
