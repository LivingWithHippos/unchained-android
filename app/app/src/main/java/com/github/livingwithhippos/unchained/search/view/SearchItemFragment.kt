package com.github.livingwithhippos.unchained.search.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.cache.InstantAvailability
import com.github.livingwithhippos.unchained.databinding.FragmentSearchItemBinding
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.model.LinkItem
import com.github.livingwithhippos.unchained.search.model.LinkItemAdapter
import com.github.livingwithhippos.unchained.search.model.LinkItemListener
import com.github.livingwithhippos.unchained.search.viewmodel.SearchViewModel
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchItemFragment : UnchainedFragment(), LinkItemListener {

    private val args: SearchItemFragmentArgs by navArgs()

    private val magnetPattern = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchItemBinding.inflate(inflater, container, false)

        val item: ScrapedItem = args.item
        setup(binding, item)
        return binding.root
    }

    private fun setup(binding: FragmentSearchItemBinding, item: ScrapedItem) {
        binding.item = item

        binding.linkCaption.setOnClickListener {
            if (item.link != null)
                context?.openExternalWebPage(item.link)
        }

        val adapter = LinkItemAdapter(this)
        binding.linkList.adapter = adapter

        val links = mutableListOf<LinkItem>()

        val cache: List<String> = activityViewModel.cacheLiveData.value?.cachedTorrents?.map {
            it.btih
        } ?: emptyList()
        item.magnets.forEach {
            val btih = magnetPattern.find(it)?.groupValues?.getOrNull(1)?.lowercase()
            if (btih != null && cache.contains(btih))
                links.add(LinkItem(getString(R.string.magnet), it.substringBefore("&"), it,true))
            else
                links.add(LinkItem(getString(R.string.magnet), it.substringBefore("&"), it))
        }
        item.torrents.forEach {
            links.add(LinkItem(getString(R.string.torrent), it, it))
        }
        item.hosting.forEach {
            links.add(LinkItem(getString(R.string.hoster), it, it))
        }
        adapter.submitList(links)
    }

    override fun onClick(item: LinkItem) {
        activityViewModel.downloadSupportedLink(item.link)
    }
}
