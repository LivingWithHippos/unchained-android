package com.github.livingwithhippos.unchained.customview


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.github.livingwithhippos.unchained.R

class ThemeColorsCircle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var topColor: Int
    var bottomLeftColor: Int
    var bottomRightColor: Int

    init {

        context.theme.obtainStyledAttributes(attrs, R.styleable.ThemeColorsCircle, 0, 0).apply {
            try {

                topColor = getColor(R.styleable.ThemeColorsCircle_topColor, ContextCompat.getColor(context, R.color.green_one_theme_primary))
                bottomLeftColor = getColor(R.styleable.ThemeColorsCircle_bottomLeftColor, ContextCompat.getColor(context, R.color.green_one_theme_surface))
                bottomRightColor = getColor(R.styleable.ThemeColorsCircle_bottomRightColor, ContextCompat.getColor(context, R.color.green_one_theme_primaryContainer))

            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val radius = width.coerceAtMost(height) / 2
        val centerX = width / 2
        val centerY = height / 2

        // Draw top half circle
        paint.color = topColor
        canvas.drawArc(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            180f, 180f, true, paint
        )

        // Draw bottom left quarter circle
        paint.color = bottomLeftColor
        canvas.drawArc(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            0f, 90f, true, paint
        )

        // Draw bottom right quarter circle
        paint.color = bottomRightColor
        canvas.drawArc(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            90f, 90f, true, paint
        )
    }
}
