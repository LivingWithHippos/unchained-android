package com.github.livingwithhippos.unchained.downloaddetails.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceDetails
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter


class ServicePickerAdapter(listener: ServicePickerListener) :
    DataBindingAdapter<RemoteServiceDetails, ServicePickerListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<RemoteServiceDetails>() {
        override fun areItemsTheSame(oldItem: RemoteServiceDetails, newItem: RemoteServiceDetails): Boolean =
            oldItem.service.id == newItem.service.id

        override fun areContentsTheSame(oldItem: RemoteServiceDetails, newItem: RemoteServiceDetails): Boolean {
            // content does not change on update
            return true
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_service_picker
}

interface ServicePickerListener {
    fun onServiceClick(serviceDetails: RemoteServiceDetails)
}
