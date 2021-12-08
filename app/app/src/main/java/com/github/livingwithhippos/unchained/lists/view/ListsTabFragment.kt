package com.github.livingwithhippos.unchained.lists.view

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentTabListsBinding
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
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private val viewModel: ListTabsViewModel by viewModels()

    // used to simulate a debounce effect while typing on the search bar
    var queryJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentTabListsBinding =
            FragmentTabListsBinding.inflate(inflater, container, false)

        binding.selectedDownloads = 0
        binding.selectedTorrents = 0
        binding.selectedTab = 0

        val downloadAdapter = DownloadListPagingAdapter(this)
        val torrentAdapter = TorrentListPagingAdapter(this)

        binding.rvDownloadList.adapter = downloadAdapter
        binding.rvTorrentList.adapter = torrentAdapter

        // torrent list selection  tracker
        val torrentTracker: SelectionTracker<TorrentItem> = SelectionTracker.Builder(
            "torrentListSelection",
            binding.rvTorrentList,
            TorrentKeyProvider(torrentAdapter),
            DataBindingDetailsLookup(binding.rvTorrentList),
            StorageStrategy.createParcelableStorage(TorrentItem::class.java)
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        torrentAdapter.tracker = torrentTracker

        torrentTracker.addObserver(
            object : SelectionTracker.SelectionObserver<TorrentItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedTorrents = torrentTracker.selection.size()
                }
            })

        // download list selection  tracker
        val downloadTracker: SelectionTracker<DownloadItem> = SelectionTracker.Builder(
            "downloadListSelection",
            binding.rvDownloadList,
            DownloadKeyProvider(downloadAdapter),
            DataBindingDetailsLookup(binding.rvDownloadList),
            StorageStrategy.createParcelableStorage(DownloadItem::class.java)
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        downloadAdapter.tracker = downloadTracker

        downloadTracker.addObserver(
            object : SelectionTracker.SelectionObserver<DownloadItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedDownloads = downloadTracker.selection.size()
                }
            })

        // listener for selection buttons
        binding.listener = object : SelectedItemsButtonsListener {
            override fun deleteSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {
                    if (downloadTracker.selection.toList().isNotEmpty())
                        viewModel.deleteDownloads(downloadTracker.selection.toList())
                } else {
                    if (torrentTracker.selection.toList().isNotEmpty())
                        viewModel.deleteTorrents(torrentTracker.selection.toList())
                }
            }

            override fun shareSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {

                    if (downloadTracker.selection.toList().isNotEmpty()) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        val shareLinks =
                            downloadTracker.selection.joinToString("\n") { it.download }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareLinks)
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.share_with)
                            )
                        )
                    }
                }
            }

            override fun downloadSelectedItems() {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {

                    if (downloadTracker.selection.toList().isNotEmpty()) {
                        var downloadStarted = false
                        val manager =
                            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadTracker.selection.forEach { item ->
                            val queuedDownload = manager.downloadFile(
                                item.download,
                                item.filename,
                                getString(R.string.app_name),
                            )
                            when (queuedDownload) {
                                is EitherResult.Failure -> {
                                    context?.showToast(
                                        getString(
                                            R.string.download_not_started_format,
                                            item.filename
                                        )
                                    )
                                }
                                is EitherResult.Success -> {
                                    downloadStarted = true
                                }
                            }
                        }
                        if (downloadStarted)
                            context?.showToast(R.string.download_started)
                    }
                } else {
                    if (torrentTracker.selection.toList().isNotEmpty()) {
                        viewModel.downloadItems(torrentTracker.selection.toList())
                    }
                }
            }
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {
                    downloadTracker.setItemsSelected(downloadAdapter.snapshot().items, true)
                } else {
                    torrentTracker.setItemsSelected(torrentAdapter.snapshot().items, true)
                }
            } else {
                if (binding.tabs.selectedTabPosition == TAB_DOWNLOADS) {
                    downloadTracker.clearSelection()
                } else {
                    torrentTracker.clearSelection()
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
                if (binding.srLayout.isRefreshing) {
                    binding.srLayout.isRefreshing = false
                    // scroll to top if we were refreshing
                    lifecycleScope.launch {
                        binding.rvDownloadList.delayedScrolling(requireContext())
                    }
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
                    lifecycleScope.launch {
                        binding.rvTorrentList.delayedScrolling(requireContext())
                    }
                }
                delay(300)
                torrentAdapter.notifyDataSetChanged()
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner, {
            if (it != null) {
                when (it.peekContent()) {
                    FSMAuthenticationState.AuthenticatedOpenToken, FSMAuthenticationState.AuthenticatedPrivateToken -> {
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
                    }
                }
            }
        }
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {

                    binding.selectedTab = it.position

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
                            lifecycleScope.launch {
                                binding.rvDownloadList.delayedScrolling(requireContext())
                            }
                        }
                    }
                    ListState.UPDATE_TORRENT -> {
                        lifecycleScope.launch {
                            delay(300L)
                            torrentAdapter.refresh()
                            lifecycleScope.launch {
                                binding.rvTorrentList.delayedScrolling(requireContext())
                            }
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
                    DOWNLOADS_DELETED -> {
                        context?.showToast(R.string.downloads_removed)
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
                                    if (activityViewModel.getAuthenticationMachineState() is FSMAuthenticationState.AuthenticatedOpenToken)
                                        activityViewModel.transitionAuthenticationMachine(
                                            FSMAuthenticationEvent.OnExpiredOpenToken
                                        )
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

        binding.fabNewDownload.setOnClickListener {
            val action = ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment()
            findNavController().navigate(action)
        }

        // starts the Transformations.switchMap(queryLiveData) which otherwise won't trigger the Paging request
        viewModel.setListFilter("")

        binding.tabs.getTabAt(viewModel.getSelectedTab())?.select()

        // an external link has been shared with the app
        activityViewModel.externalLinkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { uri ->
                val action =
                    ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment(externalUri = uri)
                findNavController().navigate(action)
            }
        )
        // a file has been downloaded, usually a torrent, and needs to be unrestricted
        activityViewModel.downloadedFileLiveData.observe(
            viewLifecycleOwner,
            EventObserver { fileID ->
                val uri = requireContext().getDownloadedFileUri(fileID)
                // no need to recheck the extension since it was checked on download
                // if (uri?.path?.endsWith(".torrent") == true)
                if (uri?.path != null) {
                    val action = ListsTabFragmentDirections.actionListTabsDestToNewDownloadFragment(
                        externalUri = uri
                    )
                    findNavController().navigate(action)
                }
            }
        )

        // a notification has been clicked
        activityViewModel.notificationTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver { torrentID ->
                val action = ListsTabFragmentDirections.actionListsTabToTorrentDetails(torrentID)
                findNavController().navigate(action)
            }
        )

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
            // since there is a 500ms delay on new queries, this will help if the user types something and press search in less than half sec. May be unnecessary. The value is checked anyway in the ViewModel to avoid reloading with the same query as the last one.
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
    }

    override fun onClick(item: TorrentItem) {
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
