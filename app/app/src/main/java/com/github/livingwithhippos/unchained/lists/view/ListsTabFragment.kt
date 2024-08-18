package com.github.livingwithhippos.unchained.lists.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadsListBinding
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentsListBinding
import com.github.livingwithhippos.unchained.lists.viewmodel.ListEvent
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.DOWNLOADS_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.DOWNLOADS_DELETED_ALL
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.DOWNLOAD_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.DOWNLOAD_NOT_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.TORRENTS_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.TORRENTS_DELETED_ALL
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.TORRENT_DELETED
import com.github.livingwithhippos.unchained.lists.viewmodel.ListTabsViewModel.Companion.TORRENT_NOT_DELETED
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.DOWNLOADS_TAB
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.TORRENTS_TAB
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.getThemedDrawable
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass. It is capable of showing a list of both [DownloadItem] and
 * [TorrentItem] switched with a tab layout.
 */
@AndroidEntryPoint
class ListsTabFragment : UnchainedFragment() {

    private val viewModel: ListTabsViewModel by activityViewModels()

    // used to simulate a debounce effect while typing on the search bar
    var queryJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentTabListsBinding =
            FragmentTabListsBinding.inflate(inflater, container, false)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.lists_bar, menu)

                    val searchItem = menu.findItem(R.id.search)
                    val searchView = searchItem.actionView as SearchView
                    // listens to the user typing in the search bar

                    searchView.setOnQueryTextListener(
                        object : SearchView.OnQueryTextListener {
                            // since there is a 500ms delay on new queries, this will help if the
                            // user types
                            // something and press search in less than half sec. May be unnecessary.
                            // The value
                            // is checked anyway in the ViewModel to avoid reloading with the same
                            // query as
                            // the last one.
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                viewModel.setListFilter(query)
                                return true
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                // simulate debounce
                                queryJob?.cancel()

                                queryJob =
                                    lifecycleScope.launch {
                                        delay(500)
                                        if (isActive) viewModel.setListFilter(newText)
                                    }
                                return true
                            }
                        })
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.search -> {
                            true
                        }
                        R.id.delete_all_downloads -> {
                            showDeleteAllDialog(DOWNLOADS_TAB)
                            true
                        }
                        R.id.delete_all_torrents -> {
                            showDeleteAllDialog(TORRENTS_TAB)
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED)

        val listsAdapter = ListsAdapter(this)
        binding.listPager.adapter = listsAdapter

        binding.tabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    // to restore on resume?
                    if (tab != null) viewModel.setSelectedTab(tab.position)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    // either do nothing or remove/add observers
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    // either do nothing or refresh
                }
            })

        binding.fabNewDownload.setOnClickListener {
            val action = ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment()
            findNavController().navigate(action)
        }

        // an external link has been shared with the app
        activityViewModel.externalLinkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { uri ->
                val action =
                    ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment(
                        externalUri = uri)
                findNavController().navigate(action)
            })

        // a file has been downloaded, usually a torrent, and needs to be unrestricted
        activityViewModel.downloadedFileLiveData.observe(
            viewLifecycleOwner,
            EventObserver { fileID ->
                val uri = requireContext().getDownloadedFileUri(fileID)
                // no need to recheck the extension since it was checked on download
                // if (uri?.path?.endsWith(".torrent") == true)
                if (uri?.path != null) {
                    val action =
                        ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment(
                            externalUri = uri)
                    findNavController().navigate(action)
                }
            })

        // a notification has been clicked
        activityViewModel.notificationTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver { torrentID ->
                val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(torrentID)
                findNavController().navigate(action)
            })

        viewModel.eventLiveData.observe(
            viewLifecycleOwner,
            EventObserver { event ->
                when (event) {
                    is ListEvent.DownloadItemClick -> {
                        val action =
                            ListsTabFragmentDirections.actionListsTabToDownloadDetails(event.item)
                        var loop = 0
                        val controller = findNavController()
                        lifecycleScope.launch {
                            while (loop++ < 20 &&
                                controller.currentDestination?.id != R.id.list_tabs_dest) {
                                delay(100)
                            }
                            if (controller.currentDestination?.id == R.id.list_tabs_dest)
                                controller.navigate(action)
                        }
                    }
                    is ListEvent.TorrentItemClick -> {
                        when (event.item.status) {
                            "downloaded" -> {
                                if (event.item.links.size > 1) {
                                    val action =
                                        ListsTabFragmentDirections
                                            .actionListTabsDestToFolderListFragment2(
                                                folder = null,
                                                torrent = event.item,
                                                linkList = null)
                                    findNavController().navigate(action)
                                } else viewModel.unrestrictTorrent(event.item)
                            }
                            // open the torrent details fragment
                            else -> {
                                val action =
                                    ListsTabFragmentDirections.actionListsTabToTorrentDetails(
                                        event.item)
                                var loop = 0

                                val controller = findNavController()
                                lifecycleScope.launch {
                                    while (loop++ < 20 &&
                                        controller.currentDestination?.id != R.id.list_tabs_dest) {
                                        delay(100)
                                    }
                                    if (controller.currentDestination?.id == R.id.list_tabs_dest)
                                        controller.navigate(action)
                                }
                            }
                        }
                    }
                    is ListEvent.OpenTorrent -> {
                        val action =
                            ListsTabFragmentDirections.actionListsTabToTorrentDetails(event.item)

                        // workaround to avoid issues when the dialog still hasn't been popped from
                        // the
                        // navigation stack
                        val controller = findNavController()
                        var loop = 0
                        lifecycleScope.launch {
                            while (loop++ < 20 &&
                                controller.currentDestination?.id != R.id.list_tabs_dest) {
                                delay(100)
                            }
                            if (controller.currentDestination?.id == R.id.list_tabs_dest)
                                controller.navigate(action)
                        }
                    }
                    is ListEvent.SetTab -> {

                        if (event.tab == DOWNLOADS_TAB) {
                            if (binding.listPager.currentItem == TORRENTS_TAB)
                                binding.listPager.currentItem = DOWNLOADS_TAB
                        } else {
                            if (viewModel.getSelectedTab() == DOWNLOADS_TAB)
                                binding.listPager.currentItem = TORRENTS_TAB
                        }
                    }
                    ListEvent.NewDownload -> {
                        val action =
                            ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment()
                        findNavController().navigate(action)
                    }
                }
            })

        viewModel.errorsLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                for (error in it) {
                    when (error) {
                        is APIError -> {
                            context?.let { c -> c.showToast(c.getApiErrorMessage(error.errorCode)) }
                            when (error.errorCode) {
                                8 -> {
                                    // bad token, try refreshing it
                                    if (activityViewModel.getAuthenticationMachineState()
                                        is FSMAuthenticationState.AuthenticatedOpenToken)
                                        activityViewModel.transitionAuthenticationMachine(
                                            FSMAuthenticationEvent.OnExpiredOpenToken)
                                    context?.showToast(R.string.refreshing_token)
                                }
                            }
                        }
                        is EmptyBodyError -> {}
                        is NetworkError -> {
                            context?.showToast(R.string.network_error)
                        }
                        is ApiConversionError -> {
                            context?.showToast(R.string.parsing_error)
                        }
                    }
                }
            })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout: TabLayout = view.findViewById(R.id.tabs)
        val viewPager: ViewPager2 = view.findViewById(R.id.listPager)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                if (position == DOWNLOADS_TAB) {
                    tab.text = getString(R.string.downloads)
                    tab.icon = requireContext().getThemedDrawable(R.drawable.icon_cloud_done)
                } else {
                    tab.text = getString(R.string.torrents)
                    tab.icon = requireContext().getThemedDrawable(R.drawable.icon_torrent_logo)
                }
            }
            .attach()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun showDeleteAllDialog(selectedTab: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_all))
            .setMessage(
                if (selectedTab == DOWNLOADS_TAB) getString(R.string.delete_all_downloads_message)
                else getString(R.string.delete_all_torrents_message))
            .setNegativeButton(getString(R.string.decline)) { _, _ -> }
            .setPositiveButton(getString(R.string.accept)) { _, _ ->
                if (selectedTab == DOWNLOADS_TAB) viewModel.deleteAllDownloads()
                else viewModel.deleteAllTorrents()
            }
            .show()
    }
}

class ListsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == DOWNLOADS_TAB) {
            DownloadsListFragment()
        } else {
            TorrentsListFragment()
        }
    }
}

@AndroidEntryPoint
class DownloadsListFragment : UnchainedFragment(), DownloadListListener {

    private val viewModel: ListTabsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDownloadsListBinding.inflate(inflater, container, false)

        binding.selectedDownloads = 0

        val downloadAdapter = DownloadListPagingAdapter(this)
        binding.rvDownloadList.adapter = downloadAdapter

        // download list selection  tracker
        val downloadTracker: SelectionTracker<DownloadItem> =
            SelectionTracker.Builder(
                    "downloadListSelection",
                    binding.rvDownloadList,
                    DownloadKeyProvider(downloadAdapter),
                    DataBindingDetailsLookup(binding.rvDownloadList),
                    StorageStrategy.createParcelableStorage(DownloadItem::class.java))
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        downloadAdapter.tracker = downloadTracker

        downloadTracker.addObserver(
            object : SelectionTracker.SelectionObserver<DownloadItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedDownloads = downloadTracker.selection.size()
                }
            })

        // listener for selection buttons
        binding.listener =
            object : SelectedItemsButtonsListener {
                override fun deleteSelectedItems() {
                    if (downloadTracker.selection.toList().isNotEmpty())
                        viewModel.deleteDownloads(downloadTracker.selection.toList())
                    else context?.showToast(R.string.select_one_item)
                }

                override fun shareSelectedItems() {
                    if (downloadTracker.selection.toList().isNotEmpty()) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val shareLinks =
                            downloadTracker.selection.joinToString("\n") { it.download }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareLinks)
                        startActivity(
                            Intent.createChooser(shareIntent, getString(R.string.share_with)))
                    } else context?.showToast(R.string.select_one_item)
                }

                override fun downloadSelectedItems() {
                    val downloads: List<DownloadItem> = downloadTracker.selection.toList()
                    if (downloads.isNotEmpty()) {
                        if (downloads.size == 1) {
                            activityViewModel.enqueueDownload(
                                downloads.first().download, downloads.first().filename)
                        } else {
                            activityViewModel.enqueueDownloads(downloads)
                        }
                    } else context?.showToast(R.string.select_one_item)
                }

                override fun openSelectedDetails() {
                    // used only in torrent view
                }

                override fun openNewDownload() {
                    viewModel.postEventNotice(ListEvent.NewDownload)
                }

                override fun refreshList() {
                    if (!binding.srLayout.isRefreshing) {
                        binding.srLayout.isRefreshing = true
                        downloadAdapter.refresh()
                    }
                }
            }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                downloadTracker.setItemsSelected(downloadAdapter.snapshot().items, true)
            } else {
                downloadTracker.clearSelection()
            }
        }

        binding.srLayout.setOnRefreshListener { downloadAdapter.refresh() }

        // observers created to be easily added and removed. Pass the retrieved list to the adapter
        // and
        // removes the loading icon from the swipe layout
        val downloadObserver =
            Observer<PagingData<DownloadItem>> {
                lifecycleScope.launch {
                    downloadAdapter.submitData(it)
                    // stop the refresh animation if playing
                    if (binding.srLayout.isRefreshing) {
                        binding.srLayout.isRefreshing = false
                        // scroll to top if we were refreshing
                        lifecycleScope.launch {
                            binding.rvDownloadList.delayedScrolling(requireContext())
                        }
                    }
                    // delay for notifying the list that the items have changed, otherwise stuff
                    // like the
                    // status and the progress are not updated until you scroll away and back there
                    delay(300)
                    downloadAdapter.notifyDataSetChanged()
                }
            }

        // checks the authentication state. Needed to avoid automatic API calls before the
        // authentication process is finished
        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            when (it?.peekContent()) {
                FSMAuthenticationState.AuthenticatedOpenToken,
                FSMAuthenticationState.AuthenticatedPrivateToken -> {
                    // register observers if not already registered
                    if (!viewModel.downloadsLiveData.hasActiveObservers())
                        viewModel.downloadsLiveData.observe(viewLifecycleOwner, downloadObserver)
                }
                else -> {}
            }
        }

        viewModel.downloadItemLiveData.observe(
            viewLifecycleOwner,
            EventObserver { links ->
                // todo: if it gets emptied null/empty should be processed too
                if (links.isNotEmpty()) {
                    // simulate list refresh
                    binding.srLayout.isRefreshing = true
                    // refresh items, when returned they'll stop the animation
                    downloadAdapter.refresh()

                    viewModel.postEventNotice(ListEvent.SetTab(DOWNLOADS_TAB))
                }
            })

        activityViewModel.listStateLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    ListState.UpdateDownload -> {
                        lifecycleScope.launch {
                            delay(300L)
                            downloadAdapter.refresh()
                            lifecycleScope.launch {
                                binding.rvDownloadList.delayedScrolling(requireContext())
                            }
                        }
                    }
                    else -> {}
                }
            })

        viewModel.deletedDownloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    DOWNLOAD_NOT_DELETED -> {}
                    DOWNLOAD_DELETED -> {
                        context?.showToast(R.string.download_removed)
                        downloadAdapter.refresh()
                    }
                    DOWNLOADS_DELETED -> {
                        context?.showToast(R.string.downloads_removed)
                        downloadAdapter.refresh()
                    }
                    DOWNLOADS_DELETED_ALL -> {
                        context?.showToast(R.string.downloads_removed)
                        lifecycleScope.launch {
                            // if we don't refresh the cached copy of the last result will be
                            // restored on the
                            // first list redraw
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
            })

        // starts the Transformations.switchMap(queryLiveData) which otherwise won't trigger the
        // Paging
        // request
        viewModel.setListFilter("")

        return binding.root
    }

    override fun onClick(item: DownloadItem) {
        viewModel.postEventNotice(ListEvent.DownloadItemClick(item))
    }
}

@AndroidEntryPoint
class TorrentsListFragment : UnchainedFragment(), TorrentListListener {

    private val viewModel: ListTabsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentsListBinding.inflate(inflater, container, false)

        val torrentAdapter = TorrentListPagingAdapter(this)
        binding.rvTorrentList.adapter = torrentAdapter

        // torrent list selection  tracker
        binding.selectedTorrents = 0
        val torrentTracker: SelectionTracker<TorrentItem> =
            SelectionTracker.Builder(
                    "torrentListSelection",
                    binding.rvTorrentList,
                    TorrentKeyProvider(torrentAdapter),
                    DataBindingDetailsLookup(binding.rvTorrentList),
                    StorageStrategy.createParcelableStorage(TorrentItem::class.java))
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        torrentAdapter.tracker = torrentTracker

        torrentTracker.addObserver(
            object : SelectionTracker.SelectionObserver<TorrentItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedTorrents = torrentTracker.selection.size()
                    if (torrentTracker.selection.size() == 1) {
                        binding.bDetailsSelected.visibility = View.VISIBLE
                    } else {
                        binding.bDetailsSelected.visibility = View.GONE
                    }
                }
            })

        // listener for selection buttons
        binding.listener =
            object : SelectedItemsButtonsListener {
                override fun deleteSelectedItems() {
                    if (torrentTracker.selection.toList().isNotEmpty())
                        viewModel.deleteTorrents(torrentTracker.selection.toList())
                    else context?.showToast(R.string.select_one_item)
                }

                override fun shareSelectedItems() {
                    // do nothing for torrents
                }

                override fun downloadSelectedItems() {
                    if (torrentTracker.selection.toList().isNotEmpty()) {
                        viewModel.downloadItems(torrentTracker.selection.toList())
                    } else context?.showToast(R.string.select_one_item)
                }

                override fun openSelectedDetails() {
                    if (torrentTracker.selection.toList().size == 1) {
                        val item: TorrentItem = torrentTracker.selection.toList().first()
                        val action =
                            if (beforeSelectionStatusList.contains(item.status))
                                ListsTabFragmentDirections
                                    .actionListTabsDestToTorrentProcessingFragment(
                                        torrentID = item.id)
                            else ListsTabFragmentDirections.actionListsTabToTorrentDetails(item)
                        findNavController().navigate(action)
                    } else
                        Timber.e(
                            "Somehow user triggered openSelectedDetails with a selection size of ${torrentTracker.selection.toList().size}")
                }

                override fun openNewDownload() {
                    viewModel.postEventNotice(ListEvent.NewDownload)
                }

                override fun refreshList() {
                    if (!binding.srLayout.isRefreshing) {
                        binding.srLayout.isRefreshing = true
                        torrentAdapter.refresh()
                    }
                }
            }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                torrentTracker.setItemsSelected(torrentAdapter.snapshot().items, true)
            } else {
                torrentTracker.clearSelection()
            }
        }

        binding.srLayout.setOnRefreshListener { torrentAdapter.refresh() }

        val torrentObserver =
            Observer<PagingData<TorrentItem>> {
                lifecycleScope.launch {
                    torrentAdapter.submitData(it)
                    if (binding.srLayout.isRefreshing) {
                        binding.srLayout.isRefreshing = false
                        lifecycleScope.launch {
                            binding.rvTorrentList.delayedScrolling(requireContext())
                        }
                    }
                    delay(300)
                    torrentAdapter.notifyDataSetChanged()
                }
            }

        // checks the authentication state. Needed to avoid automatic API calls before the
        // authentication process is finished
        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            when (it?.peekContent()) {
                FSMAuthenticationState.AuthenticatedOpenToken,
                FSMAuthenticationState.AuthenticatedPrivateToken -> {
                    // register observers if not already registered
                    if (!viewModel.torrentsLiveData.hasActiveObservers())
                        viewModel.torrentsLiveData.observe(viewLifecycleOwner, torrentObserver)
                }
                else -> {}
            }
        }

        viewModel.deletedTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    TORRENT_NOT_DELETED -> {}
                    TORRENT_DELETED -> {
                        context?.showToast(R.string.torrent_removed)
                        torrentAdapter.refresh()
                    }
                    TORRENTS_DELETED_ALL -> {
                        context?.showToast(R.string.torrents_removed)
                        lifecycleScope.launch {
                            // if we don't refresh the cached copy of the last result will be
                            // restored on the
                            // first list redraw
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
            })

        activityViewModel.listStateLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    ListState.UpdateTorrent -> {
                        lifecycleScope.launch {
                            delay(300L)
                            torrentAdapter.refresh()
                            lifecycleScope.launch {
                                binding.rvTorrentList.delayedScrolling(requireContext())
                            }
                        }
                    }
                    else -> {}
                }
            })

        return binding.root
    }

    override fun onClick(item: TorrentItem) {

        // this was used to wait for some loading, can't remember the use case anymore
        // maybe a link share or a notification click?
        var loop = 0

        lifecycleScope.launch {
            val controller = findNavController()
            while (loop++ < 20 && controller.currentDestination?.id != R.id.list_tabs_dest) {
                delay(100)
            }

            if (item.status == "downloaded") {
                // unrestrict and move to download tab
                if (item.links.size > 1) {
                    val action =
                        ListsTabFragmentDirections.actionListTabsDestToFolderListFragment2(
                            folder = null, torrent = item, linkList = null)
                    if (controller.currentDestination?.id == R.id.list_tabs_dest)
                        controller.navigate(action)
                    else
                        Timber.e(
                            "Correct tab was not ready within 2 seconds after clicking torrent $item")
                } else viewModel.unrestrictTorrent(item)
            } else if (beforeSelectionStatusList.contains(item.status)) {
                // go to torrent processing since it is still loading
                val action =
                    ListsTabFragmentDirections.actionListTabsDestToTorrentProcessingFragment(
                        torrentID = item.id)
                if (controller.currentDestination?.id == R.id.list_tabs_dest)
                    controller.navigate(action)
                else
                    Timber.e(
                        "Correct tab was not ready within 2 seconds after clicking torrent $item")
            } else {
                // go to torrent details
                val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(item)
                if (controller.currentDestination?.id == R.id.list_tabs_dest)
                    controller.navigate(action)
                else
                    Timber.e(
                        "Correct tab was not ready within 2 seconds after clicking torrent $item")
            }
        }
    }
}

sealed class ListState {
    object UpdateTorrent : ListState()

    object UpdateDownload : ListState()

    object Ready : ListState()
}

interface SelectedItemsButtonsListener {
    fun deleteSelectedItems()

    fun shareSelectedItems()

    fun downloadSelectedItems()

    fun openSelectedDetails()

    fun openNewDownload()

    fun refreshList()
}
