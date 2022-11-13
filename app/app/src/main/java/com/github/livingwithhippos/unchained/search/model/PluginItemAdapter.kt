package com.github.livingwithhippos.unchained.search.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter


class PluginItemAdapter(listener: PluginItemListener) :
    DataBindingAdapter<RemotePlugin, PluginItemListener>(
        DiffCallback(), listener
    ) {
    class DiffCallback : DiffUtil.ItemCallback<RemotePlugin>() {
        override fun areItemsTheSame(oldItem: RemotePlugin, newItem: RemotePlugin): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RemotePlugin, newItem: RemotePlugin): Boolean {
            return oldItem.versions.size == newItem.versions.size && oldItem.status == newItem.status
        }
    }


    override fun getItemViewType(position: Int): Int {
        val item: RemotePlugin = this.getItem(position)
        return when (item.status) {
            PluginStatus.uncompatible -> {
                R.layout.item_list_plugin_uncompatible
            }
            PluginStatus.isNew -> {
                R.layout.item_list_plugin_new
            }
            PluginStatus.ready -> {
                R.layout.item_list_plugin_ready
            }
            PluginStatus.hasUpdate -> {
                R.layout.item_list_plugin_update
            }
            else -> {
                // should not happen
                R.layout.item_list_plugin_uncompatible
            }
        }
    }
}

interface PluginItemListener {
    fun onClick(item: RemotePlugin)
}
