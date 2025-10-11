package com.github.livingwithhippos.unchained.remotedevice.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.databinding.ItemListRemoteServiceBinding
import com.github.livingwithhippos.unchained.utilities.extension.setDrawableByServiceType

class RemoteServiceListAdapter(private val listener: ServiceListListener) :
    ListAdapter<RemoteService, RemoteServiceViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<RemoteService>? = null

    class DiffCallback : DiffUtil.ItemCallback<RemoteService>() {
        override fun areItemsTheSame(oldItem: RemoteService, newItem: RemoteService): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RemoteService, newItem: RemoteService): Boolean =
            oldItem.isDefault == newItem.isDefault &&
                oldItem.name == newItem.name &&
                oldItem.type == newItem.type &&
                oldItem.port == newItem.port
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteServiceViewHolder {
        val binding =
            ItemListRemoteServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RemoteServiceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: RemoteServiceViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_remote_service

    fun getService(position: Int): RemoteService? = super.getItem(position)

    fun getPosition(id: Int) = currentList.indexOfFirst { it.id == id }
}

class RemoteServiceViewHolder(
    private val binding: ItemListRemoteServiceBinding,
    private val listener: ServiceListListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: RemoteService) {
        binding.defaultIndicator.visibility = if (item.isDefault) View.VISIBLE else View.INVISIBLE

        binding.tvTitle.text = item.name
        binding.tvPort.text = item.port.toString()
        setDrawableByServiceType(binding.ivType, item.type)

        binding.cvDevice.setOnClickListener { listener.onServiceClick(item) }
    }
}

class ServiceKeyProvider(private val adapter: RemoteServiceListAdapter) :
    ItemKeyProvider<RemoteService>(SCOPE_MAPPED) {
    override fun getKey(position: Int): RemoteService? = adapter.getService(position)

    override fun getPosition(key: RemoteService): Int = adapter.getPosition(key.id)
}

interface ServiceListListener {
    fun onServiceClick(item: RemoteService)
}
