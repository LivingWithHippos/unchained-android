package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentCachePickerBinding
import com.github.livingwithhippos.unchained.torrentfilepicker.view.TorrentCacheListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

private const val CACHE_LIST_KEY = "key_cache_list"

class TorrentCachePickerFragment : UnchainedFragment() {

    private lateinit var cache: CachedTorrent
    private lateinit var torrentID: String

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            // it.classLoader = ?
            cache = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(CACHE_LIST_KEY, CachedTorrent::class.java)!!
            } else {
                it.getParcelable(CACHE_LIST_KEY)!!
            }
            torrentID = it.getString(KEY_TORRENT_ID) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentCachePickerBinding.inflate(inflater, container, false)

        val cacheAdapter =
            CachePagerAdapter(this, cache)
        binding.cachePager.adapter = cacheAdapter
        binding.cacheTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                activityViewModel.setCurrentTorrentCachePick(torrentID, tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout: TabLayout = view.findViewById(R.id.cacheTabs)
        val viewPager: ViewPager2 = view.findViewById(R.id.cachePager)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(R.string.cache_format, position + 1)
        }.attach()

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        const val KEY_CACHE_INDEX = "cache_index_key"
        const val KEY_TORRENT_ID = "torrent_id_key"

        @JvmStatic
        fun newInstance(cache: CachedTorrent?, torrentID: String) =
            TorrentCachePickerFragment().apply {
                if (cache != null)
                    arguments = Bundle().apply {
                        putParcelable(CACHE_LIST_KEY, cache)
                        putString(KEY_TORRENT_ID, torrentID)
                    }
            }
    }
}


class CachePagerAdapter(fragment: Fragment, private val cache: CachedTorrent) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = cache.cachedAlternatives.size

    override fun createFragment(position: Int): Fragment {
        return TorrentCacheListFragment.newInstance(cache.cachedAlternatives[position])
    }
}