package com.github.livingwithhippos.unchained.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
import com.github.livingwithhippos.unchained.lists.model.DownloadItem
import com.github.livingwithhippos.unchained.lists.model.TorrentListListener
import com.github.livingwithhippos.unchained.lists.model.TorrentListPagingAdapter
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListsTabFragment: UnchainedFragment(), DownloadListListener, TorrentListListener {

    private val viewModel: DownloadListViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val listBinding = FragmentTabListsBinding.inflate(inflater, container, false)

        val downloadAdapter = DownloadListPagingAdapter(this)
        val torrentAdapter = TorrentListPagingAdapter(this)

        listBinding.rvDownloadList.adapter = downloadAdapter
        listBinding.rvTorrentList.adapter = torrentAdapter

        //todo: add scroll to top when a new item is added
        listBinding.srLayout.setOnRefreshListener {
            when(listBinding.tabs.selectedTabPosition) {
                TAB_DOWNLOADS -> {
                    downloadAdapter.refresh()
                }
                TAB_TORRENTS -> {
                    torrentAdapter.refresh()
                }
            }
        }

        // observer created to easily add and remove it. Pass the retrieved download list to the adapter and removes the loading icon from the swipe layout
        val downloadObserver = Observer<PagingData<DownloadItem>> {
            lifecycleScope.launch {
                downloadAdapter.submitData(it)
                listBinding.srLayout.isRefreshing = false
            }
        }

        val torrentObserver = Observer<PagingData<TorrentItem>> {
            lifecycleScope.launch {
                torrentAdapter.submitData(it)
                listBinding.srLayout.isRefreshing = false
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(viewLifecycleOwner, Observer {
            if (it.peekContent() == MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                // register observer if not already registered
                if (!viewModel.downloadsLiveData.hasActiveObservers())
                    viewModel.downloadsLiveData.observe(viewLifecycleOwner, downloadObserver)
                if (!viewModel.torrentsLiveData.hasActiveObservers())
                    viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
            } else {
                // remove observer if present
                viewModel.downloadsLiveData.removeObserver(downloadObserver)
                viewModel.torrentsLiveData.removeObserver(torrentObserver)
            }
        })

        listBinding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    listBinding.selectedTab = it.position

                    when (it.position) {
                        TAB_DOWNLOADS -> {
                            if (!viewModel.downloadsLiveData.hasActiveObservers())
                                viewModel.downloadsLiveData.observe(viewLifecycleOwner, downloadObserver)
                        }
                        TAB_TORRENTS -> {
                            if (!viewModel.torrentsLiveData.hasActiveObservers())
                                viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
                        }
                    }
                }

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // either do nothing or refresh
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // remove observer
                when (tab?.position) {
                    TAB_DOWNLOADS -> {
                        viewModel.downloadsLiveData.removeObserver(downloadObserver)
                    }
                    TAB_TORRENTS -> {
                        viewModel.torrentsLiveData.removeObserver(torrentObserver)
                    }
                }
            }
        })

        listBinding.selectedTab = listBinding.tabs.selectedTabPosition

        return listBinding.root
    }

    override fun onClick(item: DownloadItem) {
        val action = ListsTabFragmentDirections.actionListsTabToDownloadDetails(item)
        findNavController().navigate(action)
    }

    override fun onClick(item: TorrentItem) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAB_DOWNLOADS = 0
        private const val TAB_TORRENTS = 1
    }

}