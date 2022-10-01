package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentProcessingBinding
import com.github.livingwithhippos.unchained.newdownload.model.CacheFileAdapter
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
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

    // todo: move to viewmodel
    private var cacheIndex = 0
    private var cachedTorrent: CachedTorrent? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentTorrentProcessingBinding.inflate(inflater, container, false)

        setup(binding, args.link)

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
                    binding.tvStatus.text = getString(R.string.checking_cache)
                    val hash = content.item.hash.lowercase()
                    //  Check current caching
                    val currentCache = activityViewModel.cacheLiveData.value
                    if (currentCache != null && currentCache.cachedTorrents.map { tor -> tor.btih.lowercase() }
                            .contains(hash)) {
                        // todo: cached! go to next step!
                    } else {
                        // check this torrent cache again
                        viewModel.checkTorrentCache(hash)
                    }
                }
                is TorrentEvent.Availability -> {
                    Timber.d("Found cache ${content.cache}")
                    binding.tvStatus.visibility = View.INVISIBLE
                    binding.progressBar.visibility = View.INVISIBLE

                    cachedTorrent = content.cache.cachedTorrents.first()
                    // found my torrent, let the user pick
                    setupCacheList(binding, cachedTorrent!!)
                }
                TorrentEvent.CacheMiss -> {
                    Timber.d("Cached torrent not found")
                    binding.tvStatus.visibility = View.INVISIBLE
                    binding.progressBar.visibility = View.INVISIBLE

                    // let the user pick the files or download all
                    /// todo: show no cache indicator and hide cache stuff
                    binding.bNextCache.isEnabled = false
                    binding.bPreviousCache.isEnabled = false
                }
            }
        }

        return binding.root
    }

    private fun setupCacheList(binding: FragmentTorrentProcessingBinding, cache: CachedTorrent) {
        // check null or empty cache
        if (cache.cachedAlternatives.isEmpty()) {
            // todo: hide cache views
        } else {
            if (cache.cachedAlternatives.size == 1) {
                // single cache, disable buttons
                binding.bNextCache.isEnabled = false
                binding.bPreviousCache.isEnabled = false
            }
            val firstCache = cache.cachedAlternatives.first().cachedFiles
            (binding.rvCacheList.adapter as CacheFileAdapter).submitList(firstCache)
        }
    }

    private fun setup(binding: FragmentTorrentProcessingBinding, link: String) {

        // todo: do this only the first time
        val listsAdapter = CacheFileAdapter()
        binding.rvCacheList.adapter = listsAdapter

        binding.bPreviousCache.setOnClickListener {
            val cacheSize = cachedTorrent?.cachedAlternatives?.size ?: 0
            if (cacheSize == 0) {
                // button should be hidden
            } else {
                when (cacheIndex) {
                    0 -> {
                        cacheIndex = cacheSize - 1
                        viewModel.setCacheIndex(cacheIndex)
                        (binding.rvCacheList.adapter as CacheFileAdapter).submitList(cachedTorrent!!.cachedAlternatives[cacheIndex].cachedFiles)
                    }
                    else -> {
                        cacheIndex -= 1
                        viewModel.setCacheIndex(cacheIndex)
                        (binding.rvCacheList.adapter as CacheFileAdapter).submitList(cachedTorrent!!.cachedAlternatives[cacheIndex].cachedFiles)
                    }
                }
            }
        }

        binding.bNextCache.setOnClickListener {
            val cacheSize = cachedTorrent?.cachedAlternatives?.size ?: 0
            if (cacheSize == 0) {
                // button should be hidden
            } else {
                when (cacheIndex) {
                    cacheSize - 1 -> {
                        cacheIndex = 0
                        viewModel.setCacheIndex(cacheIndex)
                        (binding.rvCacheList.adapter as CacheFileAdapter).submitList(cachedTorrent!!.cachedAlternatives[cacheIndex].cachedFiles)
                    }
                    else -> {
                        cacheIndex += 1
                        viewModel.setCacheIndex(cacheIndex)
                        (binding.rvCacheList.adapter as CacheFileAdapter).submitList(cachedTorrent!!.cachedAlternatives[cacheIndex].cachedFiles)
                    }
                }
            }
        }

        binding.fabDownload.setOnClickListener {
            showMenu(it, R.menu.download_mode_picker)
        }

        when {
            link.isTorrent() -> {
                Timber.d("Found torrent $link")
            }
            link.isMagnet() -> {
                Timber.d("Found magnet $link")
                viewModel.fetchAddedMagnet(link)
            }
            else -> {
                Timber.e("Torrent processing link not recognized")
            }
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                 R.id.download_cache -> {
                 }
                 R.id.download_all -> {}
                 R.id.manual_pick -> {}
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
}
