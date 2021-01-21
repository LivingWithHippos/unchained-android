package com.github.livingwithhippos.unchained.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.extension.verticalScrollToPosition
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of showing a list of both [DownloadItem] and [TorrentItem] switched with a tab layout.
 */
@AndroidEntryPoint
class ListsTabFragment : UnchainedFragment(), DownloadListListener, TorrentListListener {

    enum class ListState {
        UPDATE_TORRENT, UPDATE_DOWNLOAD, READY
    }

    //todo: rename viewModel/fragment to ListTab or DownloadLists
    private val viewModel: DownloadListViewModel by viewModels()

    // used to simulate a debounce effect while typing on the search bar
    var queryJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val listBinding = FragmentTabListsBinding.inflate(inflater, container, false)

        val downloadAdapter = DownloadListPagingAdapter(this)
        val torrentAdapter = TorrentListPagingAdapter(this)

        listBinding.rvDownloadList.adapter = downloadAdapter
        listBinding.rvTorrentList.adapter = torrentAdapter

        listBinding.srLayout.setOnRefreshListener {
            when (listBinding.tabs.selectedTabPosition) {
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
                // stop the refresh animation if playing
                if (listBinding.srLayout.isRefreshing) {
                    listBinding.srLayout.isRefreshing = false
                    // scroll to top if we were refreshing
                    delayedListScrolling(listBinding.rvDownloadList)
                }
                // delay for notifying the list that the items have changed, otherwise stuff like the status and the progress are not updated until you scroll away and back there
                delay(300)
                downloadAdapter.notifyDataSetChanged()
            }
        }

        val torrentObserver = Observer<PagingData<TorrentItem>> {
            lifecycleScope.launch {
                torrentAdapter.submitData(it)
                if (listBinding.srLayout.isRefreshing) {
                    listBinding.srLayout.isRefreshing = false
                    delayedListScrolling(listBinding.rvTorrentList)
                }
                delay(300)
                torrentAdapter.notifyDataSetChanged()
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(viewLifecycleOwner, {
            when (it.peekContent()) {
                AuthenticationState.AUTHENTICATED, AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                    // register observers if not already registered
                    if (!viewModel.downloadsLiveData.hasActiveObservers())
                        viewModel.downloadsLiveData.observe(viewLifecycleOwner, downloadObserver)
                    if (!viewModel.torrentsLiveData.hasActiveObservers())
                        viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
                }
                else -> {
                    // remove observers if present
                    viewModel.downloadsLiveData.removeObserver(downloadObserver)
                    viewModel.torrentsLiveData.removeObserver(torrentObserver)
                }
            }
        })

        listBinding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    listBinding.selectedTab = it.position

                    when (it.position) {
                        TAB_DOWNLOADS -> {
                            viewModel.setSelectedTab(TAB_DOWNLOADS)
                            if (!viewModel.downloadsLiveData.hasActiveObservers())
                                viewModel.downloadsLiveData.observe(
                                    viewLifecycleOwner,
                                    downloadObserver
                                )
                        }
                        TAB_TORRENTS -> {
                            viewModel.setSelectedTab(TAB_TORRENTS)
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

        viewModel.downloadItemLiveData.observe(viewLifecycleOwner, EventObserver { links ->
                if (!links.isNullOrEmpty()) {
                    // switch to download tab
                    listBinding.tabs.getTabAt(TAB_DOWNLOADS)?.select()
                    // simulate list refresh
                    listBinding.srLayout.isRefreshing = true
                    // refresh items, when returned they'll stop the animation
                    downloadAdapter.refresh()
                }
        })


        viewModel.deletedTorrentLiveData.observe(viewLifecycleOwner, EventObserver {
            context?.showToast(R.string.torrent_deleted)
            torrentAdapter.refresh()
        })

        activityViewModel.listStateLiveData.observe(viewLifecycleOwner, EventObserver{
            when (it) {
                ListState.UPDATE_DOWNLOAD -> {
                    lifecycleScope.launch{
                        delay(300L)
                        downloadAdapter.refresh()
                        delayedListScrolling(listBinding.rvDownloadList)
                    }
                }
                ListState.UPDATE_TORRENT -> {
                    lifecycleScope.launch {
                        delay(300L)
                        torrentAdapter.refresh()
                        delayedListScrolling(listBinding.rvTorrentList)
                    }
                }
                ListState.READY -> {
                }
            }
        })


        setFragmentResultListener("downloadActionKey") { _, bundle ->
            bundle.getString("deletedDownloadKey")?.let {
                viewModel.deleteDownload(it)
            }
            bundle.getParcelable<DownloadItem>("openedDownloadItem")?.let {
                onClick(it)
            }
        }

        setFragmentResultListener("torrentActionKey") { _, bundle ->
            bundle.getString("deletedTorrentKey")?.let {
                viewModel.deleteTorrent(it)
            }
            bundle.getString("openedTorrentItem")?.let {
                val authState = activityViewModel.authenticationState.value?.peekContent()
                if (authState == AuthenticationState.AUTHENTICATED) {
                    val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(it)
                    findNavController().navigate(action)
                } else
                    context?.showToast(R.string.premium_needed)
            }
            bundle.getParcelable<TorrentItem>("downloadedTorrentItem")?.let {
                onClick(it)
            }
        }

        viewModel.deletedDownloadLiveData.observe(viewLifecycleOwner, EventObserver {
            context?.showToast(R.string.download_removed)
            downloadAdapter.refresh()
        })

        viewModel.errorsLiveData.observe(viewLifecycleOwner, EventObserver {
            for (error in it) {
                when (error) {
                    is APIError -> {
                        context?.let { c ->
                            c.showToast(c.getApiErrorMessage(error.errorCode))
                        }
                        when(error.errorCode) {
                            8 -> {
                                //bad token, try refreshing it
                                activityViewModel.setBadToken()
                                context?.showToast(R.string.refreshing_token)
                            }
                        }
                    }
                    is EmptyBodyError -> {
                    }
                    is NetworkError -> {
                        context?.showToast(R.string.network_error)
                    }
                    is ApiConversionError -> {
                        context?.showToast(R.string.parsing_error)
                    }
                }
            }
        })

        // without this the lists won't get initialized
        viewModel.setListFilter("")

        listBinding.tabs.getTabAt(viewModel.getSelectedTab())?.select()

        return listBinding.root
    }

    // menu-related functions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lists_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView
        // listens to the user typing in the search bar

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // since there is a 500ms delay on new queries, this will help if the user types something and press search in less than half sec. May be unnecessary. The value is checked anyway in the viewmodel to avoid reloading with the same query as the last one.
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setListFilter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // simulate debounce
                queryJob?.cancel()

                queryJob = lifecycleScope.launch {
                    delay(500)
                    viewModel.setListFilter(newText)
                }
                return true
            }

        })

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(item: DownloadItem) {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED) {
            val action = ListsTabFragmentDirections.actionListsTabToDownloadDetails(item)
            findNavController().navigate(action)
        } else
            context?.showToast(R.string.premium_needed)
    }

    override fun onLongClick(item: DownloadItem) {
        val dialog = DownloadContextualDialogFragment(item)
        dialog.show(parentFragmentManager, "DownloadContextualDialogFragment")
    }

    override fun onClick(item: TorrentItem) {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED) {
            when (item.status) {
                "downloaded" -> {
                    // if the item has many links to download, show a toast
                    if (item.links.size > 2)
                        context?.showToast(R.string.downloading_torrent)
                    viewModel.downloadTorrent(item)
                }
                // open the torrent details fragment
                else -> {
                    val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(item.id)
                    findNavController().navigate(action)
                }
            }
        } else
            context?.showToast(R.string.premium_needed)
    }

    override fun onLongClick(item: TorrentItem) {
        val dialog = TorrentContextualDialogFragment(item)
        dialog.show(parentFragmentManager, "TorrentContextualDialogFragment")
    }

    private fun delayedListScrolling(recyclerView: RecyclerView, delay: Long = 300) {
        recyclerView.layoutManager?.let{
            lifecycleScope.launch {
                // this delay is needed to activate the scrolling, otherwise it won't work. It probably depends on the device.
                delay(delay)
                it.verticalScrollToPosition(requireContext())
            }
        }
    }

    companion object {
        const val TAB_DOWNLOADS = 0
        const val TAB_TORRENTS = 1
    }

}