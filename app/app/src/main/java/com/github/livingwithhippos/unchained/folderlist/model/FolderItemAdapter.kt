package com.github.livingwithhippos.unchained.folderlist.model

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
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.ItemListDownloadBinding
import com.github.livingwithhippos.unchained.lists.view.DownloadListListener
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString

class FolderItemAdapter(private val listener: DownloadListListener) :
    ListAdapter<DownloadItem, ItemFolderViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<DownloadItem>? = null

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean =
            oldItem.link == newItem.link

        // content does not change on update
        override fun areContentsTheSame(
            oldItem: DownloadItem,
            newItem: DownloadItem
        ): Boolean = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFolderViewHolder {
        val binding =
            ItemListDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemFolderViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: ItemFolderViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item, tracker?.isSelected(item) ?: false)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download

    fun getFolderItem(position: Int): DownloadItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }
}

class ItemFolderViewHolder(
    private val binding: ItemListDownloadBinding,
    private val listener: DownloadListListener
) : RecyclerView.ViewHolder(binding.root) {

    var mItem: DownloadItem? = null

    fun bindCell(item: DownloadItem, selected: Boolean) {
        mItem = item
        binding.selectionIndicator.visibility = if (selected) View.VISIBLE else View.GONE
        binding.tvTitle.text =
            if (item.streamable == 1) itemView.context.getString(R.string.streaming) else itemView.context.getString(
                R.string.download
            )
        binding.tvName.text = item.filename
        binding.tvSize.text = getFileSizeString(itemView.context, item.fileSize)
        binding.cvDownload.setOnClickListener { listener.onClick(item) }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<DownloadItem> =
        object : ItemDetailsLookup.ItemDetails<DownloadItem>() {
            override fun getPosition(): Int = layoutPosition

            override fun getSelectionKey(): DownloadItem? = mItem
        }
}

class FolderDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<DownloadItem>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<DownloadItem>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as ItemFolderViewHolder)
                .getItemDetails()
        }
        return null
    }
}

class FolderKeyProvider(private val adapter: FolderItemAdapter) :
    ItemKeyProvider<DownloadItem>(SCOPE_MAPPED) {
    override fun getKey(position: Int): DownloadItem? {
        return adapter.getFolderItem(position)
    }

    override fun getPosition(key: DownloadItem): Int {
        return adapter.currentList.indexOfFirst { it.id == key.id }
    }
}