package com.github.livingwithhippos.unchained.folderlist.model

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.lists.view.DownloadListListener
import com.github.livingwithhippos.unchained.utilities.DataBindingTrackedAdapter

class FolderItemAdapter(listener: DownloadListListener) :
    DataBindingTrackedAdapter<DownloadItem, DownloadListListener>(
        DiffCallback(), listener
    ) {
    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            // content does not change on update
            return true
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download

    fun getFolderItem(position: Int): DownloadItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }
}

class FolderKeyProvider(private val adapter: FolderItemAdapter) :
    ItemKeyProvider<DownloadItem>(SCOPE_CACHED) {
    override fun getKey(position: Int): DownloadItem? {
        return adapter.getFolderItem(position)
    }

    override fun getPosition(key: DownloadItem): Int {
        return adapter.currentList.indexOfFirst { it.id == key.id }
    }
}
