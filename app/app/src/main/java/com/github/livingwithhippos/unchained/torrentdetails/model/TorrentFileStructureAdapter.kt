package com.github.livingwithhippos.unchained.torrentdetails.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.ItemListTorrentDirectoryBinding
import com.github.livingwithhippos.unchained.databinding.ItemListTorrentFileBinding
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString

data class TorrentFileItem(
    val id: Int,
    val absolutePath: String,
    val bytes: Long,
    var selected: Boolean,
    val name: String,
) {
    override fun equals(other: Any?): Boolean {
        return if (other is TorrentFileItem)
            this.id == other.id &&
                this.absolutePath == other.absolutePath &&
                this.name == other.name
        else super.equals(other)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + absolutePath.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    companion object {
        const val TYPE_FOLDER = -1
    }
}

fun getFilesNodes(
    item: TorrentItem,
    selectedOnly: Boolean = false,
    flattenFolders: Boolean = false,
): Node<TorrentFileItem> {
    val rootFolder = Node(TorrentFileItem(TYPE_FOLDER, "", 0, selected = false, "/"))

    if (!item.files.isNullOrEmpty()) {
        val files = if (selectedOnly) item.files.filter { it.selected == 1 } else item.files
        for (file in files) {
            val paths = file.path.split("/").drop(1)
            // todo: just use InnerTorrentFile instead of TorrentFileItem
            var currentNode = rootFolder
            paths.forEachIndexed { index, value ->
                if (index == paths.lastIndex) {
                    // this is a file, end of path
                    currentNode.children.add(
                        Node(
                            TorrentFileItem(
                                file.id,
                                paths.dropLast(1).joinToString("/"),
                                file.bytes,
                                selected = file.selected == 1,
                                value,
                            )
                        )
                    )
                } else {
                    // this is a folder, if we are not in flattening mode we add it to the list
                    if (flattenFolders.not()) {
                        val node = currentNode.children.firstOrNull { it.value.name == value }
                        if (node == null) {
                            currentNode.children.add(
                                Node(
                                    TorrentFileItem(
                                        TYPE_FOLDER,
                                        paths.subList(0, index + 1).joinToString("/"),
                                        0,
                                        selected = false,
                                        value,
                                    )
                                )
                            )
                        }

                        currentNode = currentNode.children.first { it.value.name == value }
                    }
                }
            }
        }
    }

    return rootFolder
}

interface TorrentContentListener {
    fun onSelectedFile(item: TorrentFileItem)

    fun onSelectedFolder(item: TorrentFileItem)
}

class TorrentContentFilesAdapter :
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
            return true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.id == TYPE_FOLDER) R.layout.item_list_torrent_directory
        else R.layout.item_list_torrent_file
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_list_torrent_directory -> {
                val binding =
                    ItemListTorrentDirectoryBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                DirectoryViewHolder(binding)
            }

            R.layout.item_list_torrent_file -> {
                val binding =
                    ItemListTorrentFileBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                FileViewHolder(binding)
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

    class DirectoryViewHolder(private val binding: ItemListTorrentDirectoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindCell(item: TorrentFileItem) {
            binding.tvDirectoryName.text = item.name
        }
    }

    class FileViewHolder(private val binding: ItemListTorrentFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindCell(item: TorrentFileItem) {
            binding.tvFileID.text = item.id.toString()
            binding.tvFileName.text = item.name
            binding.tvFileSize.text = getFileSizeString(itemView.context, item.bytes)
        }
    }
}
