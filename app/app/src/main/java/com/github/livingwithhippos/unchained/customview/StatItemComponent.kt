package com.github.livingwithhippos.unchained.customview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.getResourceIdOrThrow
import com.github.livingwithhippos.unchained.R


class StatItemComponent
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val tvLabel: TextView
    private val tvContent: TextView
    private val tvCaption: TextView
    private val ivIcon: ImageView

    init {
        inflate(context, R.layout.stat_component_item, this)

        context.theme.obtainStyledAttributes(attrs, R.styleable.StatItemComponent, 0, 0).apply {
            try {

                tvLabel = findViewById(R.id.tvLabel)
                tvContent = findViewById(R.id.tvContent)
                tvCaption = findViewById(R.id.tvCaption)
                ivIcon = findViewById(R.id.ivIcon)

                val showLabel = getBoolean(R.styleable.StatItemComponent_show_label, true)
                val label = getString(R.styleable.StatItemComponent_label) ?: ""
                val showCaption = getBoolean(R.styleable.StatItemComponent_show_caption, true)
                val caption = getString(R.styleable.StatItemComponent_caption) ?: ""
                val content = getString(R.styleable.StatItemComponent_item_content) ?: ""
                val showIcon = getBoolean(R.styleable.StatItemComponent_show_icon, true)
                val icon = getResourceIdOrThrow(R.styleable.StatItemComponent_item_icon)

                tvContent.text = content

                if (!showLabel) {
                    tvLabel.visibility = View.GONE
                } else {
                    tvLabel.text = label
                }
                if (!showCaption) {
                    tvCaption.visibility = View.GONE
                } else {
                    tvCaption.text = caption
                }

                if (!showIcon) {
                    ivIcon.visibility = View.GONE
                } else {
                    ivIcon.setImageResource(icon)
                }
            } finally {
                recycle()
            }
        }
    }

    fun setLabel(
        label: String
    ) {
        tvLabel.text = label
        invalidate()
        requestLayout()
    }

    fun showLabel(show: Boolean) {
        tvLabel.visibility = if (show) View.VISIBLE else View.GONE
        invalidate()
        requestLayout()
    }

    fun setContent(
        content: String
    ) {
        tvContent.text = content
        invalidate()
        requestLayout()
    }

    fun showContent(show: Boolean) {
        tvContent.visibility = if (show) View.VISIBLE else View.GONE
        invalidate()
        requestLayout()
    }

    fun setCaption(
        caption: String
    ) {
        tvCaption.text = caption
        invalidate()
        requestLayout()
    }

    fun showCaption(show: Boolean) {
        tvCaption.visibility = if (show) View.VISIBLE else View.GONE
        invalidate()
        requestLayout()
    }

    fun setIcon(
        @DrawableRes icon: Int
    ) {
        ivIcon.setImageResource(icon)
        invalidate()
        requestLayout()
    }

    fun showIcon(show: Boolean) {
        ivIcon.visibility = if (show) View.VISIBLE else View.GONE
        invalidate()
        requestLayout()
    }
}