package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentCachePickerBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

private const val CACHE_LIST_KEY = "key_cache_list"

class TorrentCachePickerFragment : Fragment() {

    private lateinit var cache: CachedTorrent

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
        @JvmStatic
        fun newInstance(cache: CachedTorrent?) =
            TorrentCachePickerFragment().apply {
                if (cache != null)
                    arguments = Bundle().apply {
                        putParcelable(CACHE_LIST_KEY, cache)
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