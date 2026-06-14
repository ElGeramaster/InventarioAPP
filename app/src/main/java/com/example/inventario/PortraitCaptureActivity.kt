package com.example.inventario

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Actividad de captura del escáner de códigos forzada en orientación vertical.
 *
 * La actividad de captura por defecto de zxing-android-embedded abre la cámara
 * en horizontal. Esta subclase no agrega lógica: solo existe para declararla en
 * el AndroidManifest con android:screenOrientation="portrait" y así mostrar el
 * escáner en vertical.
 */
class PortraitCaptureActivity : CaptureActivity()
