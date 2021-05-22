package com.github.livingwithhippos.unchained.search.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class SearchItemAdapter(listener: SearchItemListener) :
    DataBindingAdapter<LinkData, SearchItemListener>(
        DiffCallback(), listener
    ) {
    class DiffCallback : DiffUtil.ItemCallback<LinkData>() {
        override fun areItemsTheSame(oldItem: LinkData, newItem: LinkData): Boolean =
            oldItem.link == newItem.link

        override fun areContentsTheSame(oldItem: LinkData, newItem: LinkData): Boolean {
            return oldItem.magnets.size == newItem.magnets.size
                    && oldItem.torrents.size == newItem.torrents.size
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_search
}

interface SearchItemListener {
    fun onClick(linkData: LinkData)
}