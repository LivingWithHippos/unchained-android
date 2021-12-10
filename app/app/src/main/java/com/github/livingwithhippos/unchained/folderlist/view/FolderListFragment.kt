package com.github.livingwithhippos.unchained.folderlist.view

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.databinding.FragmentFolderListBinding
import com.github.livingwithhippos.unchained.folderlist.model.FolderItemAdapter
import com.github.livingwithhippos.unchained.folderlist.model.FolderKeyProvider
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.lists.view.*
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.delayedScrolling
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
import com.github.livingwithhippos.unchained.utilities.extension.getThemedDrawable
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class FolderListFragment : Fragment(), DownloadListListener {

    private val viewModel: FolderListViewModel by viewModels()
    private val args: FolderListFragmentArgs by navArgs()

    private val mediaRegex =
        "\\.(webm|avi|mkv|ogg|MTS|M2TS|TS|mov|wmv|mp4|m4p|m4v|mp2|mpe|mpv|mpg|mpeg|m2v|3gp)$".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.folder_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.download_all -> {
                downloadAll()
                true
            }
            R.id.share_all -> {
                shareAll()
                true
            }
            R.id.copy_all -> {
                copyAll()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareAll() {
        val downloads: List<DownloadItem>? = viewModel.folderLiveData.value?.peekContent()

        if (!downloads.isNullOrEmpty()) {
            val downloadList = StringBuilder()
            downloads.forEach {
                downloadList.appendLine(it.download)
            }
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, downloadList.toString())
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_all_with)))
        } else {
            context?.showToast(R.string.no_links)
        }
    }

    private fun copyAll() {
        val downloads: List<DownloadItem>? = viewModel.folderLiveData.value?.peekContent()

        if (!downloads.isNullOrEmpty()) {
            val downloadList = StringBuilder()
            downloads.forEach {
                downloadList.appendLine(it.download)
            }
            copyToClipboard(getString(R.string.links), downloadList.toString())
            context?.showToast(R.string.link_copied)
        } else {
            context?.showToast(R.string.no_links)
        }
    }

    private fun downloadAll() {
        val downloads: List<DownloadItem>? = viewModel.folderLiveData.value?.peekContent()
        if (!downloads.isNullOrEmpty()) {
            var downloadStarted = false
            val manager =
                requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloads.forEach {
                val queuedDownload = manager.downloadFile(
                    it.download,
                    it.filename,
                    getString(R.string.app_name)
                )
                when (queuedDownload) {
                    is EitherResult.Failure -> {
                        context?.showToast(
                            getString(
                                R.string.download_not_started_format,
                                it.filename
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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFolderListBinding.inflate(inflater, container, false)

        setup(binding)
        return binding.root
    }

    private fun setup(binding: FragmentFolderListBinding) {

        binding.selectedLinks = 0

        if (viewModel.shouldShowFilters()) {
            binding.cbFilterSize.visibility = View.VISIBLE
            // load size filter button status
            binding.cbFilterSize.isChecked = viewModel.getFilterSizePreference()

            binding.cbFilterType.visibility = View.VISIBLE
            // load type filter button status
            binding.cbFilterType.isChecked = viewModel.getFilterTypePreference()
        } else {
            binding.cbFilterSize.visibility = View.GONE
            binding.cbFilterType.visibility = View.GONE
        }

        val adapter = FolderItemAdapter(this)

        // this must be assigned BEFORE the SelectionTracker builder
        binding.rvFolderList.adapter = adapter

        val linkTracker: SelectionTracker<DownloadItem> = SelectionTracker.Builder(
            "folderListSelection",
            binding.rvFolderList,
            FolderKeyProvider(adapter),
            DataBindingDetailsLookup(binding.rvFolderList),
            StorageStrategy.createParcelableStorage(DownloadItem::class.java)
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()

        adapter.tracker = linkTracker

        linkTracker.addObserver(
            object : SelectionTracker.SelectionObserver<DownloadItem>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    binding.selectedLinks = linkTracker.selection.size()
                }
            })

        binding.listener = object : SelectedItemsButtonsListener {
            override fun deleteSelectedItems() {
                if (linkTracker.selection.toList().isNotEmpty()) {
                    viewModel.deleteDownloadList(linkTracker.selection.toList())
                }
            }

            override fun shareSelectedItems() {
                if (linkTracker.selection.toList().isNotEmpty()) {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val shareLinks =
                        linkTracker.selection.joinToString("\n") { it.download }
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareLinks)
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            getString(R.string.share_with)
                        )
                    )
                }
            }

            override fun downloadSelectedItems() {
                if (linkTracker.selection.toList().isNotEmpty()) {
                    var downloadStarted = false
                    val manager =
                        requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    linkTracker.selection.forEach { item ->
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
            }
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                linkTracker.setItemsSelected(adapter.currentList, true)
            } else {
                linkTracker.clearSelection()
            }
        }


        viewModel.deletedDownloadLiveData.observe(
            viewLifecycleOwner
        ) {
            Timber.d(it.toString())
            it.getContentIfNotHandled()?.let { item ->
                val originalList = mutableListOf<DownloadItem>()
                originalList.addAll(adapter.currentList.minus(item))
                if (originalList.size != adapter.currentList.size) {
                    adapter.submitList(originalList)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        // observe the list loading status
        viewModel.folderLiveData.observe(viewLifecycleOwner) {
            // todo: if I use just getContent() I can restore on reload?
            it.getContentIfNotHandled()?.let { _ ->
                updateList(adapter)
                lifecycleScope.launch {
                    binding.rvFolderList.delayedScrolling(requireContext(), delay = 500)
                }
            }
        }

        // observe errors
        viewModel.errorsLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { exception ->
                when (exception) {
                    is APIError -> {
                        // val error = requireContext().getApiErrorMessage(exception.errorCode)
                        // requireContext().showToast(error)
                        binding.tvError.visibility = View.VISIBLE
                    }
                    is EmptyBodyError -> Timber.d("Empty Body error, return code: ${exception.returnCode}")
                    is NetworkError -> Timber.d("Network error, message: ${exception.message}")
                    is ApiConversionError -> Timber.d("Api Conversion error, error: ${exception.error}")
                }
            }
        }

        // update the progress bar
        viewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.progressIndicator.progress = it
        }

        // observe the search bar for changes
        binding.tiFilter.addTextChangedListener {
            viewModel.filterList(it?.toString())
        }

        viewModel.queryLiveData.observe(viewLifecycleOwner) {
            updateList(adapter)
            lifecycleScope.launch {
                binding.rvFolderList.delayedScrolling(requireContext())
            }
        }

        binding.cbFilterSize.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFilterSizePreference(isChecked)
            updateList(adapter)
            lifecycleScope.launch {
                binding.rvFolderList.delayedScrolling(requireContext())
            }
        }

        binding.cbFilterType.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFilterTypePreference(isChecked)
            updateList(adapter)
            lifecycleScope.launch {
                binding.rvFolderList.delayedScrolling(requireContext())
            }
        }

        // load list sorting status
        val sortTag = viewModel.getListSortPreference()
        val sortDrawableID = getSortingDrawable(sortTag)
        binding.sortingButton.background = requireContext().getThemedDrawable(sortDrawableID)

        binding.sortingButton.setOnClickListener {
            showSortingPopup(it, R.menu.sorting_popup, adapter, binding.rvFolderList)
        }

        // load all the links
        when {
            args.folder != null -> viewModel.retrieveFolderFileList(args.folder!!)
            args.torrent != null -> {
                binding.tvTitle.text = args.torrent!!.filename
                viewModel.retrieveFiles(args.torrent!!.links)
            }
            args.linkList != null -> {
                viewModel.retrieveFiles(args.linkList!!.toList())
            }
        }
    }

    private fun showSortingPopup(
        v: View,
        @MenuRes menuRes: Int,
        folderAdapter: FolderItemAdapter,
        folderList: RecyclerView
    ) {

        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // todo: check if the theme is needed, in case use getSortDrawable and remove from the menu xml the icons
            v.background = menuItem.icon
            // save the new sorting preference
            when (menuItem.itemId) {
                R.id.sortByDefault -> {
                    viewModel.setListSortPreference(TAG_DEFAULT_SORT)
                }
                R.id.sortByAZ -> {
                    viewModel.setListSortPreference(TAG_SORT_AZ)
                }
                R.id.sortByZA -> {
                    viewModel.setListSortPreference(TAG_SORT_ZA)
                }
                R.id.sortBySizeAsc -> {
                    viewModel.setListSortPreference(TAG_SORT_SIZE_ASC)
                }
                R.id.sortBySizeDesc -> {
                    viewModel.setListSortPreference(TAG_SORT_SIZE_DESC)
                }
            }
            // update the list and scroll it to the top
            updateList(folderAdapter)
            lifecycleScope.launch {
                folderList.delayedScrolling(requireContext())
            }
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun getSortingDrawable(tag: String): Int {
        return when (tag) {
            TAG_DEFAULT_SORT -> R.drawable.icon_sort_default
            TAG_SORT_AZ -> R.drawable.icon_sort_az
            TAG_SORT_ZA -> R.drawable.icon_sort_za
            TAG_SORT_SIZE_DESC -> R.drawable.icon_sort_size_desc
            TAG_SORT_SIZE_ASC -> R.drawable.icon_sort_size_asc
            else -> R.drawable.icon_sort_default
        }
    }

    private fun updateList(
        adapter: FolderItemAdapter
    ) {
        val items: List<DownloadItem>? = viewModel.folderLiveData.value?.peekContent()
        val filterSize: Boolean = viewModel.getFilterSizePreference()
        val filterType: Boolean = viewModel.getFilterTypePreference()
        val filterQuery: String? = viewModel.queryLiveData.value
        val sortTag: String = viewModel.getListSortPreference()

        val customizedList = mutableListOf<DownloadItem>()

        if (!items.isNullOrEmpty()) {
            customizedList.addAll(items)
            if (filterSize) {
                customizedList.clear()
                customizedList.addAll(
                    items.filter {
                        it.fileSize > viewModel.getMinFileSize()
                    }
                )
            }
            if (filterType) {
                val temp = customizedList.filter {
                    mediaRegex.find(it.filename) != null
                }
                customizedList.clear()
                customizedList.addAll(temp)
            }
            if (!filterQuery.isNullOrBlank()) {
                val temp = customizedList.filter { item ->
                    item.filename.contains(filterQuery)
                }
                customizedList.clear()
                customizedList.addAll(temp)
            }
        }
        // if I get passed an empty list I need to empty the list (shouldn't be possible in this particular fragment)
        when (sortTag) {
            TAG_DEFAULT_SORT -> {
                adapter.submitList(customizedList)
            }
            TAG_SORT_AZ -> {
                adapter.submitList(
                    customizedList.sortedBy { item ->
                        item.filename
                    }
                )
            }
            TAG_SORT_ZA -> {
                adapter.submitList(
                    customizedList.sortedByDescending { item ->
                        item.filename
                    }
                )
            }
            TAG_SORT_SIZE_DESC -> {
                adapter.submitList(
                    customizedList.sortedByDescending { item ->
                        item.fileSize
                    }
                )
            }
            TAG_SORT_SIZE_ASC -> {
                adapter.submitList(
                    customizedList.sortedBy { item ->
                        item.fileSize
                    }
                )
            }
            else -> {
                adapter.submitList(customizedList)
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onClick(item: DownloadItem) {
        val action =
            FolderListFragmentDirections.actionFolderListFragmentToDownloadDetailsDest(item)
        findNavController().navigate(action)
    }

    companion object {
        const val TAG_DEFAULT_SORT = "sort_default_tag"
        const val TAG_SORT_AZ = "sort_az_tag"
        const val TAG_SORT_ZA = "sort_za_tag"
        const val TAG_SORT_SIZE_ASC = "sort_size_asc_tag"
        const val TAG_SORT_SIZE_DESC = "sort_size_desc_tag"
    }
}
