package com.github.livingwithhippos.unchained.downloaddetails.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteServiceDetails
import com.github.livingwithhippos.unchained.databinding.ItemListServicePickerBinding
import com.github.livingwithhippos.unchained.utilities.extension.setDrawableByServiceType

class ServicePickerAdapter(private val listener: ServicePickerListener) :
    ListAdapter<CompleteRemoteServiceDetails, CompleteRemoteServiceViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<CompleteRemoteServiceDetails>() {
        override fun areItemsTheSame(
            oldItem: CompleteRemoteServiceDetails,
            newItem: CompleteRemoteServiceDetails,
        ): Boolean = oldItem.service.id == newItem.service.id

        // content does not change on update
        override fun areContentsTheSame(
            oldItem: CompleteRemoteServiceDetails,
            newItem: CompleteRemoteServiceDetails,
        ): Boolean = true
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CompleteRemoteServiceViewHolder {
        val binding =
            ItemListServicePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompleteRemoteServiceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: CompleteRemoteServiceViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_service_picker
}

class CompleteRemoteServiceViewHolder(
    private val binding: ItemListServicePickerBinding,
    private val listener: ServicePickerListener,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bindCell(item: CompleteRemoteServiceDetails) {
        // fixme: had no getString originally?
        binding.serviceType.text = itemView.context.getString(item.type.nameRes)
        setDrawableByServiceType(binding.serviceIcon, item.service.type)
        binding.serviceName.text = item.service.name
        binding.serviceAddress.text = item.service.address
        binding.cvService.setOnClickListener { listener.onServiceClick(item) }
    }
}

interface ServicePickerListener {
    fun onServiceClick(service: CompleteRemoteServiceDetails)
}
