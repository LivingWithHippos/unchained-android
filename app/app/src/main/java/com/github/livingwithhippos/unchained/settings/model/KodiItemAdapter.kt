package com.github.livingwithhippos.unchained.settings.model

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.databinding.ItemListKodiDeviceBinding


class KodiDeviceAdapter(private val listener: KodiDeviceListener) :
    ListAdapter<KodiDevice, KodiDeviceViewHolder>(DiffCallback()) {


    class DiffCallback : DiffUtil.ItemCallback<KodiDevice>() {
        override fun areItemsTheSame(oldItem: KodiDevice, newItem: KodiDevice): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(
            oldItem: KodiDevice,
            newItem: KodiDevice
        ): Boolean =
            oldItem.address == newItem.address &&
                    oldItem.port == newItem.port &&
                    oldItem.name == newItem.name &&
                    // these are not shown in the layout at the moment
                    // oldItem.username == newItem.username &&
                    // oldItem.password == newItem.password &&
                    oldItem.isDefault == newItem.isDefault
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KodiDeviceViewHolder {
        val binding =
            ItemListKodiDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KodiDeviceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: KodiDeviceViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindCell(item)
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_kodi_device
}

class KodiDeviceViewHolder(
    private val binding: ItemListKodiDeviceBinding,
    private val listener: KodiDeviceListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bindCell(item: KodiDevice) {
        binding.defaultIndicator.visibility = if (item.isDefault) View.VISIBLE else View.INVISIBLE
        binding.tvTitle.text = item.name
        binding.tvDetails.text = "${item.address}:${item.port}"
        binding.cvDevice.setOnClickListener { listener.onEditClick(item) }
    }
}

interface KodiDeviceListener {
    fun onEditClick(item: KodiDevice)
}
