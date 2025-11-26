package com.github.livingwithhippos.unchained.torrentfilepicker.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemListTorrentSelectionDirectoryBinding
import com.github.livingwithhippos.unchained.databinding.ItemListTorrentSelectionFileBinding
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString

class TorrentContentFilesSelectionAdapter(private val listener: TorrentContentListener) :
    ListAdapter<TorrentFileItem, RecyclerView.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<TorrentFileItem>() {
        override fun areItemsTheSame(oldItem: TorrentFileItem, newItem: TorrentFileItem): Boolean {
            return oldItem.absolutePath == newItem.absolutePath &&
                oldItem.name == newItem.name &&
                oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TorrentFileItem,
            newItem: TorrentFileItem,
        ): Boolean {
            return oldItem.selected == newItem.selected
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.id == TYPE_FOLDER) R.layout.item_list_torrent_selection_directory
        else R.layout.item_list_torrent_selection_file
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_list_torrent_selection_directory -> {
                val binding =
                    ItemListTorrentSelectionDirectoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                DirectoryViewHolder(binding, listener)
            }

            R.layout.item_list_torrent_selection_file -> {
                val binding =
                    ItemListTorrentSelectionFileBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                FileViewHolder(binding, listener)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DirectoryViewHolder -> holder.bindCell(item)
            is FileViewHolder -> holder.bindCell(item)
        }
    }

    class DirectoryViewHolder(
        private val binding: ItemListTorrentSelectionDirectoryBinding,
        private val listener: TorrentContentListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindCell(item: TorrentFileItem) {
            binding.cbSelectDirectory.isChecked = item.selected
            binding.cbSelectDirectory.setOnClickListener { listener.onSelectedFolder(item) }
            binding.tvDirectoryName.text = item.name
        }
    }

    class FileViewHolder(
        private val binding: ItemListTorrentSelectionFileBinding,
        private val listener: TorrentContentListener,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindCell(item: TorrentFileItem) {
            binding.cbSelectFile.isChecked = item.selected
            binding.cbSelectFile.setOnClickListener { listener.onSelectedFile(item) }
            binding.fileListItem.setOnClickListener { binding.cbSelectFile.performClick() }
            binding.tvFileName.text = item.name
            binding.tvFileSize.text = getFileSizeString(itemView.context, item.bytes)
        }
    }
}
