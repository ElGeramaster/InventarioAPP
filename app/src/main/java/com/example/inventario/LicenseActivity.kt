package com.example.inventario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventario.TiendaGUI.TiendaActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Pantalla de activación. Es la pantalla de inicio de la app.
 * Si la app ya está activada (y no ha vencido), pasa directo a la tienda.
 */
class LicenseActivity : BaseActivity() {

    private lateinit var tilCodigo: TextInputLayout
    private lateinit var etCodigo: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        // Si no se requiere activación, entrar directo a la tienda.
        if (!LicenseManager.requiereActivacion(this)) {
            irATienda()
            return
        }

        setContentView(R.layout.activity_license)

        mostrarLogo()

        tilCodigo = findViewById(R.id.tilCodigoActivacion)
        etCodigo = findViewById(R.id.etCodigoActivacion)

        // El usuario ya no se pide, se oculta en el layout.
        findViewById<View>(R.id.tilUsuarioActivacion).visibility = View.GONE

        etCodigo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validar()
                true
            } else {
                false
            }
        }

        findViewById<Button>(R.id.btnActivar).setOnClickListener { validar() }
    }

    private fun validar() {
        val codigo = etCodigo.text?.toString()?.trim().orEmpty()

        tilCodigo.error = null

        if (codigo.isEmpty()) {
            tilCodigo.error = "Ingresa tu contraseña"
            return
        }

        when (LicenseManager.activar(this, codigo)) {
            LicenseManager.ResultadoActivacion.OK -> {
                Toast.makeText(this, "Activación correcta. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
                irATienda()
            }
            LicenseManager.ResultadoActivacion.CLAVE_INCORRECTA -> {
                tilCodigo.error = "Contraseña incorrecta"
                Toast.makeText(this, "La contraseña no es válida", Toast.LENGTH_SHORT).show()
            }
            LicenseManager.ResultadoActivacion.CLAVE_REPETIDA -> {
                tilCodigo.error = "Esa clave ya se usó. Usa una distinta a la anterior."
                Toast.makeText(
                    this,
                    "Esa clave ya se usó. Debes usar una clave distinta a la anterior.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarLogo() {
        val path = LogoManager.obtenerPath(this) ?: return
        val bitmap = ImagenUtils.miniatura(path) ?: return
        val ivLogo = findViewById<ImageView>(R.id.ivLogoActivacion)
        val tvCandado = findViewById<TextView>(R.id.tvCandado)
        ivLogo.setImageBitmap(bitmap)
        ivLogo.visibility = View.VISIBLE
        tvCandado.visibility = View.GONE
    }

    private fun irATienda() {
        startActivity(Intent(this, TiendaActivity::class.java))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No se puede saltar la activación: salir de la app.
        finishAffinity()
    }
}