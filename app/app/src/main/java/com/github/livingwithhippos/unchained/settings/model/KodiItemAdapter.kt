package com.github.livingwithhippos.unchained.settings.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

class KodiDeviceAdapter(listener: KodiDeviceListener) :
    DataBindingAdapter<KodiDevice, KodiDeviceListener>(
        DiffCallback(), listener
    ) {
    class DiffCallback : DiffUtil.ItemCallback<KodiDevice>() {
        override fun areItemsTheSame(oldItem: KodiDevice, newItem: KodiDevice): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: KodiDevice, newItem: KodiDevice): Boolean =
            oldItem.address == newItem.address &&
                oldItem.port == newItem.port &&
                oldItem.name == newItem.name &&
                // these are not shown in the layout at the moment
                // oldItem.username == newItem.username &&
                // oldItem.password == newItem.password &&
                oldItem.isDefault == newItem.isDefault
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_kodi_device
}

interface KodiDeviceListener {
    fun onEditClick(item: KodiDevice)
}
