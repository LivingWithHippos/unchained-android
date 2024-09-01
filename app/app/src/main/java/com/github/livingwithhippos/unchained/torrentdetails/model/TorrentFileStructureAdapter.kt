package com.github.livingwithhippos.unchained.torrentdetails.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter
import com.github.livingwithhippos.unchained.utilities.DataBindingStaticAdapter
import com.github.livingwithhippos.unchained.utilities.Node

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

class TorrentContentFilesAdapter : DataBindingStaticAdapter<TorrentFileItem>(DiffCallback()) {

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
            // content is not dynamic unless selected is added
            return true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item: TorrentFileItem = this.getItem(position)
        return if (item.id == TYPE_FOLDER) R.layout.item_list_torrent_directory
        else R.layout.item_list_torrent_file
    }
}

class TorrentContentFilesSelectionAdapter(listener: TorrentContentListener) :
    DataBindingAdapter<TorrentFileItem, TorrentContentListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<TorrentFileItem>() {
        override fun areItemsTheSame(oldItem: TorrentFileItem, newItem: TorrentFileItem): Boolean {
            return oldItem.id == newItem.id &&
                oldItem.name == newItem.name &&
                oldItem.absolutePath == newItem.absolutePath
        }

        override fun areContentsTheSame(
            oldItem: TorrentFileItem,
            newItem: TorrentFileItem,
        ): Boolean {
            return oldItem.selected == newItem.selected
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item: TorrentFileItem = this.getItem(position)
        return if (item.id == TYPE_FOLDER) R.layout.item_list_torrent_selection_directory
        else R.layout.item_list_torrent_selection_file
    }
}
