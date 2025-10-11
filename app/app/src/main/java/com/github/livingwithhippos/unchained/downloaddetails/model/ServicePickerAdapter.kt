package com.github.livingwithhippos.unchained.downloaddetails.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteServiceDetails
import com.github.livingwithhippos.unchained.databinding.ItemListServicePickerBinding
import com.github.livingwithhippos.unchained.utilities.extension.setDrawableByServiceType

class ServicePickerAdapter(private val listener: ServicePickerListener) :
    ListAdapter<RemoteServiceDetails, RemoteServiceViewHolder>(DiffCallback()) {


    class DiffCallback : DiffUtil.ItemCallback<RemoteServiceDetails>() {
        override fun areItemsTheSame(
            oldItem: RemoteServiceDetails,
            newItem: RemoteServiceDetails,
        ): Boolean = oldItem.service.id == newItem.service.id

        // content does not change on update
        override fun areContentsTheSame(
            oldItem: RemoteServiceDetails,
            newItem: RemoteServiceDetails
        ): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteServiceViewHolder {
        val binding =
            ItemListServicePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RemoteServiceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: RemoteServiceViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_service_picker
}

class RemoteServiceViewHolder(
    private val binding: ItemListServicePickerBinding,
    private val listener: ServicePickerListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bindCell(item: RemoteServiceDetails) {
        // fixme: had no getString originally?
        binding.serviceType.text = itemView.context.getString(item.type.nameRes)
        setDrawableByServiceType(binding.serviceIcon, item.service.type)
        binding.serviceName.text = item.service.name
        binding.serviceAddress.text = "${item.device.address}:${item.service.port}"
        binding.cvService.setOnClickListener { listener.onServiceClick(item) }
    }
}

interface ServicePickerListener {
    fun onServiceClick(serviceDetails: RemoteServiceDetails)
}
