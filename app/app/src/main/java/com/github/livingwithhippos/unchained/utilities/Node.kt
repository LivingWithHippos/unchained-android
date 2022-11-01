package com.github.livingwithhippos.unchained.utilities

/**
 * Credits to [sources](https://github.com/montwell/KotlinTreeTraversals) and [article](https://medium.com/swlh/tree-traversals-in-kotlin-7ff1940af7fa)
 *
 * @param T
 * @property value
 * @property children
 * @constructor Create empty Node
 */
class Node<T>(
    var value: T,
    var children: MutableList<Node<T>> = mutableListOf()
) {

    companion object {

        fun <T> traverseDepthFirst(
            rootNode: Node<T>,
            action: (value: T) -> Unit
        ) {
            val stack = ArrayDeque<Node<T>>()
            stack.addFirst(rootNode)

            while (stack.isNotEmpty()) {
                val currentNode = stack.removeFirst()

                action.invoke(currentNode.value)

                for (index in currentNode.children.size - 1 downTo 0) {
                    stack.addFirst(currentNode.children[index])
                }
            }
        }

        fun <T> traverseNodeDepthFirst(
            rootNode: Node<T>,
            action: (value: Node<T>) -> Unit
        ) {
            val stack = ArrayDeque<Node<T>>()
            stack.addFirst(rootNode)

            while (stack.isNotEmpty()) {
                val currentNode = stack.removeFirst()

                action.invoke(currentNode)

                for (index in currentNode.children.size - 1 downTo 0) {
                    stack.addFirst(currentNode.children[index])
                }
            }
        }

        fun <T> traverseBreadthFirst(
            rootNode: Node<T>,
            action: (value: T) -> Unit
        ) {
            val queue = ArrayDeque<Node<T>>()
            queue.addFirst(rootNode)

            while (queue.isNotEmpty()) {
                val currentNode = queue.removeLast()

                action.invoke(currentNode.value)

                for (childNode in currentNode.children) {
                    queue.addFirst(childNode)
                }
            }
        }

        fun <T> traverseNodeBreadthFirst(
            rootNode: Node<T>,
            action: (value: Node<T>) -> Unit
        ) {
            val queue = ArrayDeque<Node<T>>()
            queue.addFirst(rootNode)

            while (queue.isNotEmpty()) {
                val currentNode = queue.removeLast()

                action.invoke(currentNode)

                for (childNode in currentNode.children) {
                    queue.addFirst(childNode)
                }
            }
        }
    }
}