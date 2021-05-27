package com.github.livingwithhippos.unchained.search.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.plugins.ScrapedItem
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class SearchItemAdapter(listener: SearchItemListener) :
    DataBindingAdapter<ScrapedItem, SearchItemListener>(
        DiffCallback(), listener
    ) {
    class DiffCallback : DiffUtil.ItemCallback<ScrapedItem>() {
        override fun areItemsTheSame(oldItem: ScrapedItem, newItem: ScrapedItem): Boolean =
            oldItem.link == newItem.link

        override fun areContentsTheSame(oldItem: ScrapedItem, newItem: ScrapedItem): Boolean {
            return oldItem.magnets.size == newItem.magnets.size
                    && oldItem.torrents.size == newItem.torrents.size
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_search
}

interface SearchItemListener {
    fun onClick(item: ScrapedItem)
}