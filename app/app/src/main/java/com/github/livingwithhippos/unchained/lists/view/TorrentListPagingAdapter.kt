package com.github.livingwithhippos.unchained.lists.view

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.utilities.DataBindingPagingTrackedAdapter

class TorrentListPagingAdapter(listener: TorrentListListener) :
    DataBindingPagingTrackedAdapter<TorrentItem, TorrentListListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<TorrentItem>() {
        override fun areItemsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean {
            // check the torrent progress
            return oldItem.progress == newItem.progress &&
                // check the torrent status
                oldItem.status == newItem.status &&
                // may be triggered by different cache
                oldItem.bytes == newItem.bytes
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_torrent

    fun getTorrentItem(position: Int): TorrentItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }

    fun getPosition(id: String) = snapshot().indexOfFirst { it?.id == id }
}

interface TorrentListListener {
    fun onClick(item: TorrentItem)
}

class TorrentKeyProvider(private val adapter: TorrentListPagingAdapter) :
    ItemKeyProvider<TorrentItem>(SCOPE_MAPPED) {
    override fun getKey(position: Int): TorrentItem? {
        return adapter.getTorrentItem(position)
    }

    override fun getPosition(key: TorrentItem): Int {
        return adapter.getPosition(key.id)
    }
}
