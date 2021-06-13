package com.github.livingwithhippos.unchained.folderlist.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.databinding.FragmentFolderListBinding
import com.github.livingwithhippos.unchained.folderlist.model.FolderItemAdapter
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.lists.view.DownloadListListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FolderListFragment : Fragment(), DownloadListListener {

    private var _binding: FragmentFolderListBinding? = null
    val binding get() = _binding!!

    private val viewModel: FolderListViewModel by viewModels()
    private val args: FolderListFragmentArgs by navArgs()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val mediaRegex = "\\.(webm|avi|mkv|ogg|MTS|M2TS|TS|mov|wmv|mp4|m4p|m4v|mp2|mpe|mpv|mpg|mpeg|m2v|3gp)$".toRegex()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderListBinding.inflate(inflater, container, false)

        setup()
        return binding.root
    }

    private fun setup() {

        val adapter = FolderItemAdapter(this)
        binding.rvFolderList.adapter = adapter

        // observe the list loading status
        // todo: add more sorting methodds, dinamically chosen by the user
        viewModel.folderLiveData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { files ->
                updateList(adapter, list = files)
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
            viewModel.retrieveTorrentFileList(args.torrent!!)
        }

        // observe the search bar for changes
        binding.tiFilter.addTextChangedListener {
            viewModel.filterList(it?.toString())
        }
        viewModel.queryLiveData.observe(viewLifecycleOwner) {
            updateList(adapter, query = it)
        }

        binding.cbFilterSize.setOnCheckedChangeListener { _, isChecked ->
            updateList(adapter, size = isChecked)
        }

        binding.cbFilterType.setOnCheckedChangeListener { _, isChecked ->
            updateList(adapter, type = isChecked)
        }
    }

    private fun updateList(
        adapter: FolderItemAdapter,
        list: List<DownloadItem>? = null,
        size: Boolean? = null,
        type: Boolean? = null,
        query: String? = null
    ) {
        val items: List<DownloadItem>? = list ?: viewModel.folderLiveData.value?.peekContent()
        val filterSize: Boolean = size ?: binding.cbFilterSize.isChecked
        val filterType: Boolean = type ?: binding.cbFilterType.isChecked
        val filterQuery: String? = query ?: viewModel.queryLiveData.value

        val customizedList = mutableListOf<DownloadItem>()

        if (!items.isNullOrEmpty()) {
            customizedList.addAll(items)
            if (filterSize) {
                customizedList.clear()
                customizedList.addAll(
                    items.filter {
                        it.fileSize > MAX_SIZE_BYTE
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
        adapter.submitList(customizedList.sortedBy { item ->
            item.filename
        })
        adapter.notifyDataSetChanged()
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
        // 10 MB
        const val MAX_SIZE_BYTE = (1024*1024)*10
    }
}
