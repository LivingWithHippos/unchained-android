package com.github.livingwithhippos.unchained.repository.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentRepositoryBinding
import com.github.livingwithhippos.unchained.repository.viewmodel.PluginRepositoryEvent
import com.github.livingwithhippos.unchained.repository.viewmodel.RepositoryViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RepositoryFragment : UnchainedFragment() {

    private val viewModel: RepositoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRepositoryBinding.inflate(inflater, container, false)

        viewModel.pluginsRepositoryLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    PluginRepositoryEvent.Updated -> {
                        // load data from the database
                    }
                }
            }
        )

        return binding.root
    }
}