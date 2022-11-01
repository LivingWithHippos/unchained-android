package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentStartBinding
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentFilePickerBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentFilesSelectionAdapter
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.utilities.Node
import timber.log.Timber

private const val ARG_TORRENT = "torrent_arg"

class TorrentFilePickerFragment : Fragment(), TorrentContentListener {

    private var torrent: TorrentItem? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            torrent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_TORRENT, TorrentItem::class.java)
            } else {
                it.getParcelable(ARG_TORRENT)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentFilePickerBinding.inflate(inflater, container, false)

        torrent?.let {

            val torrentStructure: Node<TorrentFileItem> =
                getFilesNodes(it, selectedOnly = false)
            // show list only if it's populated enough
            if (torrentStructure.children.size > 0) {

                val adapter = TorrentContentFilesSelectionAdapter(this)
                val filesList = mutableListOf<TorrentFileItem>()
                Node.traverseDepthFirst(torrentStructure) { item ->
                    filesList.add(item)
                }
                adapter.submitList(filesList)
            }
        }


        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(torrent: TorrentItem?) =
            TorrentFilePickerFragment().apply {
                if (torrent != null)
                    arguments = Bundle().apply {
                        putParcelable(ARG_TORRENT, torrent)
                    }
            }
    }

    override fun selectItem(item: TorrentFileItem) {
        TODO("Not yet implemented")
    }
}