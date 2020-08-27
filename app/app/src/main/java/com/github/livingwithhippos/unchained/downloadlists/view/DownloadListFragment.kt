package com.github.livingwithhippos.unchained.downloadlists.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadListBinding
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.newdownload.view.NewDownloadFragmentDirections
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class DownloadListFragment : Fragment(), DownloadListListener {

    private val viewModel: DownloadListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadsBinding = FragmentDownloadListBinding.inflate(inflater, container, false)

        val adapter = DownloadListAdapter(this)
        downloadsBinding.rvDownloadList.adapter = adapter

        viewModel.downloadLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.submitList(it)
                // move this outside of the the null check?
                downloadsBinding.srLayout.isRefreshing = false
            }
        })

        viewModel.torrentLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                //downloadsBinding.torrents = it
            }
        })

        viewModel.fetchAll()

        downloadsBinding.srLayout.setOnRefreshListener {
            viewModel.fetchAll()
        }

        return downloadsBinding.root
    }

    override fun onClick(item: DownloadItem) {

        val action = DownloadListFragmentDirections.actionDownloadListToDownloadDetails(item)
        findNavController().navigate(action)
    }

}