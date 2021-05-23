package com.github.livingwithhippos.unchained.search.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchItemBinding
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.search.model.LinkItem
import com.github.livingwithhippos.unchained.search.model.LinkItemAdapter
import com.github.livingwithhippos.unchained.search.model.LinkItemListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SearchItemFragment : UnchainedFragment(), LinkItemListener {

    private var _binding: FragmentSearchItemBinding? = null
    val binding get() = _binding!!

    private val args: SearchItemFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchItemBinding.inflate(inflater, container, false)

        setup()
        return binding.root
    }

    private fun setup() {
        val item: LinkData = args.item
        binding.item = item


        val adapter = LinkItemAdapter(this)
        binding.linkList.adapter = adapter

        val links = mutableListOf<LinkItem>()
        item.magnets.forEach {
            links.add(LinkItem(getString(R.string.magnet), it))
        }
        item.torrents.forEach {
            links.add(LinkItem(getString(R.string.torrent), it))
        }
        adapter.submitList(links)

    }

    override fun onClick(item: LinkItem) {
        activityViewModel.downloadSupportedLink(item.link)
    }
}