package com.github.livingwithhippos.unchained.remotedevice.view

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.utilities.DataBindingTrackedAdapter


class RemoteDeviceListAdapter(listener: DeviceListListener): DataBindingTrackedAdapter<RemoteDevice, DeviceListListener>(DiffCallback(), listener) {
    class DiffCallback : DiffUtil.ItemCallback<RemoteDevice>() {
        override fun areItemsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice): Boolean {
            return oldItem.isDefault == newItem.isDefault
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_remote_device


    fun getDevice(position: Int): RemoteDevice? {
        return super.getItem(position)
    }

    fun getPosition(id: Int) = currentList.indexOfFirst { it.id == id }
}

class DeviceKeyProvider(private val adapter: RemoteDeviceListAdapter) : ItemKeyProvider<RemoteDevice>(SCOPE_MAPPED) {
    override fun getKey(position: Int): RemoteDevice? =
        adapter.getDevice(position)

    override fun getPosition(key: RemoteDevice): Int = adapter.getPosition(key.id)
}



interface DeviceListListener {
    fun onClick(item: RemoteDevice)
}