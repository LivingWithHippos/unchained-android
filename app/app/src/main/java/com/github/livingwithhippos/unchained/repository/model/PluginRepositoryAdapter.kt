package com.github.livingwithhippos.unchained.repository.model

import androidx.recyclerview.widget.DiffUtil
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.DataBindingAdapter

sealed class RepositoryListItem {
    data class Repository(
        // primary id
        val link: String,
        val name: String,
        val version: Double,
        val description: String,
        val author: String
    ) : RepositoryListItem()

    data class Plugin(
        // repo link used as foreign key to differentiate plugins
        val repository: String,
        val name: String,
        val version: Double,
        val link: String,
        // see PluginStatus
        var status: String,
        var statusTranslation: String
    ) : RepositoryListItem()
}

object PluginStatus {
    // installed and ready
    const val updated = "updated"

    // installed with update available
    const val hasUpdate = "has_update"

    // new, installable
    const val new = "new"

    // new. not installable
    const val incompatible = "incompatible"
}

interface PluginListener {
    fun onPluginDownloadClick(plugin: RepositoryListItem.Plugin)
    fun onPluginRemoveClick(plugin: RepositoryListItem.Plugin)
    fun onRepositoryClick(repository: RepositoryListItem.Repository)
}

class PluginRepositoryAdapter(listener: PluginListener) :
    DataBindingAdapter<RepositoryListItem, PluginListener>(DiffCallback(), listener) {

    class DiffCallback : DiffUtil.ItemCallback<RepositoryListItem>() {
        override fun areItemsTheSame(
            oldItem: RepositoryListItem,
            newItem: RepositoryListItem
        ): Boolean {
            // trick for smart casting
            if (oldItem is RepositoryListItem.Repository && newItem is RepositoryListItem.Repository) {
                return oldItem.link.equals(newItem.link, ignoreCase = true)
            } else if (oldItem is RepositoryListItem.Plugin && newItem is RepositoryListItem.Plugin) {
                return oldItem.repository.equals(
                    newItem.repository,
                    ignoreCase = true
                ) && oldItem.link.equals(newItem.link, ignoreCase = true)
            } else
                return false
        }

        override fun areContentsTheSame(
            oldItem: RepositoryListItem,
            newItem: RepositoryListItem
        ): Boolean {
            // trick for smart casting
            if (oldItem is RepositoryListItem.Repository && newItem is RepositoryListItem.Repository) {
                return oldItem.version == newItem.version && oldItem.name == newItem.name && oldItem.description == newItem.description
            } else if (oldItem is RepositoryListItem.Plugin && newItem is RepositoryListItem.Plugin) {
                return oldItem.name == newItem.name && oldItem.version == newItem.version && oldItem.status == newItem.status
            }
            return false
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (this.getItem(position)) {
            is RepositoryListItem.Plugin -> R.layout.item_plugin_repository_plugin
            is RepositoryListItem.Repository -> R.layout.item_plugin_repository_repo
        }
    }
}