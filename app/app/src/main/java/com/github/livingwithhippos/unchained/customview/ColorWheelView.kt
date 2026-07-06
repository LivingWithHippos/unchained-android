package com.github.livingwithhippos.unchained.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An HSV color wheel: hue is the angle around the wheel, saturation is the distance from the
 * center (fully saturated at the edge, white at the center). Brightness (HSV value) isn't
 * represented on the wheel itself, since a 2D wheel can only encode two of the three HSV
 * components; pair this with a separate brightness control that sets [value].
 */
class ColorWheelView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = SELECTOR_STROKE_WIDTH
            color = Color.WHITE
        }
    private val selectorOutlinePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = SELECTOR_STROKE_WIDTH / 2
            color = Color.BLACK
            alpha = 128
        }

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    /** Degrees, 0-360 */
    var hue = 0f
        private set

    /** 0-1 */
    var saturation = 0f
        private set

    /** 0-1. Not represented on the wheel; set this from a separate brightness control. */
    var value = 1f
        set(newValue) {
            field = newValue.coerceIn(0f, 1f)
            invalidate()
        }

    var onColorChanged: ((Int) -> Unit)? = null

    val selectedColor: Int
        get() = Color.HSVToColor(floatArrayOf(hue, saturation, value))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - SELECTOR_STROKE_WIDTH

        if (radius <= 0f) return

        val hues = IntArray(361) { degree -> Color.HSVToColor(floatArrayOf(degree.toFloat(), 1f, 1f)) }
        val sweep = SweepGradient(centerX, centerY, hues, null)
        val radial =
            RadialGradient(
                centerX,
                centerY,
                radius,
                Color.WHITE,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        // the white-to-transparent radial gradient is layered on top of (SRC) the rainbow sweep
        // gradient (DST), desaturating toward the center without needing two separate draw passes
        wheelPaint.shader = ComposeShader(radial, sweep, PorterDuff.Mode.SRC_OVER)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (radius <= 0f) return

        canvas.drawCircle(centerX, centerY, radius, wheelPaint)

        val angleRad = Math.toRadians(hue.toDouble())
        val selectorDistance = saturation * radius
        val selectorX = centerX + (selectorDistance * cos(angleRad)).toFloat()
        val selectorY = centerY + (selectorDistance * sin(angleRad)).toFloat()
        canvas.drawCircle(selectorX, selectorY, SELECTOR_RADIUS, selectorOutlinePaint)
        canvas.drawCircle(selectorX, selectorY, SELECTOR_RADIUS, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                updateFromTouch(event.x, event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFromTouch(x: Float, y: Float) {
        if (radius <= 0f) return
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        hue = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
        saturation = (distance / radius).coerceIn(0f, 1f)

        invalidate()
        onColorChanged?.invoke(selectedColor)
    }

    /** Programmatically seed the wheel from an existing color, e.g. a quick-select swatch. */
    fun setColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        invalidate()
    }

    companion object {
        private const val SELECTOR_STROKE_WIDTH = 6f
        private const val SELECTOR_RADIUS = 24f
    }
}
