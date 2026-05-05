package com.example.inventario

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgregarProductoActivity : AppCompatActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etCategoria: TextInputEditText
    private lateinit var etPrecioCompra: TextInputEditText
    private lateinit var etPrecio: TextInputEditText
    private lateinit var etCantidad: TextInputEditText
    private lateinit var etStockMinimo: TextInputEditText
    private lateinit var ivProducto: ImageView
    private lateinit var layoutPlaceholder: View
    private lateinit var btnQuitarImagen: Button
    private lateinit var ivQrCode: ImageView
    private lateinit var layoutQrPlaceholder: LinearLayout
    private lateinit var db: AppDatabase

    private var productoExistente: Producto? = null
    private var imagenUri: String? = null
    private var tempCameraUri: Uri? = null

    private val tomarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) {
            tempCameraUri?.let { uri ->
                val archivoGuardado = copiarImagenAInterno(uri)
                if (archivoGuardado != null) {
                    imagenUri = archivoGuardado
                    mostrarImagen(archivoGuardado)
                }
            }
        }
    }

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val archivoGuardado = copiarImagenAInterno(it)
            if (archivoGuardado != null) {
                imagenUri = archivoGuardado
                mostrarImagen(archivoGuardado)
            }
        }
    }

    private val permisoCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        db = AppDatabase.getInstance(this)

        etNombre       = findViewById(R.id.etNombre)
        etCategoria    = findViewById(R.id.etCategoria)
        etPrecioCompra = findViewById(R.id.etPrecioCompra)
        etPrecio       = findViewById(R.id.etPrecio)
        etCantidad     = findViewById(R.id.etCantidad)
        etStockMinimo  = findViewById(R.id.etStockMinimo)
        ivProducto     = findViewById(R.id.ivProducto)
        layoutPlaceholder   = findViewById(R.id.layoutPlaceholder)
        btnQuitarImagen     = findViewById(R.id.btnQuitarImagen)
        ivQrCode            = findViewById(R.id.ivQrCode)
        layoutQrPlaceholder = findViewById(R.id.layoutQrPlaceholder)

        val productoId = intent.getIntExtra("PRODUCTO_ID", -1)
        if (productoId != -1) {
            productoExistente = db.productoDao().obtenerPorId(productoId)
            productoExistente?.let {
                llenarCampos(it)
                imagenUri = it.imagenUri
                it.imagenUri?.let { uri -> mostrarImagen(uri) }
                mostrarQr(it)
            }
            title = "Editar producto"
        } else {
            title = "Agregar producto"
        }

        findViewById<Button>(R.id.btnCamara).setOnClickListener {
            verificarPermisoYAbrirCamara()
        }

        findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnQuitarImagen.setOnClickListener {
            imagenUri = null
            ivProducto.visibility = View.GONE
            layoutPlaceholder.visibility = View.VISIBLE
            btnQuitarImagen.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnGenerarQr).setOnClickListener {
            generarQrDesdeFormulario()
        }

        findViewById<Button>(R.id.btnGuardar).setOnClickListener {
            guardarProducto()
        }

        findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            finish()
        }
    }

    private fun llenarCampos(producto: Producto) {
        etNombre.setText(producto.nombre)
        etCategoria.setText(producto.categoria)
        etPrecioCompra.setText(producto.precioCompra.toString())
        etPrecio.setText(producto.precio.toString())
        etCantidad.setText(producto.cantidad.toString())
        etStockMinimo.setText(producto.stockMinimo.toString())
    }

    private fun mostrarQr(producto: Producto) {
        try {
            val bitmap = QrCodeHelper.generarQr(producto)
            ivQrCode.setImageBitmap(bitmap)
            ivQrCode.visibility = View.VISIBLE
            layoutQrPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generarQrDesdeFormulario() {
        val nombre    = etNombre.text.toString().trim()
        val categoria = etCategoria.text.toString().trim()
        val precioStr = etPrecio.text.toString().trim()

        if (nombre.isEmpty() || categoria.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Completa nombre, categoría y precio para generar el QR", Toast.LENGTH_SHORT).show()
            return
        }

        val precio = precioStr.toDoubleOrNull()
        if (precio == null) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val bitmap = if (productoExistente != null) {
                val actualizado = productoExistente!!.copy(
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio
                )
                QrCodeHelper.generarQr(actualizado)
            } else {
                QrCodeHelper.generarQrPreview(nombre, categoria, precio)
            }
            ivQrCode.setImageBitmap(bitmap)
            ivQrCode.visibility = View.VISIBLE
            layoutQrPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar código QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamara()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val archivoFoto = crearArchivoImagen() ?: return
        tempCameraUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            archivoFoto
        )
        tomarFotoLauncher.launch(tempCameraUri!!)
    }

    private fun crearArchivoImagen(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val directorio = File(filesDir, "images")
            if (!directorio.exists()) directorio.mkdirs()
            File.createTempFile("IMG_${timeStamp}_", ".jpg", directorio)
        } catch (e: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun copiarImagenAInterno(uri: Uri): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val directorio = File(filesDir, "images")
            if (!directorio.exists()) directorio.mkdirs()
            val archivo = File(directorio, "IMG_${timeStamp}.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                archivo.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            archivo.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun mostrarImagen(path: String) {
        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap != null) {
            ivProducto.setImageBitmap(bitmap)
            ivProducto.visibility = View.VISIBLE
            layoutPlaceholder.visibility = View.GONE
            btnQuitarImagen.visibility = View.VISIBLE
        }
    }

    private fun guardarProducto() {
        val nombre          = etNombre.text.toString().trim()
        val categoria       = etCategoria.text.toString().trim()
        val precioCompraStr = etPrecioCompra.text.toString().trim()
        val precioStr       = etPrecio.text.toString().trim()
        val cantidadStr     = etCantidad.text.toString().trim()
        val stockMinimoStr  = etStockMinimo.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            return
        }
        if (categoria.isEmpty()) {
            etCategoria.error = "La categoría es obligatoria"
            return
        }
        if (precioCompraStr.isEmpty()) {
            etPrecioCompra.error = "El precio de compra es obligatorio"
            return
        }
        if (precioStr.isEmpty()) {
            etPrecio.error = "El precio de venta es obligatorio"
            return
        }
        if (cantidadStr.isEmpty()) {
            etCantidad.error = "La cantidad es obligatoria"
            return
        }
        if (stockMinimoStr.isEmpty()) {
            etStockMinimo.error = "El stock mínimo es obligatorio"
            return
        }

        val precioCompra = precioCompraStr.toDouble()
        val precio       = precioStr.toDouble()
        val cantidad     = cantidadStr.toInt()
        val stockMinimo  = stockMinimoStr.toInt()

        if (productoExistente != null) {
            val actualizado = productoExistente!!.copy(
                nombre = nombre,
                categoria = categoria,
                precioCompra = precioCompra,
                precio = precio,
                cantidad = cantidad,
                stockMinimo = stockMinimo,
                imagenUri = imagenUri
            )
            db.productoDao().actualizar(actualizado)
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
        } else {
            val nuevo = Producto(
                nombre = nombre,
                categoria = categoria,
                precioCompra = precioCompra,
                precio = precio,
                cantidad = cantidad,
                stockMinimo = stockMinimo,
                imagenUri = imagenUri
            )
            db.productoDao().insertar(nuevo)
            Toast.makeText(this, "Producto guardado", Toast.LENGTH_SHORT).show()
        }

        NotificationHelper.verificarYNotificarStockBajo(this)

        finish()
    }
}
