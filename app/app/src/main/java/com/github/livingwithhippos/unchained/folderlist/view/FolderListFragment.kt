package com.github.livingwithhippos.unchained.folderlist.view

import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.databinding.FragmentFolderListBinding
import com.github.livingwithhippos.unchained.folderlist.model.FolderItemAdapter
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.lists.view.DownloadListListener
import com.github.livingwithhippos.unchained.utilities.EitherResult
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
            else -> super.onOptionsItemSelected(item)
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

        val adapter = FolderItemAdapter(this)
        binding.rvFolderList.adapter = adapter

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
            binding.loadingCircle.progress = it
            if (it >= 100) {
                binding.bDownloadAll.visibility = View.VISIBLE
                binding.loadingCircle.visibility = View.GONE
            }
        }

        binding.bDownloadAll.setOnClickListener {
            downloadAll()
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

        // load size filter button status
        binding.cbFilterSize.isChecked = viewModel.getFilterSizePreference()

        binding.cbFilterSize.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFilterSizePreference(isChecked)
            updateList(adapter)
            lifecycleScope.launch {
                binding.rvFolderList.delayedScrolling(requireContext())
            }
        }

        // load type filter button status
        binding.cbFilterType.isChecked = viewModel.getFilterTypePreference()

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

    private fun getNextSortingTag(currentTag: String): String {
        return when (currentTag) {
            TAG_DEFAULT_SORT -> TAG_SORT_AZ
            TAG_SORT_AZ -> TAG_SORT_ZA
            TAG_SORT_ZA -> TAG_SORT_SIZE_DESC
            TAG_SORT_SIZE_DESC -> TAG_SORT_SIZE_ASC
            TAG_SORT_SIZE_ASC -> TAG_DEFAULT_SORT
            else -> TAG_DEFAULT_SORT
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
