package com.github.livingwithhippos.unchained.lists.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
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
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.DOWNLOADS_DELETED_ALL
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.DOWNLOAD_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.DOWNLOAD_NOT_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.TORRENTS_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.TORRENTS_DELETED_ALL
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.TORRENT_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.DownloadListViewModel.Companion.TORRENT_NOT_DELETED
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.extension.verticalScrollToPosition
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of showing a list of both [DownloadItem] and [TorrentItem] switched with a tab layout.
 */
@AndroidEntryPoint
class ListsTabFragment : UnchainedFragment(), DownloadListListener, TorrentListListener {

    enum class ListState {
        UPDATE_TORRENT, UPDATE_DOWNLOAD, READY
    }

    // todo: rename viewModel/fragment to ListTab or DownloadLists
    private val viewModel: DownloadListViewModel by viewModels()

    // used to simulate a debounce effect while typing on the search bar
    var queryJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentTabListsBinding =
            FragmentTabListsBinding.inflate(inflater, container, false)

        binding.selectedItems = 0

        val downloadAdapter = DownloadListPagingAdapter(this)
        val torrentAdapter = TorrentListPagingAdapter(this)

        binding.rvDownloadList.adapter = downloadAdapter
        binding.rvTorrentList.adapter = torrentAdapter

        val tracker: SelectionTracker<TorrentItem> = SelectionTracker.Builder(
            "torrentListSelection",
            binding.rvTorrentList,
            TorrentKeyProvider(torrentAdapter),
            DataBindingDetailsLookup(binding.rvTorrentList),
            StorageStrategy.createParcelableStorage(TorrentItem::class.java)
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        torrentAdapter.tracker = tracker

        tracker.addObserver(
            object : SelectionTracker.SelectionObserver<TorrentItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedItems = tracker.selection.size()
                }
            })

        binding.listener = object: SelectedItemsButtonsListener {
            override fun deleteSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {

                } else {
                    viewModel.deleteTorrents(tracker.selection.toList())
                }
            }

            override fun shareSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {

                } else {
                }
            }

            override fun downloadSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {

                } else {
                    viewModel.downloadItems(tracker.selection.toList())
                }
            }
        }


        binding.srLayout.setOnRefreshListener {
            when (binding.tabs.selectedTabPosition) {
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
                // fixme: lots of crashes here because binding is null. Check if the commit removing "val binding get() = _binding!!" worked
                if (binding.srLayout.isRefreshing) {
                    binding.srLayout.isRefreshing = false
                    // scroll to top if we were refreshing
                    delayedListScrolling(binding.rvDownloadList)
                }
                // delay for notifying the list that the items have changed, otherwise stuff like the status and the progress are not updated until you scroll away and back there
                delay(300)
                downloadAdapter.notifyDataSetChanged()
            }
        }

        val torrentObserver = Observer<PagingData<TorrentItem>> {
            lifecycleScope.launch {
                torrentAdapter.submitData(it)
                if (binding.srLayout.isRefreshing) {
                    binding.srLayout.isRefreshing = false
                    delayedListScrolling(binding.rvTorrentList)
                }
                delay(300)
                torrentAdapter.notifyDataSetChanged()
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(
            viewLifecycleOwner,
            {
                when (it.peekContent()) {
                    AuthenticationState.AUTHENTICATED, AuthenticationState.AUTHENTICATED_NO_PREMIUM -> {
                        // register observers if not already registered
                        if (!viewModel.downloadsLiveData.hasActiveObservers())
                            viewModel.downloadsLiveData.observe(
                                viewLifecycleOwner,
                                downloadObserver
                            )
                        if (!viewModel.torrentsLiveData.hasActiveObservers())
                            viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
                    }
                    else -> {
                        // remove observers if present
                        viewModel.downloadsLiveData.removeObserver(downloadObserver)
                        viewModel.torrentsLiveData.removeObserver(torrentObserver)
                    }
                }
            }
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {

                    when (it.position) {
                        TAB_DOWNLOADS -> {
                            viewModel.setSelectedTab(TAB_DOWNLOADS)
                            binding.rvTorrentList.visibility = View.GONE
                            binding.rvDownloadList.visibility = View.VISIBLE
                            if (!viewModel.downloadsLiveData.hasActiveObservers())
                                viewModel.downloadsLiveData.observe(
                                    viewLifecycleOwner,
                                    downloadObserver
                                )
                        }
                        TAB_TORRENTS -> {
                            viewModel.setSelectedTab(TAB_TORRENTS)
                            binding.rvTorrentList.visibility = View.VISIBLE
                            binding.rvDownloadList.visibility = View.GONE
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

        viewModel.downloadItemLiveData.observe(
            viewLifecycleOwner,
            EventObserver { links ->
                if (!links.isNullOrEmpty()) {
                    // switch to download tab
                    binding.tabs.getTabAt(TAB_DOWNLOADS)?.select()
                    // simulate list refresh
                    binding.srLayout.isRefreshing = true
                    // refresh items, when returned they'll stop the animation
                    downloadAdapter.refresh()
                }
            }
        )

        viewModel.deletedTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    TORRENT_NOT_DELETED -> {
                    }
                    TORRENT_DELETED -> {
                        context?.showToast(R.string.torrent_removed)
                        torrentAdapter.refresh()
                    }
                    TORRENTS_DELETED_ALL -> {
                        context?.showToast(R.string.torrents_removed)
                        lifecycleScope.launch {
                            // if we don't refresh the cached copy of the last result will be restored on the first list redraw
                            torrentAdapter.refresh()
                            torrentAdapter.submitData(PagingData.empty())
                        }
                    }
                    TORRENTS_DELETED -> {
                        context?.showToast(R.string.torrents_removed)
                        torrentAdapter.refresh()
                    }
                    0 -> {
                        context?.showToast(R.string.removing_torrents)
                    }
                    else -> {
                        torrentAdapter.refresh()
                    }
                }
            }
        )

        activityViewModel.listStateLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    ListState.UPDATE_DOWNLOAD -> {
                        lifecycleScope.launch {
                            delay(300L)
                            downloadAdapter.refresh()
                            delayedListScrolling(binding.rvDownloadList)
                        }
                    }
                    ListState.UPDATE_TORRENT -> {
                        lifecycleScope.launch {
                            delay(300L)
                            torrentAdapter.refresh()
                            delayedListScrolling(binding.rvTorrentList)
                        }
                    }
                    ListState.READY -> {
                    }
                }
            }
        )

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

                    // workaround to avoid issues when the dialog still hasn't been popped from the navigation stack
                    val controller = findNavController()
                    var loop = 0
                    lifecycleScope.launch {
                        while (loop++ < 20 && controller.currentDestination?.id != R.id.list_tabs_dest) {
                            delay(100)
                        }
                        if (controller.currentDestination?.id == R.id.list_tabs_dest)
                            controller.navigate(action)
                    }

                } else
                    context?.showToast(R.string.premium_needed)
            }
            bundle.getParcelable<TorrentItem>("downloadedTorrentItem")?.let {
                onClick(it)
            }
        }

        viewModel.deletedDownloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    DOWNLOAD_NOT_DELETED -> {
                    }
                    DOWNLOAD_DELETED -> {
                        context?.showToast(R.string.download_removed)
                        downloadAdapter.refresh()
                    }
                    DOWNLOADS_DELETED_ALL -> {
                        context?.showToast(R.string.downloads_removed)
                        lifecycleScope.launch {
                            // if we don't refresh the cached copy of the last result will be restored on the first list redraw
                            downloadAdapter.refresh()
                            downloadAdapter.submitData(PagingData.empty())
                        }
                    }
                    0 -> {
                        context?.showToast(R.string.removing_downloads)
                    }
                    else -> {
                        downloadAdapter.refresh()
                    }
                }
            }
        )

        viewModel.errorsLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                for (error in it) {
                    when (error) {
                        is APIError -> {
                            context?.let { c ->
                                c.showToast(c.getApiErrorMessage(error.errorCode))
                            }
                            when (error.errorCode) {
                                8 -> {
                                    // bad token, try refreshing it
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
            }
        )

        // without this the lists won't get initialized
        viewModel.setListFilter("")

        binding.tabs.getTabAt(viewModel.getSelectedTab())?.select()

        return binding.root
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
            R.id.delete_all -> {
                showDeleteAllDialog(viewModel.getSelectedTab())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteAllDialog(selectedTab: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_all))
            .setMessage(
                if (selectedTab == TAB_DOWNLOADS)
                    getString(R.string.delete_all_downloads_message)
                else
                    getString(R.string.delete_all_torrents_message)
            )
            .setNegativeButton(getString(R.string.decline)) { _, _ ->
            }
            .setPositiveButton(getString(R.string.accept)) { _, _ ->
                if (selectedTab == TAB_DOWNLOADS)
                    viewModel.deleteAllDownloads()
                else
                    viewModel.deleteAllTorrents()
            }
            .show()
    }

    override fun onClick(item: DownloadItem) {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED) {
            val action = ListsTabFragmentDirections.actionListsTabToDownloadDetails(item)
            var loop = 0
            val controller = findNavController()
            lifecycleScope.launch {
                while (loop++ < 20 && controller.currentDestination?.id != R.id.list_tabs_dest) {
                    delay(100)
                }
                if (controller.currentDestination?.id == R.id.list_tabs_dest)
                    controller.navigate(action)
            }
        } else
            context?.showToast(R.string.premium_needed)
    }

    override fun onLongClick(item: DownloadItem) {
        val action = ListsTabFragmentDirections.actionListTabsDestToDownloadContextualDialogFragment(item)
        findNavController().navigate(action)
    }

    override fun onClick(item: TorrentItem) {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED) {
            when (item.status) {
                "downloaded" -> {
                    if (item.links.size > 1) {
                        val action =
                            ListsTabFragmentDirections.actionListTabsDestToFolderListFragment2(
                                folder = null,
                                torrent = item,
                                linkList = null
                            )
                        findNavController().navigate(action)
                    } else
                        viewModel.downloadTorrent(item)
                }
                // open the torrent details fragment
                else -> {
                    val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(item.id)
                    var loop = 0

                    val controller = findNavController()
                    lifecycleScope.launch {
                        while (loop++ < 20 && controller.currentDestination?.id != R.id.list_tabs_dest) {
                            delay(100)
                        }
                        if (controller.currentDestination?.id == R.id.list_tabs_dest)
                            controller.navigate(action)
                    }
                }
            }
        } else
            context?.showToast(R.string.premium_needed)
    }

    /*
    override fun onLongClick(item: TorrentItem) {
        val action =
            ListsTabFragmentDirections.actionListTabsDestToTorrentContextualDialogFragment(item)
        findNavController().navigate(action)
    }
     */

    private fun delayedListScrolling(recyclerView: RecyclerView, delay: Long = 300) {
        recyclerView.layoutManager?.let {
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

interface SelectedItemsButtonsListener {
    fun deleteSelectedItems()
    fun shareSelectedItems()
    fun downloadSelectedItems()
}
