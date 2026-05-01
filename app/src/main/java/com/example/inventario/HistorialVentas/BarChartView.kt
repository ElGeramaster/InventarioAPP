package com.example.inventario.HistorialVentas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarEntry(val label: String, val value: Float)

    private var entries: List<BarEntry> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#43A047")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32")
        textSize = 26f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 2f
    }

    fun setData(data: List<BarEntry>) {
        entries = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val paddingLeft = 16f
        val paddingRight = 16f
        val paddingTop = 40f
        val paddingBottom = 48f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxValue = entries.maxOf { it.value }.takeIf { it > 0f } ?: 1f

        val barCount = entries.size
        val totalGap = chartWidth * 0.3f
        val gapWidth = if (barCount > 1) totalGap / (barCount - 1) else 0f
        val barWidth = (chartWidth - totalGap) / barCount

        val baseline = paddingTop + chartHeight
        canvas.drawLine(paddingLeft, baseline, paddingLeft + chartWidth, baseline, baselinePaint)

        entries.forEachIndexed { index, entry ->
            val barLeft = paddingLeft + index * (barWidth + gapWidth)
            val barTop = baseline - (entry.value / maxValue) * chartHeight * 0.9f
            val barRight = barLeft + barWidth
            val rect = RectF(barLeft, barTop, barRight, baseline)

            barPaint.color = if (index % 2 == 0) Color.parseColor("#43A047") else Color.parseColor("#66BB6A")
            canvas.drawRoundRect(rect, 8f, 8f, barPaint)

            val cx = barLeft + barWidth / 2
            canvas.drawText(entry.label, cx, baseline + paddingBottom - 8f, labelPaint)

            if (entry.value > 0f) {
                val valueText = if (entry.value >= 1000f) "${"%.0f".format(entry.value / 1000)}k"
                                else "${"%.0f".format(entry.value)}"
                canvas.drawText(valueText, cx, barTop - 8f, valuePaint)
            }
        }
    }
}
