package com.github.livingwithhippos.unchained.folderlist.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
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
import com.github.livingwithhippos.unchained.utilities.extension.verticalScrollToPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class FolderListFragment : Fragment(), DownloadListListener {

    private val viewModel: FolderListViewModel by viewModels()
    private val args: FolderListFragmentArgs by navArgs()

    private val mediaRegex =
        "\\.(webm|avi|mkv|ogg|MTS|M2TS|TS|mov|wmv|mp4|m4p|m4v|mp2|mpe|mpv|mpg|mpeg|m2v|3gp)$".toRegex()

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
        // todo: add more sorting methodds, dinamically chosen by the user
        viewModel.folderLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { files ->
                updateList(binding, adapter, list = files)
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
        }

        // load all the links
        if (args.folder != null)
            viewModel.retrieveFolderFileList(args.folder!!)
        else if (args.torrent != null) {
            binding.tvTitle.text = args.torrent!!.filename
            viewModel.retrieveFiles(args.torrent!!.links)
        } else if (args.linkList != null) {
            viewModel.retrieveFiles(args.linkList!!.toList())
        }

        // observe the search bar for changes
        binding.tiFilter.addTextChangedListener {
            viewModel.filterList(it?.toString())
        }
        viewModel.queryLiveData.observe(viewLifecycleOwner) {
            updateList(binding, adapter, query = it)
        }

        binding.cbFilterSize.setOnCheckedChangeListener { _, isChecked ->
            updateList(binding, adapter, size = isChecked)
        }

        binding.cbFilterType.setOnCheckedChangeListener { _, isChecked ->
            updateList(binding, adapter, type = isChecked)
        }

        binding.sortingButton.setOnClickListener {
            // every click changes to the next state
            when (it.tag) {
                TAG_SORT_AZ -> {
                    binding.sortingButton.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_sort_za,
                        requireContext().theme
                    )
                    it.tag = TAG_SORT_ZA
                    updateList(binding, adapter, sort = TAG_SORT_ZA)
                }
                TAG_SORT_ZA -> {
                    binding.sortingButton.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_sort_size_desc,
                        requireContext().theme
                    )
                    it.tag = TAG_SORT_SIZE_DESC
                    updateList(binding, adapter, sort = TAG_SORT_SIZE_DESC)
                }
                TAG_SORT_SIZE_DESC -> {
                    binding.sortingButton.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_sort_size_asc,
                        requireContext().theme
                    )
                    it.tag = TAG_SORT_SIZE_ASC
                    updateList(binding, adapter, sort = TAG_SORT_SIZE_ASC)
                }
                TAG_SORT_SIZE_ASC -> {
                    binding.sortingButton.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_sort_az,
                        requireContext().theme
                    )
                    it.tag = TAG_SORT_AZ
                    updateList(binding, adapter, sort = TAG_SORT_AZ)
                }
                else -> {
                    binding.sortingButton.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_sort_az,
                        requireContext().theme
                    )
                    it.tag = TAG_SORT_AZ
                    updateList(binding, adapter, sort = TAG_SORT_AZ)
                }
            }
        }
    }

    private fun updateList(
        binding: FragmentFolderListBinding,
        adapter: FolderItemAdapter,
        list: List<DownloadItem>? = null,
        size: Boolean? = null,
        type: Boolean? = null,
        query: String? = null,
        sort: String? = null
    ) {
        val items: List<DownloadItem>? = list ?: viewModel.folderLiveData.value?.peekContent()
        val filterSize: Boolean = size ?: binding.cbFilterSize.isChecked
        val filterType: Boolean = type ?: binding.cbFilterType.isChecked
        val filterQuery: String? = query ?: viewModel.queryLiveData.value
        val sortTag: String = sort ?: binding.sortingButton.tag.toString()

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
                adapter.submitList(
                    customizedList.sortedBy { item ->
                        item.filename
                    }
                )
            }
        }
        adapter.notifyDataSetChanged()
        lifecycleScope.launch {
            delay(100)
            binding.rvFolderList.layoutManager?.verticalScrollToPosition(
                requireContext(),
                position = 0
            )
        }
    }

    override fun onClick(item: DownloadItem) {
        val action =
            FolderListFragmentDirections.actionFolderListFragmentToDownloadDetailsDest(item)
        findNavController().navigate(action)
    }

    override fun onLongClick(item: DownloadItem) {
        // do nothing for now
    }

    companion object {
        const val TAG_SORT_AZ = "sort_az_tag"
        const val TAG_SORT_ZA = "sort_za_tag"
        const val TAG_SORT_SIZE_ASC = "sort_size_asc_tag"
        const val TAG_SORT_SIZE_DESC = "sort_size_desc_tag"
    }
}
