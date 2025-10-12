package com.github.livingwithhippos.unchained.search.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemListSearchBinding
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem

class SearchItemAdapter(private val listener: SearchItemListener) :
    ListAdapter<ScrapedItem, SearchItemViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<ScrapedItem>() {
        override fun areItemsTheSame(oldItem: ScrapedItem, newItem: ScrapedItem): Boolean =
            oldItem.link == newItem.link

        override fun areContentsTheSame(oldItem: ScrapedItem, newItem: ScrapedItem): Boolean {
            return oldItem.magnets.size == newItem.magnets.size &&
                oldItem.torrents.size == newItem.torrents.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        val binding =
            ItemListSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchItemViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_search
}

class SearchItemViewHolder(
    private val binding: ItemListSearchBinding,
    private val listener: SearchItemListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: ScrapedItem) {
        binding.tvName.text = item.name.trim()

        val linksCount = item.magnets.size + item.torrents.size + item.hosting.size
        binding.tvLinks.text =
            itemView.context.resources.getQuantityString(
                R.plurals.links_format,
                linksCount,
                linksCount,
            )
        binding.tvLinks.visibility =
            if (item.magnets.isEmpty() && item.hosting.isEmpty()) View.GONE else View.VISIBLE

        binding.tvSeeders.text =
            item.seeders?.let { itemView.context.getString(R.string.seeders_short_format, it) }
                ?: ""
        binding.tvSeeders.visibility = if (item.seeders == null) View.GONE else View.VISIBLE

        binding.tvLeechers.text =
            item.leechers?.let { itemView.context.getString(R.string.leechers_short_format, it) }
                ?: ""
        binding.tvLeechers.visibility = if (item.leechers == null) View.GONE else View.VISIBLE

        binding.tvSize.text = item.size ?: ""
        binding.tvSize.visibility = if (item.size == null) View.GONE else View.VISIBLE

        binding.cvScrapedItem.setOnClickListener { listener.onClick(item) }
    }
}

interface SearchItemListener {
    fun onClick(item: ScrapedItem)
}
