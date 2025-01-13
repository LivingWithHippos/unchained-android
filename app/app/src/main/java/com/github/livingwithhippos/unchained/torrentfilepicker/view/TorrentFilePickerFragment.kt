package com.github.livingwithhippos.unchained.torrentfilepicker.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentFilePickerBinding
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentFilesSelectionAdapter
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.Node
import org.koin.androidx.navigation.koinNavGraphViewModel
import timber.log.Timber

class TorrentFilePickerFragment : Fragment(), TorrentContentListener {

    // https://insert-koin.io/docs/reference/koin-android/viewmodel#navigation-graph-viewmodel
    private val viewModel: TorrentProcessingViewModel by
    koinNavGraphViewModel(R.id.navigation_lists)

    private var currentStructure: Node<TorrentFileItem>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentTorrentFilePickerBinding.inflate(inflater, container, false)

        val adapter = TorrentContentFilesSelectionAdapter(this)
        binding.rvTorrentFilePicker.adapter = adapter

        viewModel.torrentLiveData.observe(viewLifecycleOwner) {
            when (val content = it.peekContent()) {
                is TorrentEvent.TorrentInfo -> {

                    if (currentStructure == null) {
                        val torrentStructure: Node<TorrentFileItem> =
                            getFilesNodes(content.item, selectedOnly = false)
                        if (torrentStructure.children.size > 0) {
                            currentStructure = torrentStructure
                            viewModel.updateTorrentStructure(torrentStructure)
                        }
                    }
                }
                else -> {
                    // not used by this fragment
                }
            }
        }

        viewModel.structureLiveData.observe(viewLifecycleOwner) {
            val content = it.getContentIfNotHandled()
            if (content != null) {
                val filesList = mutableListOf<TorrentFileItem>()
                Node.traverseDepthFirst(content) { item -> filesList.add(item) }
                adapter.submitList(filesList)
                adapter.notifyDataSetChanged()
            }
        }

        return binding.root
    }

    companion object {
        @JvmStatic fun newInstance() = TorrentFilePickerFragment()
    }

    override fun onSelectedFile(item: TorrentFileItem) {
        Timber.d("selected file $item was ${item.selected}")
        currentStructure?.let { structure ->
            Node.traverseDepthFirst(structure) {
                if (it.id == item.id) {
                    it.selected = !it.selected
                }
            }
        }
        viewModel.updateTorrentStructure(currentStructure)
    }

    override fun onSelectedFolder(item: TorrentFileItem) {
        Timber.d("selected folder $item")
        currentStructure?.let { structure ->
            var folderNode: Node<TorrentFileItem>? = null
            Node.traverseNodeDepthFirst(structure) {
                if (
                    it.value.absolutePath == item.absolutePath &&
                        it.value.name == item.name &&
                        it.value.id == TYPE_FOLDER &&
                        item.id == TYPE_FOLDER
                ) {
                    folderNode = it
                    return@traverseNodeDepthFirst
                }
            }

            folderNode?.let {
                val newSelected = !it.value.selected
                it.value.selected = newSelected
                Node.traverseDepthFirst(it) { item -> item.selected = newSelected }
            }
        }

        viewModel.updateTorrentStructure(currentStructure)
    }
}
