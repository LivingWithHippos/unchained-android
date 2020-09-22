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
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
import com.github.livingwithhippos.unchained.lists.model.DownloadItem
import com.github.livingwithhippos.unchained.lists.model.TorrentListListener
import com.github.livingwithhippos.unchained.lists.model.TorrentListPagingAdapter
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.verticalScrollToPosition
import com.github.livingwithhippos.unchained.utilities.showToast
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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

        // observers created to be easily added and removed. Pass the retrieved list to the adapter and removes the loading icon from the swipe layout
        val downloadObserver = Observer<PagingData<DownloadItem>> {
            lifecycleScope.launch {
                downloadAdapter.submitData(it)
                if (listBinding.srLayout.isRefreshing) {
                    listBinding.srLayout.isRefreshing = false
                    // this delay is needed to activate the scrolling, otherwise it won't work. Even 150L was not enough.
                    delay(200)
                    listBinding.rvDownloadList.layoutManager?.verticalScrollToPosition(requireContext())
                    //todo: add ripple animation on item at position 0 if possible, see [runRippleAnimation]
                }

            }
        }

        val torrentObserver = Observer<PagingData<TorrentItem>> {
            lifecycleScope.launch {
                torrentAdapter.submitData(it)
                if (listBinding.srLayout.isRefreshing) {
                    listBinding.srLayout.isRefreshing = false
                    // this delay is needed to activate the scrolling, otherwise it won't work. Even 150L was not enough.
                    delay(200)
                    listBinding.rvTorrentList.layoutManager?.verticalScrollToPosition(requireContext())
                }
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(viewLifecycleOwner, Observer {
            if (it.peekContent() == MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                // register observers if not already registered
                if (!viewModel.downloadsLiveData.hasActiveObservers())
                    viewModel.downloadsLiveData.observe(viewLifecycleOwner, downloadObserver)
                if (!viewModel.torrentsLiveData.hasActiveObservers())
                    viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
            } else {
                // remove observers if present
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
                                viewModel.downloadsLiveData.observe(
                                        viewLifecycleOwner,
                                        downloadObserver
                                )
                        }
                        TAB_TORRENTS -> {
                            if (!viewModel.torrentsLiveData.hasActiveObservers())
                                viewModel.torrentsLiveData.observe(
                                        viewLifecycleOwner,
                                        torrentObserver
                                )
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

        viewModel.downloadItemLiveData.observe(viewLifecycleOwner, {
            // dwecidi come gestire la trasformazione di un torrent in download, magari usa un dialog? Il refresh dovrebbe riportarmi in cima in csao
            it.getContentIfNotHandled()?.let {
                // switch to download tab
                listBinding.tabs.getTabAt(TAB_DOWNLOADS)?.select()
                // simulate list refresh
                listBinding.srLayout.isRefreshing = true
                // refresh items, when returned they'll stop the animation
                downloadAdapter.refresh()
            }
        })

        return listBinding.root
    }

    override fun onClick(item: DownloadItem) {
        val action = ListsTabFragmentDirections.actionListsTabToDownloadDetails(item)
        findNavController().navigate(action)
    }

    override fun onClick(item: TorrentItem) {
        if (item.status=="downloaded")
            viewModel.downloadTorrent(item)
        else
            showToast(R.string.torrent_not_downloaded)
    }

    companion object {
        private const val TAB_DOWNLOADS = 0
        private const val TAB_TORRENTS = 1
    }

}