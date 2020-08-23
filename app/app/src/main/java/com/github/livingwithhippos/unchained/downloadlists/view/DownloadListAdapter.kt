package com.github.livingwithhippos.unchained.downloadlists.view

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class DownloadListAdapter(listener: DownloadListListener) : DataBindingAdapter<DownloadItem, DownloadListListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id==newItem.id

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            //todo: add progress if present
           return oldItem.link == newItem.link
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download
}

interface DownloadListListener {
    fun onClick(id: String)
}