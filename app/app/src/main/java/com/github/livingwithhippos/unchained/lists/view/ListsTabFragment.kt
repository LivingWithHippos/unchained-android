package com.github.livingwithhippos.unchained.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingData
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.TablayoutListBinding
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.view.DownloadListFragmentDirections
import com.github.livingwithhippos.unchained.downloadlists.view.DownloadListListener
import com.github.livingwithhippos.unchained.downloadlists.view.DownloadListPagingAdapter
import com.github.livingwithhippos.unchained.downloadlists.viewmodel.DownloadListViewModel
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListsTabFragment: UnchainedFragment(), DownloadListListener {

    private val viewModel: DownloadListViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val listBinding = TablayoutListBinding.inflate(inflater, container, false)

        listBinding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab select
                when (tab?.position) {
                    1 -> {//load downloads\
                    }
                    2 -> {//load torrents
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // either do nothing or refresh
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // do nothing
            }
        })

        val adapter = DownloadListPagingAdapter(this)
        listBinding.rvDownloadList.adapter = adapter

        //todo: add scroll to top when a new item is added
        listBinding.srLayout.setOnRefreshListener {
            adapter.refresh()
        }

        // observer created to easily add and remove it. Pass the retrieved download list to the adapter and removes the loading icon from the swipe layout
        val downloadObserver = Observer<PagingData<DownloadItem>> {
            lifecycleScope.launch {
                adapter.submitData(it)
                listBinding.srLayout.isRefreshing = false
            }
        }

        // checks the authentication state. Needed to avoid automatic API calls before the authentication process is finished
        activityViewModel.authenticationState.observe(viewLifecycleOwner, Observer {
            if (it.peekContent() == MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                // register observer if not already registered
                if (!viewModel.listData.hasActiveObservers())
                    viewModel.listData.observe(viewLifecycleOwner, downloadObserver)
            } else {
                // remove observer if present
                viewModel.listData.removeObserver(downloadObserver)
            }
        })

        return listBinding.root
    }

    override fun onClick(item: DownloadItem) {
    }

}