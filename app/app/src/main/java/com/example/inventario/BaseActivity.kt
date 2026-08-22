package com.example.inventario

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity base que reproduce un sonido de clic cuando se toca cualquier vista
 * que responda a clics (botones, FAB, items de menú, tarjetas de producto...).
 *
 * Se hace en [dispatchTouchEvent] para cubrir también las vistas creadas de
 * forma dinámica (por ejemplo los botones "Agregar" de la cuadrícula).
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonidoUI.init(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val raiz = window?.decorView
            if (raiz != null && hayVistaClicable(raiz, ev.rawX.toInt(), ev.rawY.toInt())) {
                SonidoUI.click(this)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /** Busca la vista clicable que está bajo las coordenadas indicadas. */
    private fun hayVistaClicable(vista: View, x: Int, y: Int): Boolean {
        if (vista.visibility != View.VISIBLE) return false

        val loc = IntArray(2)
        vista.getLocationOnScreen(loc)
        val rect = Rect(loc[0], loc[1], loc[0] + vista.width, loc[1] + vista.height)
        if (!rect.contains(x, y)) return false

        if (vista is ViewGroup) {
            for (i in vista.childCount - 1 downTo 0) {
                if (hayVistaClicable(vista.getChildAt(i), x, y)) return true
            }
        }
        return vista.isClickable
    }
}
