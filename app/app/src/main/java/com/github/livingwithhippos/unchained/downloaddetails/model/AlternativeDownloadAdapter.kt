package com.github.livingwithhippos.unchained.downloaddetails.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.Alternative
import com.github.livingwithhippos.unchained.downloaddetails.view.DownloadDetailsListener
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class AlternativeDownloadAdapter(listener: DownloadDetailsListener) :
    DataBindingAdapter<Alternative, DownloadDetailsListener>(
        DiffCallback(), listener
    ) {

    class DiffCallback : DiffUtil.ItemCallback<Alternative>() {
        override fun areItemsTheSame(oldItem: Alternative, newItem: Alternative): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Alternative, newItem: Alternative): Boolean {
            // content does not change on update
            return true
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_alternative_download
}
