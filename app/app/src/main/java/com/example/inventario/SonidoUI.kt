package com.example.inventario

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Reproduce un clic corto al presionar botones para que la app se sienta
 * más satisfactoria. Usa un único sonido cargado en memoria (res/raw/click.wav).
 */
object SonidoUI {

    private var soundPool: SoundPool? = null
    private var clickId: Int = 0
    private var cargado: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(atributos)
            .build()
            .apply {
                setOnLoadCompleteListener { _, _, status -> cargado = (status == 0) }
            }
        clickId = soundPool!!.load(context.applicationContext, R.raw.click, 1)
    }

    fun click(context: Context) {
        if (soundPool == null) init(context)
        if (cargado) {
            soundPool?.play(clickId, 0.45f, 0.45f, 1, 0, 1f)
        }
    }
}
