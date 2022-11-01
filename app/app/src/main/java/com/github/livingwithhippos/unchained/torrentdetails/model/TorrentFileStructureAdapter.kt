package com.github.livingwithhippos.unchained.torrentdetails.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter
import com.github.livingwithhippos.unchained.utilities.Node

data class TorrentFileItem(
    val id: Int,
    val absolutePath: String,
    val bytes: Long,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        return if (other is TorrentFileItem)
            this.id == other.id && this.absolutePath == other.absolutePath && this.name == other.name
        else
            super.equals(other)
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
    flattenFolders: Boolean = false
): Node<TorrentFileItem> {
    val rootFolder = Node(
        TorrentFileItem(
            TYPE_FOLDER,
            "",
            0,
            "/"
        )
    )

    if (item.files != null && item.files.isNotEmpty()) {
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
                                value
                            )
                        )
                    )
                } else {
                    // this is a folder
                    val node = currentNode.children.firstOrNull { it.value.name == value }
                    if (node == null) {
                        currentNode.children.add(
                            Node(
                                TorrentFileItem(
                                    TYPE_FOLDER,
                                    paths.subList(0, index + 1).joinToString("/"),
                                    0,
                                    value
                                )
                            )
                        )
                    }

                    currentNode = currentNode.children.first { it.value.name == value }
                }
            }
        }
    }

    if (flattenFolders) {
        val newRootFolder = Node(
            TorrentFileItem(
                TYPE_FOLDER,
                "",
                0,
                "/"
            )
        )
    }

    return rootFolder
}

interface TorrentContentListener {
    fun selectItem(item: TorrentFileItem)
}

class TorrentContentFilesAdapter(listener: TorrentContentListener) :
    DataBindingAdapter<TorrentFileItem, TorrentContentListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<TorrentFileItem>() {
        override fun areItemsTheSame(
            oldItem: TorrentFileItem,
            newItem: TorrentFileItem
        ): Boolean {
            return oldItem.absolutePath == newItem.absolutePath &&
                    oldItem.name == newItem.name &&
                    oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TorrentFileItem,
            newItem: TorrentFileItem
        ): Boolean {
            // content is not dynamic unless selected is added
            return true
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item: TorrentFileItem = this.getItem(position)
        return if (item.id == TYPE_FOLDER)
            R.layout.item_list_torrent_directory
        else
            R.layout.item_list_torrent_file
    }
}