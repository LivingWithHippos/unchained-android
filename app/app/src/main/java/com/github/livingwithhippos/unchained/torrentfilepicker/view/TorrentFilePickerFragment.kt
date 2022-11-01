package com.github.livingwithhippos.unchained.torrentfilepicker.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentFilePickerBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentFilesSelectionAdapter
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.utilities.Node

private const val ARG_TORRENT = "torrent_arg"

class TorrentFilePickerFragment : Fragment(), TorrentContentListener {

    // https://developer.android.com/training/dependency-injection/hilt-jetpack#viewmodel-navigation
    private val viewModel: TorrentProcessingViewModel by hiltNavGraphViewModels(R.id.navigation_lists)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentFilePickerBinding.inflate(inflater, container, false)

        val adapter = TorrentContentFilesSelectionAdapter(this)
        binding.rvTorrentFilePicker.adapter = adapter

        viewModel.torrentLiveData.observe(viewLifecycleOwner) {
            when (val content = it.peekContent()) {
                is TorrentEvent.TorrentInfo -> {

                    val torrentStructure: Node<TorrentFileItem> =
                        getFilesNodes(content.item, selectedOnly = false)
                    // show list only if it's populated enough
                    if (torrentStructure.children.size > 0) {
                        val filesList = mutableListOf<TorrentFileItem>()
                        Node.traverseDepthFirst(torrentStructure) { item ->
                            filesList.add(item)
                        }
                        adapter.submitList(filesList)
                    }
                }
                else -> {
                    // not used by this fragment
                }
            }
        }



        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            TorrentFilePickerFragment()
    }

    override fun selectItem(item: TorrentFileItem) {
        TODO("Not yet implemented")
    }
}