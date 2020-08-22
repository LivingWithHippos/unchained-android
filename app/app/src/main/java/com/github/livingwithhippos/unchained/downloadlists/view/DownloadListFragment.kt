package com.github.livingwithhippos.unchained.downloadlists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadListBinding
import com.github.livingwithhippos.unchained.downloadlists.viewmodel.DownloadListViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class DownloadListFragment : Fragment() {

    private val viewModel: DownloadListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadsBinding = FragmentDownloadListBinding.inflate(inflater, container, false)

        viewModel.downloadLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                downloadsBinding.downloads = it
            }
        })

        viewModel.torrentLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                downloadsBinding.torrents = it
            }
        })

        return downloadsBinding.root
    }

}