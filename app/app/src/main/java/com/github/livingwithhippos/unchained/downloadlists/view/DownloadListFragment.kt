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
import androidx.paging.PagingData
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadListBinding
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.newdownload.view.NewDownloadFragmentDirections
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DownloadListFragment : UnchainedFragment(), DownloadListListener {

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

        // observer created to easily add and remove it. Pass the retrieved download list to the adapter and removes the loading icon from the swipe layout
        val downloadObserver = Observer<PagingData<DownloadItem>> {
            lifecycleScope.launch {
                adapter.submitData(it)
                downloadsBinding.srLayout.isRefreshing = false
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(viewLifecycleOwner, Observer {
            if (it == MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                // register observer if not already registered
                if (!viewModel.listData.hasActiveObservers())
                    viewModel.listData.observe(viewLifecycleOwner, downloadObserver)
            } else {
                // remove observer if present
                viewModel.listData.removeObserver(downloadObserver)
                // [MainActivity] will observe this value and go back to home with the login page
                activityViewModel.setUnauthenticated()
            }
        })

        return downloadsBinding.root
    }

    override fun onClick(item: DownloadItem) {

        val action = DownloadListFragmentDirections.actionDownloadListToDownloadDetails(item)
        findNavController().navigate(action)
    }

}