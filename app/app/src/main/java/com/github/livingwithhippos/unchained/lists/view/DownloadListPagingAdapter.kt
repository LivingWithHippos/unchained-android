package com.github.livingwithhippos.unchained.lists.view

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.utilities.DataBindingPagingAdapter
import com.github.livingwithhippos.unchained.utilities.DataBindingPagingTrackedAdapter

/**
 * A [DataBindingPagingAdapter] subclass.
 * Displays a list of [DownloadItem].
 */
class DownloadListPagingAdapter(listener: DownloadListListener) :
    DataBindingPagingTrackedAdapter<DownloadItem, DownloadListListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            // content does not change on update
            return true
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download

    fun getDownloadItem(position: Int): DownloadItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }

    fun getPosition(id: String) = snapshot().indexOfFirst { it?.id == id }
}

interface DownloadListListener {
    fun onClick(item: DownloadItem)
    fun onLongClick(item: DownloadItem)
}

class DownloadKeyProvider(private val adapter: DownloadListPagingAdapter) :
    ItemKeyProvider<DownloadItem>(SCOPE_CACHED) {
    override fun getKey(position: Int): DownloadItem? {
        return adapter.getDownloadItem(position)
    }

    override fun getPosition(key: DownloadItem): Int {
        return adapter.getPosition(key.id)
    }
}

