package com.github.livingwithhippos.unchained.lists.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.ItemListDownloadBinding
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString

class DownloadListPagingAdapter(private val listener: DownloadListListener) :
    PagingDataAdapter<DownloadItem, DownloadViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<DownloadItem>? = null

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.id == newItem.id

        // content does not change on update
        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding =
            ItemListDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item, tracker?.isSelected(item) ?: false)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download

    fun getDownloadItem(position: Int): DownloadItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }

    fun getPosition(id: String) = snapshot().indexOfFirst { it?.id == id }
}

class DownloadViewHolder(
    private val binding: ItemListDownloadBinding,
    private val listener: DownloadListListener,
) : RecyclerView.ViewHolder(binding.root) {

    var mItem: DownloadItem? = null

    fun bindCell(item: DownloadItem, selected: Boolean) {
        mItem = item
        binding.tvTitle.text =
            if (item.streamable == 1) itemView.context.getString(R.string.streaming)
            else itemView.context.getString(R.string.download)
        binding.tvName.text = item.filename
        binding.tvSize.text = getFileSizeString(itemView.context, item.fileSize)
        binding.selectionIndicator.visibility = if (selected) View.VISIBLE else View.GONE
        binding.cvDownload.setOnClickListener { listener.onClick(item) }
    }
}

interface DownloadListListener {
    fun onClick(item: DownloadItem)
}

class DownloadKeyProvider(private val adapter: DownloadListPagingAdapter) :
    ItemKeyProvider<DownloadItem>(SCOPE_MAPPED) {
    override fun getKey(position: Int): DownloadItem? {
        return adapter.getDownloadItem(position)
    }

    override fun getPosition(key: DownloadItem): Int {
        return adapter.getPosition(key.id)
    }
}
