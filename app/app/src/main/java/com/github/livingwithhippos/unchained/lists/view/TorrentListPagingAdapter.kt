package com.github.livingwithhippos.unchained.lists.view

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.ItemListTorrentBinding
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import com.github.livingwithhippos.unchained.utilities.extension.getStatusTranslation

class TorrentListPagingAdapter(private val listener: TorrentListListener) :
    PagingDataAdapter<TorrentItem, TorrentViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<TorrentItem>? = null

    class DiffCallback : DiffUtil.ItemCallback<TorrentItem>() {
        override fun areItemsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean =
            oldItem.id == newItem.id

        // content does not change on update
        override fun areContentsTheSame(oldItem: TorrentItem, newItem: TorrentItem): Boolean {
            // check the torrent progress
            return oldItem.progress == newItem.progress &&
                // check the torrent status
                oldItem.status == newItem.status &&
                // may be triggered by different cache
                oldItem.bytes == newItem.bytes
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder {
        val binding =
            ItemListTorrentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TorrentViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item, tracker?.isSelected(item) ?: false)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_torrent

    fun getTorrentItem(position: Int): TorrentItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }

    fun getPosition(id: String) = snapshot().indexOfFirst { it?.id == id }
}

class TorrentViewHolder(
    private val binding: ItemListTorrentBinding,
    private val listener: TorrentListListener,
) : RecyclerView.ViewHolder(binding.root) {

    var mItem: TorrentItem? = null

    fun bindCell(item: TorrentItem, selected: Boolean) {
        mItem = item
        binding.selectionIndicator.visibility = if (selected) View.VISIBLE else View.GONE

        if (item.status == "downloaded") {
            // "ready" is used to make it clearer that the torrent is NOT downloaded on the phone
            binding.tvTitle.text = binding.root.context.getStatusTranslation("ready")
        } else
            binding.tvTitle.text = binding.root.context.getStatusTranslation(item.status)
        if (item.progress >= 0 && item.progress < 100) {
            binding.tvProgress.text =
                itemView.context.getString(R.string.percent_format, item.progress)
            binding.tvProgress.visibility = View.VISIBLE
        } else {
            binding.tvProgress.visibility = View.GONE
        }
        binding.tvName.text = item.filename
        binding.tvSize.text = getFileSizeString(itemView.context, item.bytes)

        binding.cvTorrent.setOnClickListener { listener.onClick(item) }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<TorrentItem> =
        object : ItemDetailsLookup.ItemDetails<TorrentItem>() {
            override fun getPosition(): Int = layoutPosition

            override fun getSelectionKey(): TorrentItem? = mItem
        }
}

class TorrentDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<TorrentItem>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<TorrentItem>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as TorrentViewHolder).getItemDetails()
        }
        return null
    }
}

interface TorrentListListener {
    fun onClick(item: TorrentItem)
}

class TorrentKeyProvider(private val adapter: TorrentListPagingAdapter) :
    ItemKeyProvider<TorrentItem>(SCOPE_MAPPED) {
    override fun getKey(position: Int): TorrentItem? {
        return adapter.getTorrentItem(position)
    }

    override fun getPosition(key: TorrentItem): Int {
        return adapter.getPosition(key.id)
    }
}
