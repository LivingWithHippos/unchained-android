package com.github.livingwithhippos.unchained.remotedevice.view

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.utilities.DataBindingTrackedAdapter

class RemoteServiceListAdapter(listener: ServiceListListener) :
    DataBindingTrackedAdapter<RemoteService, ServiceListListener>(DiffCallback(), listener) {
    class DiffCallback : DiffUtil.ItemCallback<RemoteService>() {
        override fun areItemsTheSame(oldItem: RemoteService, newItem: RemoteService): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RemoteService, newItem: RemoteService): Boolean {
            return oldItem.isDefault == newItem.isDefault &&
                oldItem.name == newItem.name &&
                oldItem.type == newItem.type &&
                oldItem.port == newItem.port
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_remote_service

    fun getDevice(position: Int): RemoteService? {
        return super.getItem(position)
    }

    fun getPosition(id: Int) = currentList.indexOfFirst { it.id == id }
}

class ServiceKeyProvider(private val adapter: RemoteServiceListAdapter) :
    ItemKeyProvider<RemoteService>(SCOPE_MAPPED) {
    override fun getKey(position: Int): RemoteService? = adapter.getDevice(position)

    override fun getPosition(key: RemoteService): Int = adapter.getPosition(key.id)
}

interface ServiceListListener {
    fun onServiceClick(item: RemoteService)
}
