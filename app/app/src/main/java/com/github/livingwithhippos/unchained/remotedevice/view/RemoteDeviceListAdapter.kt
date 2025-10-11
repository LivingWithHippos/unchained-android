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
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.databinding.ItemListRemoteDeviceBinding

class RemoteDeviceListAdapter(private val listener: DeviceListListener) :
    ListAdapter<RemoteDevice, RemoteDeviceViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<RemoteDevice>? = null

    class DiffCallback : DiffUtil.ItemCallback<RemoteDevice>() {
        override fun areItemsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice): Boolean =
            oldItem.isDefault == newItem.isDefault &&
                oldItem.name == newItem.name &&
                oldItem.services == newItem.services &&
                oldItem.address == newItem.address
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteDeviceViewHolder {
        val binding =
            ItemListRemoteDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RemoteDeviceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: RemoteDeviceViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item, tracker?.isSelected(item) ?: false)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_remote_device

    fun getDevice(position: Int): RemoteDevice? = super.getItem(position)

    fun getPosition(id: Int) = currentList.indexOfFirst { it.id == id }
}

class RemoteDeviceViewHolder(
    private val binding: ItemListRemoteDeviceBinding,
    private val listener: DeviceListListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: RemoteDevice, selected: Boolean) {
        binding.defaultIndicator.visibility = if (item.isDefault) View.VISIBLE else View.INVISIBLE

        val servicesCount = item.services ?: 0
        binding.tvServices.text =
            if (item.services == null) ""
            else
                itemView.context.resources.getQuantityString(
                    R.plurals.services_format,
                    servicesCount,
                    servicesCount,
                )
        binding.tvServices.visibility = if (item.services == null) View.GONE else View.VISIBLE

        binding.tvTitle.text = item.name
        binding.tvDetails.text = item.address

        binding.cvDevice.setOnClickListener { listener.onDeviceClick(item) }
    }
}

class DeviceKeyProvider(private val adapter: RemoteDeviceListAdapter) :
    ItemKeyProvider<RemoteDevice>(SCOPE_MAPPED) {
    override fun getKey(position: Int): RemoteDevice? = adapter.getDevice(position)

    override fun getPosition(key: RemoteDevice): Int = adapter.getPosition(key.id)
}

interface DeviceListListener {
    fun onDeviceClick(item: RemoteDevice)
}
