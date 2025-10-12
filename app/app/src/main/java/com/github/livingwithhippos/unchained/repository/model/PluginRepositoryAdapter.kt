package com.github.livingwithhippos.unchained.repository.model

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.ItemPluginRepositoryPluginBinding
import com.github.livingwithhippos.unchained.databinding.ItemPluginRepositoryRepoBinding
import com.github.livingwithhippos.unchained.utilities.extension.setDrawableByPluginStatus
import kotlinx.parcelize.Parcelize

class PluginRepositoryAdapter(private val listener: PluginListener) :
    ListAdapter<RepositoryListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<RepositoryListItem>() {
        override fun areItemsTheSame(
            oldItem: RepositoryListItem,
            newItem: RepositoryListItem,
        ): Boolean {
            // trick for smart casting
            if (
                oldItem is RepositoryListItem.Repository && newItem is RepositoryListItem.Repository
            ) {
                return oldItem.link.equals(newItem.link, ignoreCase = true)
            } else if (
                oldItem is RepositoryListItem.Plugin && newItem is RepositoryListItem.Plugin
            ) {
                return oldItem.repository.equals(newItem.repository, ignoreCase = true) &&
                    oldItem.link.equals(newItem.link, ignoreCase = true)
            } else return false
        }

        override fun areContentsTheSame(
            oldItem: RepositoryListItem,
            newItem: RepositoryListItem,
        ): Boolean {
            // trick for smart casting
            if (
                oldItem is RepositoryListItem.Repository && newItem is RepositoryListItem.Repository
            ) {
                return oldItem.version == newItem.version &&
                    oldItem.name == newItem.name &&
                    oldItem.description == newItem.description
            } else if (
                oldItem is RepositoryListItem.Plugin && newItem is RepositoryListItem.Plugin
            ) {
                return oldItem.name == newItem.name &&
                    oldItem.version == newItem.version &&
                    oldItem.status == newItem.status
            }
            return false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            R.layout.item_plugin_repository_repo -> {
                val binding =
                    ItemPluginRepositoryRepoBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                RepositoryViewHolder(binding, listener)
            }

            R.layout.item_plugin_repository_plugin -> {
                val binding =
                    ItemPluginRepositoryPluginBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    )
                PluginViewHolder(binding, listener)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PluginViewHolder -> (holder).bindCell(item as RepositoryListItem.Plugin)
            is RepositoryViewHolder -> holder.bindCell(item as RepositoryListItem.Repository)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (this.getItem(position)) {
            is RepositoryListItem.Plugin -> R.layout.item_plugin_repository_plugin
            is RepositoryListItem.Repository -> R.layout.item_plugin_repository_repo
        }
    }
}

class PluginViewHolder(
    private val binding: ItemPluginRepositoryPluginBinding,
    private val listener: PluginListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: RepositoryListItem.Plugin) {

        setDrawableByPluginStatus(binding.ivStatus, item.status)
        binding.tvName.text = item.name

        if (item.status == PluginStatus.isNew) binding.bDownload.visibility = View.VISIBLE
        else binding.bDownload.visibility = View.GONE
        binding.bDownload.setOnClickListener { listener.onPluginDownloadClick(item) }

        if (item.status == PluginStatus.hasUpdate) binding.bUpdate.visibility = View.VISIBLE
        else binding.bUpdate.visibility = View.GONE
        binding.bUpdate.setOnClickListener { listener.onPluginDownloadClick(item) }

        if (
            item.status == PluginStatus.updated ||
                item.status == PluginStatus.hasUpdate ||
                item.status == PluginStatus.hasIncompatibleUpdate
        )
            binding.bDelete.visibility = View.VISIBLE
        else binding.bDelete.visibility = View.GONE
        binding.bDelete.setOnClickListener { listener.onPluginRemoveClick(item) }

        binding.tvVersion.text = item.version.toString()
        binding.tvStatus.text = item.statusTranslation
    }
}

class RepositoryViewHolder(
    private val binding: ItemPluginRepositoryRepoBinding,
    private val listener: PluginListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bindCell(item: RepositoryListItem.Repository) {
        binding.tvName.text = item.name
        binding.tvVersion.text = item.version.toString()
        binding.tvAuthor.text = item.author
        binding.bManage.setOnClickListener { listener.onRepositoryClick(item) }
    }
}

sealed class RepositoryListItem {
    @Parcelize
    data class Repository(
        // primary id
        val link: String,
        val name: String,
        val version: Double,
        val description: String,
        val author: String,
    ) : RepositoryListItem(), Parcelable

    data class Plugin(
        // repo link used as foreign key to differentiate plugins
        val repository: String,
        val name: String,
        val version: Float,
        val link: String,
        val author: String?,
        // see PluginStatus
        var status: String,
        var statusTranslation: String,
    ) : RepositoryListItem()
}

object PluginStatus {
    // installed and ready
    const val updated = "updated"

    // installed with compatible update available
    const val hasUpdate = "has_update"

    // installed with no compatible update available
    const val hasIncompatibleUpdate = "has_incompatible_update"

    // no versions in repository, split between installed and not installed?
    const val unknown = "unknown"

    // new, installable
    const val isNew = "new"

    // new. not installable
    const val incompatible = "incompatible"
}

interface PluginListener {
    fun onPluginDownloadClick(plugin: RepositoryListItem.Plugin)

    fun onPluginRemoveClick(plugin: RepositoryListItem.Plugin)

    fun onRepositoryClick(repository: RepositoryListItem.Repository)
}
