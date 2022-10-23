package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem

private const val ARG_TORRENT = "torrent_arg"

class TorrentFilePickerFragment : Fragment() {

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
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_torrent_file_picker, container, false)
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
}