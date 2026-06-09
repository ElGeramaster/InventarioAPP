package com.example.inventario

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventario.TiendaGUI.TiendaActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Pantalla de activación. Es la pantalla de inicio de la app.
 * Si la app ya está activada (y no ha vencido), pasa directo a la tienda.
 */
class LicenseActivity : AppCompatActivity() {

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

        tilCodigo = findViewById(R.id.tilCodigoActivacion)
        etCodigo = findViewById(R.id.etCodigoActivacion)

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
        if (codigo.isEmpty()) {
            tilCodigo.error = "Ingresa el código de activación"
            return
        }

        if (LicenseManager.activar(this, codigo)) {
            tilCodigo.error = null
            Toast.makeText(this, "Activación correcta. ¡Bienvenido!", Toast.LENGTH_SHORT).show()
            irATienda()
        } else {
            tilCodigo.error = "Código incorrecto"
            Toast.makeText(this, "El código no es válido", Toast.LENGTH_SHORT).show()
        }
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
