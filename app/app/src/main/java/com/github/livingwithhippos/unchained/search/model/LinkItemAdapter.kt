package com.github.livingwithhippos.unchained.search.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemListLinkBinding


class LinkItemAdapter(private val listener: LinkItemListener) :
    ListAdapter<LinkItem, ItemLinkViewHolder>(DiffCallback()) {


    class DiffCallback : DiffUtil.ItemCallback<LinkItem>() {
        override fun areItemsTheSame(oldItem: LinkItem, newItem: LinkItem): Boolean =
            oldItem.link == newItem.link

        // content does not change on update
        override fun areContentsTheSame(
            oldItem: LinkItem,
            newItem: LinkItem
        ): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemLinkViewHolder {
        val binding =
            ItemListLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemLinkViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ItemLinkViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_link
}

class ItemLinkViewHolder(
    private val binding: ItemListLinkBinding,
    private val listener: LinkItemListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: LinkItem) {
        binding.type.text = item.type
        binding.link.text = item.name
        binding.cvListLink.setOnClickListener { listener.onClick(item) }
        binding.cvListLink.setOnLongClickListener { listener.onLongClick(item) }
    }
}

interface LinkItemListener {
    fun onClick(item: LinkItem)

    fun onLongClick(item: LinkItem): Boolean
}

data class LinkItem(val type: String, val name: String, val link: String)
