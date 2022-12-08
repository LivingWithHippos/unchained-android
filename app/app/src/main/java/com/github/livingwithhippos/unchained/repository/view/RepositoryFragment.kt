package com.github.livingwithhippos.unchained.repository.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.databinding.FragmentRepositoryBinding
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.repository.model.PluginListener
import com.github.livingwithhippos.unchained.repository.model.PluginRepositoryAdapter
import com.github.livingwithhippos.unchained.repository.model.PluginStatus
import com.github.livingwithhippos.unchained.repository.model.RepositoryListItem
import com.github.livingwithhippos.unchained.repository.viewmodel.PluginRepositoryEvent
import com.github.livingwithhippos.unchained.repository.viewmodel.RepositoryViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class RepositoryFragment : UnchainedFragment(), PluginListener {

    private val viewModel: RepositoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRepositoryBinding.inflate(inflater, container, false)

        val adapter = PluginRepositoryAdapter(this)
        binding.rvPluginsList.adapter = adapter

        viewModel.pluginsRepositoryLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    PluginRepositoryEvent.Updated -> {
                        // load data from the database
                        viewModel.retrieveDatabaseRepositories()
                    }
                    is PluginRepositoryEvent.FullData -> {
                        // data loaded from db, load into UI
                        updateList(adapter, it.data)
                    }
                    PluginRepositoryEvent.Installed -> {
                        context?.showToast(R.string.plugin_install_installed)
                    }
                }
            }
        )

        viewModel.checkCurrentRepositories()

        // observe the search bar for changes
        binding.tiSearch.addTextChangedListener {
            viewModel.filterList(it?.toString())
        }

        return binding.root
    }

    private fun updateList(
        adapter: PluginRepositoryAdapter,
        data: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>
    ) {
        val plugins = mutableListOf<RepositoryListItem>()
        for (repository in data){
            plugins.add(
                RepositoryListItem.Repository(
                    link = repository.key.link,
                    name = repository.key.name,
                    version = repository.key.version,
                    description = repository.key.description,
                    author = repository.key.author,
                )
            )
            for (plugin in repository.value) {
                var latestVersion: PluginVersion? = null
                // pick the latest compatible version
                for (version in plugin.value) {
                    if (isPluginSupported(version.engine))
                        if (latestVersion == null || version.version > latestVersion.version)
                            latestVersion = version
                }
                // check if any compatible version was found
                if (latestVersion == null) {
                    plugins.add(
                        RepositoryListItem.Plugin(
                            repository = repository.key.link,
                            name = plugin.key.name,
                            version = 0.0,
                            link = "",
                            status = PluginStatus.incompatible,
                            statusTranslation = getStatusTranslation(PluginStatus.incompatible)
                        )
                    )
                } else {
                    // todo: compare with currently installed plugins version
                    // todo: use 1 folder per repository to avoid issues
                    plugins.add(
                        RepositoryListItem.Plugin(
                            repository = repository.key.link,
                            name = plugin.key.name,
                            version = latestVersion.version,
                            link = latestVersion.link,
                            status = PluginStatus.new,
                            statusTranslation = getStatusTranslation(PluginStatus.new),
                        )
                    )
                }
            }
            adapter.submitList(plugins)
            adapter.notifyDataSetChanged()
        }
    }

    private fun getStatusTranslation(status: String): String {
        return when(status) {
            PluginStatus.updated -> getString(R.string.updated)
            PluginStatus.hasUpdate -> getString(R.string.new_update)
            PluginStatus.new -> getString(R.string.new_word)
            PluginStatus.incompatible -> getString(R.string.incompatible)
            else -> getString(R.string.unknown_status)
        }
    }

    private fun isPluginSupported(engineVersion: Double): Boolean {
        return engineVersion.toInt() == Parser.PLUGIN_ENGINE_VERSION.toInt() && Parser.PLUGIN_ENGINE_VERSION >= engineVersion
    }

    override fun onPluginDownloadClick(plugin: RepositoryListItem.Plugin) {
        Timber.d("Pressed plugin $plugin")
        when (plugin.status) {
            PluginStatus.hasUpdate -> {
                context?.showToast(R.string.downloading)
                viewModel.downloadPlugin(plugin.link, plugin.repository, requireContext())
            }
            PluginStatus.new -> {
                context?.showToast(R.string.downloading)
                viewModel.downloadPlugin(plugin.link, plugin.repository, requireContext())
            }
        }
    }

    override fun onPluginRemoveClick(plugin: RepositoryListItem.Plugin) {
        TODO("Not yet implemented")
    }

    override fun onRepositoryClick(repository: RepositoryListItem.Repository) {
        Timber.d("Pressed repository $repository")
    }
}