package com.github.livingwithhippos.unchained.search.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class LinkItemAdapter(listener: LinkItemListener) :
    DataBindingAdapter<LinkItem, LinkItemListener>(DiffCallback(), listener) {
    class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {
        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem.link == newItem.link

        override fun areContentsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean {
            return true
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_link
}

interface LinkItemListener {
    fun onClick(item: LinkItem)

    fun onLongClick(item: LinkItem): Boolean
}

data class LinkItem(val type: String, val name: String, val link: String)
