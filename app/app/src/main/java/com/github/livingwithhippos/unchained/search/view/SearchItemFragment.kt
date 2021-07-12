package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentSearchItemBinding
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.LinkItem
import com.github.livingwithhippos.unchained.search.model.LinkItemAdapter
import com.github.livingwithhippos.unchained.search.model.LinkItemListener
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchItemFragment : UnchainedFragment(), LinkItemListener {

    private val args: SearchItemFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchItemBinding.inflate(inflater, container, false)

        setup(binding)
        return binding.root
    }

    private fun setup(binding: FragmentSearchItemBinding) {
        val item: ScrapedItem = args.item
        binding.item = item

        binding.linkCaption.setOnClickListener {
            if (item.link != null)
                openExternalWebPage(item.link)
        }

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
