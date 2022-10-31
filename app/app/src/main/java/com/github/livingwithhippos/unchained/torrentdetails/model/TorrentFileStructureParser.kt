package com.github.livingwithhippos.unchained.torrentdetails.model

import com.github.livingwithhippos.unchained.data.model.TorrentItem
import timber.log.Timber


class TreeNode<T, U>(val key: T, var value: U) {
    var parent: TreeNode<T, U>? = null

    private val children: MutableMap<T, TreeNode<T, U>> = mutableMapOf()

    /**
     * Add child if its key its new, otherwise skip it.
     * Return the child with this key
     *
     * @param node
     * @return
     */
    fun addChild(node: TreeNode<T, U>): TreeNode<T, U> {
        if (children[node.key] == null) {
            node.parent = this
            children[node.key] = node
        }

        return children.getValue(node.key)
    }

    fun getChildByKey(key: T): TreeNode<T, U>? {
        return children[key]
    }

    fun getChildByValue(value: U): TreeNode<T, U>? {
        children.values.forEach {
            if (it.value == value)
                return it
        }
        return null
    }

    fun getChildFolders(): List<TreeNode<T, U>> {
        return children.values.filter { it.value == FOLDER }
    }

    fun getAllChildrenFilesID(): List<U> {
        val idList = mutableListOf<U>()
        if (this.value != FOLDER)
            idList.add(this.value)

        children.values.forEach {
            idList.addAll(it.getAllChildrenFilesID())
        }

        return idList
    }

    fun depth(): Int {
        var depth = 0
        var tempParent = parent

        while (tempParent != null) {
            depth++
            tempParent = tempParent.parent
        }
        return depth
    }

    fun nodeCount(): Int {
        if (children.isEmpty())
            return 0
        return children.size + children.values.sumOf { it.nodeCount() }
    }

    override fun toString(): String {
        val buffer = StringBuffer(if (value == FOLDER) "$key: " else "  $value. $key")
        if (children.isNotEmpty()) {
            buffer.appendLine("{")
            children.values.forEach {
                buffer.appendLine("    $it")
            }
            buffer.append("}")
        }
        return buffer.toString()
    }

    companion object {
        const val FOLDER = -1
    }
}

fun getFilesTree(item: TorrentItem): TreeNode<String, Int> {
    val rootFolder = TreeNode("/", TreeNode.FOLDER)

    try {
        if (item.files != null) {
            for (file in item.files) {
                val paths = file.path.split("/").drop(1)

                var currentNode = rootFolder
                paths.forEachIndexed { index, value ->
                    if (index == paths.lastIndex) {
                        // this is a file
                        currentNode.addChild(TreeNode(value, file.id))
                    } else {
                        // this is a folder
                        currentNode = currentNode.addChild(TreeNode(value, TreeNode.FOLDER))
                    }
                }
            }
        }
    } catch (e: Exception) {
        Timber.e("getFilesTree crashed: ${e.message}")
    }

    return rootFolder
}