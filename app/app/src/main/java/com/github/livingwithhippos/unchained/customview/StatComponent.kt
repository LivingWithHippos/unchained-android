package com.github.livingwithhippos.unchained.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDividerItemDecoration

class StatComponent
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    MaterialCardView(context, attrs, defStyleAttr) {

    private val recyclerView: RecyclerView
    val adapter: StatAdapter
    private val showLabel: Boolean
    private val showCaption: Boolean
    private val showIcon: Boolean
    private val showDividers: Boolean
    private val direction: Int
    private val dividerItemDecoration: MaterialDividerItemDecoration

    init {
        inflate(context, R.layout.stat_component, this)

        recyclerView = findViewById(R.id.recyclerView)

        context.theme.obtainStyledAttributes(attrs, R.styleable.StatComponent, 0, 0).apply {
            try {
                cardElevation =
                    resources.getDimensionPixelSize(R.dimen.stat_corner_radius).toFloat()
                radius = resources.getDimensionPixelSize(R.dimen.stat_card_elevation).toFloat()
                strokeWidth = 0

                showLabel = getBoolean(R.styleable.StatItemComponent_show_label, true)
                showCaption = getBoolean(R.styleable.StatItemComponent_show_caption, true)
                showIcon = getBoolean(R.styleable.StatItemComponent_show_icon, true)
                showDividers = getBoolean(R.styleable.StatComponent_show_dividers, true)
                direction = getInteger(R.styleable.StatComponent_statDirection, 0)

                adapter = StatAdapter(showLabel, showCaption, showIcon)
                recyclerView.adapter = adapter

                val layoutManager: FlexboxLayoutManager = FlexboxLayoutManager(context)
                layoutManager.flexDirection =
                    if (direction == 1) FlexDirection.COLUMN else FlexDirection.ROW
                layoutManager.justifyContent = JustifyContent.CENTER
                layoutManager.alignItems = AlignItems.CENTER

                dividerItemDecoration =
                    MaterialDividerItemDecoration(
                        context,
                        if (direction == 1) MaterialDividerItemDecoration.VERTICAL
                        else MaterialDividerItemDecoration.HORIZONTAL)

                if (showDividers) {
                    recyclerView.addItemDecoration(dividerItemDecoration)
                }
                recyclerView.layoutManager = layoutManager
            } finally {
                recycle()
            }
        }
    }
}

data class StatItem(
    val content: String,
    val label: String,
    val caption: String,
    @DrawableRes val icon: Int
)

class StatAdapter(
    private val showLabel: Boolean,
    private val showCaption: Boolean,
    private val showIcon: Boolean
) : ListAdapter<StatItem, StatAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<StatItem>() {
        override fun areItemsTheSame(oldItem: StatItem, newItem: StatItem): Boolean =
            oldItem.content == newItem.content &&
                oldItem.label == newItem.label &&
                oldItem.caption == newItem.caption &&
                oldItem.icon == newItem.icon

        override fun areContentsTheSame(oldItem: StatItem, newItem: StatItem): Boolean = true
    }

    /** Provide a reference to the type of views that you are using (custom ViewHolder) */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelTextView: TextView = view.findViewById(R.id.tvLabel)
        val contentTextView: TextView = view.findViewById(R.id.tvContent)
        val captionTextView: TextView = view.findViewById(R.id.tvCaption)
        val iconImageView: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.stat_component_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val stat = getItem(position)

        viewHolder.contentTextView.text = stat.content

        if (showLabel.not()) viewHolder.labelTextView.visibility = View.GONE
        else viewHolder.labelTextView.text = stat.label

        if (showCaption.not()) viewHolder.captionTextView.visibility = View.GONE
        else viewHolder.captionTextView.text = stat.caption

        if (showIcon.not()) viewHolder.iconImageView.visibility = View.GONE
        else viewHolder.iconImageView.setImageResource(stat.icon)
    }

    override fun getItemViewType(position: Int) = R.layout.stat_component_item
}
