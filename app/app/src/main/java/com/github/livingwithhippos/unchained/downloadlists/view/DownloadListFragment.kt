package com.github.livingwithhippos.unchained.downloadlists.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadListBinding
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.newdownload.view.NewDownloadFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DownloadListFragment : Fragment(), DownloadListListener {

    private val viewModel: DownloadListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadsBinding = FragmentDownloadListBinding.inflate(inflater, container, false)

        val adapter = DownloadListPagingAdapter(this)
        downloadsBinding.rvDownloadList.adapter = adapter

        downloadsBinding.srLayout.setOnRefreshListener {
            adapter.refresh()
        }

        viewModel.listData.observe(viewLifecycleOwner, Observer {
            lifecycleScope.launch {
                adapter.submitData(it)
                downloadsBinding.srLayout.isRefreshing = false
            }
        })

        return downloadsBinding.root
    }

    override fun onClick(item: DownloadItem) {

        val action = DownloadListFragmentDirections.actionDownloadListToDownloadDetails(item)
        findNavController().navigate(action)
    }

}