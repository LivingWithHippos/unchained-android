package com.github.livingwithhippos.unchained.lists.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.TablayoutListBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ListsTabFragment: UnchainedFragment() {

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

        return listBinding.root
    }
}