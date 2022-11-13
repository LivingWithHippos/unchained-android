package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentPluginsBinding
import com.github.livingwithhippos.unchained.search.model.PluginItemAdapter
import com.github.livingwithhippos.unchained.search.model.PluginItemListener
import com.github.livingwithhippos.unchained.search.model.RemotePlugin
import com.github.livingwithhippos.unchained.search.viewmodel.PluginEvent
import com.github.livingwithhippos.unchained.search.viewmodel.PluginsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class PluginsFragment: UnchainedFragment(), PluginItemListener {

    private val viewModel: PluginsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPluginsBinding.inflate(inflater, container, false)

        val adapter = PluginItemAdapter(this)
        binding.rvPluginsList.adapter = adapter

        viewModel.pluginsLiveData.observe(viewLifecycleOwner) {
            when (val event = it.getContentIfNotHandled()) {
                is PluginEvent.Repository -> {
                    viewModel.checkWithLocalPlugins(event.plugins)
                }
                is PluginEvent.RepositoryError -> {
                    context?.showToast(R.string.network_error)
                }
                is PluginEvent.CheckedPlugins -> {
                    adapter.submitList(event.plugins)
                }
                null -> {

                }
            }
        }

        viewModel.checkRepository()

        return binding.root
    }

    override fun onClick(item: RemotePlugin) {
        Timber.d(item.toString())
    }
}