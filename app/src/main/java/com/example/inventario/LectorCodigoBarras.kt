package com.example.inventario

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText

/**
 * Soporte para lectores de código de barras / QR que se conectan por Bluetooth
 * como teclado físico (HID), por ejemplo el BWHS25L.
 *
 * El lector "teclea" el código en el campo que tiene el foco y al terminar manda
 * un Enter. Esta clase escucha ese Enter (o la acción "Listo"/"Buscar" del
 * teclado) y entrega el código completo en [onCodigo].
 *
 * Si el lector está configurado sin sufijo Enter, el final del escaneo se
 * detecta por el silencio que sigue a una ráfaga de teclas muy rápida (algo que
 * una persona no puede reproducir escribiendo). Por eso escribir el código a
 * mano sigue funcionando igual: en ese caso el código solo se procesa cuando el
 * usuario pulsa Enter o el botón "Listo" del teclado, nunca a medias.
 *
 * No usa BluetoothAdapter ni permisos: todo es manejo de foco y teclas.
 */
class LectorCodigoBarras private constructor(
    private val campo: EditText,
    private val longitudMinima: Int,
    private val limpiarAlProcesar: Boolean,
    private val onCodigo: (String) -> Unit
) {

    companion object {
        /**
         * Máximo de milisegundos entre teclas para seguir considerando que
         * escribe un lector y no una persona (un lector manda cada carácter en
         * pocos milisegundos).
         */
        private const val MS_MAX_ENTRE_TECLAS = 60L

        /** Silencio tras el cual se da por terminado un escaneo sin Enter. */
        private const val MS_SILENCIO_FIN = 250L

        /**
         * Ventana para descartar un mismo código repetido: el Enter puede llegar
         * por dos vías (tecla y acción del teclado) y no debe contar dos veces.
         * Es muy corta, así que escanear dos veces el mismo producto sí funciona.
         */
        private const val MS_ANTIRREBOTE = 300L

        /**
         * Prepara [campo] para recibir escaneos.
         *
         * @param longitudMinima largo mínimo del código para darlo por válido.
         * @param limpiarAlProcesar `true` vacía el campo tras procesar el código
         *        (útil para escanear uno tras otro); `false` lo deja escrito
         *        (útil en formularios donde el código es un dato del producto).
         * @param onCodigo se llama con el código ya limpio de espacios y saltos.
         */
        fun configurar(
            campo: EditText,
            longitudMinima: Int = 3,
            limpiarAlProcesar: Boolean = true,
            onCodigo: (String) -> Unit
        ): LectorCodigoBarras =
            LectorCodigoBarras(campo, longitudMinima, limpiarAlProcesar, onCodigo).apply {
                iniciar()
            }
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Marca el fin del escaneo cuando el lector no manda Enter. */
    private val finDeRafaga = Runnable { procesar(forzado = false) }

    private var ultimaTeclaMs = 0L
    private var caracteresRecibidos = 0

    private var ultimoCodigo = ""
    private var ultimoCodigoMs = 0L

    /** Se vuelve false en cuanto el ritmo de escritura parece humano. */
    private var escrituraRapida = true

    /** Evita que el propio borrado del campo se interprete como escritura. */
    private var procesando = false

    private fun iniciar() {
        // Enter no debe insertar un salto de línea ni sugerencias del teclado
        // que alteren el código.
        campo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        campo.isSingleLine = true
        campo.imeOptions = EditorInfo.IME_ACTION_DONE

        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (procesando) return
                registrarEscritura(s?.toString().orEmpty(), before, count)
            }

            override fun afterTextChanged(s: Editable?) {
                if (procesando || s == null) return
                // Algunos lectores mandan el salto de línea como texto en vez de
                // como tecla; ahí el código ya está completo.
                if (s.contains('\n') || s.contains('\r')) {
                    procesar(forzado = true)
                }
            }
        })

        campo.setOnEditorActionListener { _, actionId, event ->
            val esEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            when {
                // El Enter llega dos veces (abajo/arriba): solo se procesa una.
                esEnter && event!!.action != KeyEvent.ACTION_DOWN -> true
                esEnter ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_NEXT ||
                        actionId == EditorInfo.IME_ACTION_UNSPECIFIED -> {
                    procesar(forzado = true)
                    true
                }
                else -> false
            }
        }

        // Con teclado físico el Enter llega como tecla y no siempre dispara la
        // acción del IME, así que también se escucha aquí.
        campo.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) procesar(forzado = true)
                true
            } else {
                false
            }
        }
    }

    private fun registrarEscritura(texto: String, before: Int, count: Int) {
        if (texto.isEmpty()) {
            reiniciar()
            return
        }

        val ahora = SystemClock.elapsedRealtime()
        if (before > 0) {
            // Se borró o se reemplazó texto: es edición manual.
            escrituraRapida = false
        } else if (caracteresRecibidos > 0 && count == 1 &&
            ahora - ultimaTeclaMs > MS_MAX_ENTRE_TECLAS
        ) {
            escrituraRapida = false
        }
        caracteresRecibidos += count
        ultimaTeclaMs = ahora

        handler.removeCallbacks(finDeRafaga)
        handler.postDelayed(finDeRafaga, MS_SILENCIO_FIN)
    }

    /**
     * Entrega el código si está completo.
     *
     * @param forzado `true` cuando el usuario o el lector pulsaron Enter/Listo:
     *        se procesa aunque el texto se haya escrito a mano.
     */
    private fun procesar(forzado: Boolean): Boolean {
        handler.removeCallbacks(finDeRafaga)

        val codigo = limpiar(campo.text?.toString().orEmpty())
        if (codigo.length < longitudMinima) return false
        // Sin Enter solo se acepta lo que escribió el lector, para no disparar
        // la búsqueda a media palabra cuando alguien teclea el código.
        if (!forzado && !escrituraRapida) return false

        val ahora = SystemClock.elapsedRealtime()
        if (codigo == ultimoCodigo && ahora - ultimoCodigoMs < MS_ANTIRREBOTE) return false
        ultimoCodigo = codigo
        ultimoCodigoMs = ahora

        procesando = true
        if (limpiarAlProcesar) {
            campo.setText("")
        } else if (campo.text?.toString() != codigo) {
            campo.setText(codigo)
            campo.setSelection(codigo.length)
        }
        procesando = false
        reiniciar()

        onCodigo(codigo)
        return true
    }

    /** Quita saltos de línea y tabuladores que algunos lectores añaden al final. */
    private fun limpiar(texto: String): String =
        texto.replace("\n", "").replace("\r", "").replace("\t", "").trim()

    private fun reiniciar() {
        handler.removeCallbacks(finDeRafaga)
        caracteresRecibidos = 0
        escrituraRapida = true
        ultimaTeclaMs = 0L
    }

    /** Deja el campo vacío y con el foco, listo para el siguiente escaneo. */
    fun prepararParaEscanear() {
        procesando = true
        if (campo.text?.isNotEmpty() == true) campo.setText("")
        procesando = false
        reiniciar()
        campo.requestFocus()
    }
}
