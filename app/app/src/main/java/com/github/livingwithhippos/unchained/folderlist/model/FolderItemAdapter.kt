package com.github.livingwithhippos.unchained.folderlist.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.lists.view.DownloadListListener
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class FolderItemAdapter(listener: DownloadListListener) :
    DataBindingAdapter<DownloadItem, DownloadListListener>(
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
}
