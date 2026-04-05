package com.github.livingwithhippos.unchained.remoteservice.view

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.databinding.ItemCompleteServiceBinding
import com.github.livingwithhippos.unchained.utilities.extension.setDrawableByServiceType

class CompleteServiceListAdapter(private val listener: CompleteServiceListListener) :
    ListAdapter<CompleteRemoteService, CompleteRemoteServiceViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<CompleteRemoteService>? = null

    class DiffCallback : DiffUtil.ItemCallback<CompleteRemoteService>() {
        override fun areItemsTheSame(
            oldItem: CompleteRemoteService,
            newItem: CompleteRemoteService,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: CompleteRemoteService,
            newItem: CompleteRemoteService,
        ): Boolean =
            oldItem.isDefault == newItem.isDefault &&
                oldItem.name == newItem.name &&
                oldItem.type == newItem.type &&
                oldItem.address == newItem.address
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CompleteRemoteServiceViewHolder {
        val binding =
            ItemCompleteServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CompleteRemoteServiceViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: CompleteRemoteServiceViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_complete_service

    fun getService(position: Int): CompleteRemoteService? = super.getItem(position)

    fun getPosition(id: Int) = currentList.indexOfFirst { it.id == id }
}

class CompleteRemoteServiceViewHolder(
    private val binding: ItemCompleteServiceBinding,
    private val listener: CompleteServiceListListener,
) : RecyclerView.ViewHolder(binding.root) {

    var mItem: CompleteRemoteService? = null

    fun bindCell(item: CompleteRemoteService) {
        mItem = item
        binding.defaultIndicator.visibility = if (item.isDefault) View.VISIBLE else View.INVISIBLE

        binding.tvName.text = item.name
        binding.tvAddress.text = item.address.trim()
        setDrawableByServiceType(binding.ivType, item.type)

        binding.cvService.setOnClickListener { listener.onServiceClick(item) }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<CompleteRemoteService> =
        object : ItemDetailsLookup.ItemDetails<CompleteRemoteService>() {
            override fun getPosition(): Int = layoutPosition

            override fun getSelectionKey(): CompleteRemoteService? = mItem
        }
}

class CompleteServiceDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<CompleteRemoteService>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<CompleteRemoteService>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as CompleteRemoteServiceViewHolder)
                .getItemDetails()
        }
        return null
    }
}

class CompleteServiceKeyProvider(private val adapter: CompleteServiceListAdapter) :
    ItemKeyProvider<CompleteRemoteService>(SCOPE_MAPPED) {
    override fun getKey(position: Int): CompleteRemoteService? = adapter.getService(position)

    override fun getPosition(key: CompleteRemoteService): Int = adapter.getPosition(key.id)
}

interface CompleteServiceListListener {
    fun onServiceClick(item: CompleteRemoteService)
}
