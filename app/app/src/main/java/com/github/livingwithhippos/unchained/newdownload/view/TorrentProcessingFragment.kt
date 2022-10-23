package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.data.model.cache.InstantAvailability
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentProcessingBinding
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragmentDirections
import com.github.livingwithhippos.unchained.newdownload.view.TorrentProcessingFragment.Companion.POSITION_CACHE_PICKER
import com.github.livingwithhippos.unchained.newdownload.view.TorrentProcessingFragment.Companion.POSITION_FILE_PICKER
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * This fragments is shown after a user uploads a torrent or a magnet.
 * 1. Show uploading/loading status
 * 2. Check caching
 * 3. Let the user pick a cached version
 * 4. Let the user download all files or select which files
 * 5. Apply the modifications and send the user to the torrent details fragment
 * todo: the torrent details fragment needs to let the user do 3/4 if the torrent is still in "select files" mode
 */
@AndroidEntryPoint
class TorrentProcessingFragment : UnchainedFragment() {

    private val args: TorrentProcessingFragmentArgs by navArgs()
    private val viewModel: TorrentProcessingViewModel by viewModels()

    private var cachedTorrent: CachedTorrent? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentProcessingBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.torrentLiveData.observe(viewLifecycleOwner) {
            when (val content = it.getContentIfNotHandled()) {
                null -> {
                    // reloaded fragment, close?
                }
                is TorrentEvent.Uploaded -> {
                    binding.tvStatus.text = getString(R.string.loading_torrent)
                    // get torrent info
                    viewModel.fetchTorrentDetails(content.torrent.id)
                }
                is TorrentEvent.Updated -> {
                    // torrent loaded, enable file picking or wait for cache?

                    binding.tvStatus.text = getString(R.string.checking_cache)
                    val hash = content.item.hash.lowercase()
                    //  Check if the activity already has the torrent cache, otherwise check it again
                    val currentCache: CachedTorrent? = activityViewModel.cacheLiveData.value?.cachedTorrents?.firstOrNull { tor -> tor.btih.equals(hash, ignoreCase = true) }
                    if (
                        currentCache != null
                    ) {
                        // trigger cache hit without checking it
                        viewModel.triggerCacheResult(currentCache)
                    } else {
                        // check this torrent cache again
                        viewModel.checkTorrentCache(hash)
                    }
                }
                is TorrentEvent.CacheHit -> {
                    Timber.d("Found cache ${content.cache}")

                    binding.loadingLayout.visibility = View.INVISIBLE
                    binding.loadedLayout.visibility = View.VISIBLE

                    cachedTorrent = content.cache

                    if (content.cache.cachedAlternatives.isNotEmpty()) {
                        // cache found, enable tab swiping
                        binding.pickerPager.isUserInputEnabled = true
                    } else {
                        context?.showToast(R.string.cache_missing)
                    }
                }
                TorrentEvent.CacheMiss -> {
                    context?.showToast(R.string.cache_missing)

                    binding.loadingLayout.visibility = View.INVISIBLE
                    binding.loadedLayout.visibility = View.VISIBLE

                    Timber.d("Cached torrent not found")
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout: TabLayout = view.findViewById(R.id.pickerTabs)
        val viewPager: ViewPager2 = view.findViewById(R.id.pickerPager)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position) {
                POSITION_FILE_PICKER -> {
                    tab.text = getString(R.string.select_files)
                }
                POSITION_CACHE_PICKER -> {
                    tab.text = getString(R.string.pick_cache)
                }
            }
        }.attach()

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setup(binding: FragmentTorrentProcessingBinding) {

        binding.pickerPager.isUserInputEnabled = false

        binding.fabDownload.setOnClickListener {
            /**
             * todo: load based on status, maybe also on active tab
             * - loading info: download all
             * - loading cache or no cache: download all, download selected
             * - cache available: download all, download selected, download cache
             */
            showMenu(it, R.menu.download_mode_picker)
        }

        if (args.torrentID != null) {
            // we are loading an already available torrent
            args.torrentID?.let {
                viewModel.fetchTorrentDetails(it)
            }
        } else if (args.link != null) {
            // we are loading a new torrent
            args.link?.let {
                when {
                    it.isTorrent() -> {
                        Timber.d("Found torrent $it")
                    }
                    args.link.isMagnet() -> {
                        Timber.d("Found magnet $it")
                        viewModel.fetchAddedMagnet(it)
                    }
                    else -> {
                        Timber.e("Torrent processing link not recognized: $it")
                    }
                }
            }
        } else {
            throw IllegalArgumentException("No torrent link or torrent id was passed to TorrentProcessingFragment")
        }


        val adapter = TorrentFilePagerAdapter(this, viewModel)
        binding.pickerPager.adapter = adapter
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.download_cache -> {
                    // TODO()
                }
                R.id.download_all -> {
                    val  torrent: TorrentItem? = viewModel.getTorrentDetails()
                    if (torrent == null) {
                        Timber.e("Error loading torrent details in torrent processing fragment")
                    } else {
                        val action = TorrentProcessingFragmentDirections.actionTorrentProcessingFragmentToTorrentDetailsDest(
                            item = torrent
                        )
                        findNavController().navigate(action)
                    }
                }
                R.id.manual_pick -> {

                }
                else -> {}
            }

            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    companion object {
        const val POSITION_FILE_PICKER = 0
        const val POSITION_CACHE_PICKER = 1
    }
}


class TorrentFilePagerAdapter(fragment: Fragment, private val viewModel: TorrentProcessingViewModel) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == POSITION_FILE_PICKER) {
            TorrentFilePickerFragment.newInstance(viewModel.getTorrentDetails())
        } else  {
            TorrentCachePickerFragment.newInstance(viewModel.getCache())
        }
    }
}