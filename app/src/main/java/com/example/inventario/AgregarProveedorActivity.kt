package com.example.inventario

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formulario para dar de alta o editar un proveedor (con foto opcional).
 * Sigue el mismo patrón de imágenes que [AgregarProductoActivity].
 */
class AgregarProveedorActivity : BaseActivity() {

    private lateinit var etNombre: TextInputEditText
    private lateinit var etProducto: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etDireccion: TextInputEditText
    private lateinit var etNotas: TextInputEditText
    private lateinit var ivProveedor: ImageView
    private lateinit var layoutPlaceholder: View
    private lateinit var btnQuitarImagen: Button
    private lateinit var db: AppDatabase

    private var proveedorExistente: Proveedor? = null
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
        setContentView(R.layout.activity_agregar_proveedor)

        db = AppDatabase.getInstance(this)

        etNombre    = findViewById(R.id.etNombre)
        etProducto  = findViewById(R.id.etProducto)
        etTelefono  = findViewById(R.id.etTelefono)
        etDireccion = findViewById(R.id.etDireccion)
        etNotas     = findViewById(R.id.etNotas)
        ivProveedor = findViewById(R.id.ivProveedor)
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder)
        btnQuitarImagen   = findViewById(R.id.btnQuitarImagen)

        val proveedorId = intent.getIntExtra("PROVEEDOR_ID", -1)
        if (proveedorId != -1) {
            proveedorExistente = db.proveedorDao().obtenerPorId(proveedorId)
            proveedorExistente?.let {
                etNombre.setText(it.nombre)
                etProducto.setText(it.producto ?: "")
                etTelefono.setText(it.telefono ?: "")
                etDireccion.setText(it.direccion ?: "")
                etNotas.setText(it.notas ?: "")
                imagenUri = it.imagenUri
                it.imagenUri?.let { ruta -> mostrarImagen(ruta) }
            }
            title = "Editar proveedor"
        } else {
            title = "Agregar proveedor"
        }

        findViewById<Button>(R.id.btnCamara).setOnClickListener {
            verificarPermisoYAbrirCamara()
        }
        findViewById<Button>(R.id.btnGaleria).setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }
        btnQuitarImagen.setOnClickListener {
            imagenUri = null
            ivProveedor.visibility = View.GONE
            layoutPlaceholder.visibility = View.VISIBLE
            btnQuitarImagen.visibility = View.GONE
        }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarProveedor() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
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
            File.createTempFile("PROV_${timeStamp}_", ".jpg", directorio)
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
            val archivo = File(directorio, "PROV_${timeStamp}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                archivo.outputStream().use { output -> input.copyTo(output) }
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
            ivProveedor.setImageBitmap(bitmap)
            ivProveedor.visibility = View.VISIBLE
            layoutPlaceholder.visibility = View.GONE
            btnQuitarImagen.visibility = View.VISIBLE
        }
    }

    private fun guardarProveedor() {
        val nombre = etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            return
        }

        val producto  = etProducto.text.toString().trim().ifEmpty { null }
        val telefono  = etTelefono.text.toString().trim().ifEmpty { null }
        val direccion = etDireccion.text.toString().trim().ifEmpty { null }
        val notas     = etNotas.text.toString().trim().ifEmpty { null }

        if (proveedorExistente != null) {
            val actualizado = proveedorExistente!!.copy(
                nombre = nombre,
                producto = producto,
                telefono = telefono,
                direccion = direccion,
                notas = notas,
                imagenUri = imagenUri
            )
            db.proveedorDao().actualizar(actualizado)
            Toast.makeText(this, "Proveedor actualizado", Toast.LENGTH_SHORT).show()
        } else {
            val nuevo = Proveedor(
                nombre = nombre,
                producto = producto,
                telefono = telefono,
                direccion = direccion,
                notas = notas,
                imagenUri = imagenUri
            )
            db.proveedorDao().insertar(nuevo)
            Toast.makeText(this, "Proveedor guardado", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
