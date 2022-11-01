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
import com.github.livingwithhippos.unchained.data.model.cache.CachedAlternative
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentProcessingBinding
import com.github.livingwithhippos.unchained.newdownload.view.TorrentProcessingFragment.Companion.POSITION_FILE_PICKER
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.newdownload.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.torrentfilepicker.view.TorrentFilePickerFragment
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * This fragments is shown after a user uploads a torrent or a magnet.
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
                    // todo: add a loop so this is repeated if it fails, instead of wasting the fragment
                }
                is TorrentEvent.TorrentInfo -> {
                    // torrent loaded
                    if (activityViewModel.getCurrentTorrentCachePick()?.first != content.item.id) {
                        // clear up old cache
                        activityViewModel.clearCurrentTorrentCachePick()
                    }
                    // check if we are already beyond file selection
                    if (!beforeSelectionStatusList.contains(content.item.status)) {
                        val action =
                            TorrentProcessingFragmentDirections.actionTorrentProcessingFragmentToTorrentDetailsDest(
                                item = content.item
                            )
                        findNavController().navigate(action)
                    } else {
                        binding.tvStatus.text = getString(R.string.checking_cache)
                        val hash = content.item.hash.lowercase()
                        //  Check if the activity already has the torrent cache, otherwise check it again
                        val currentCache: CachedTorrent? =
                            activityViewModel.cacheLiveData.value?.cachedTorrents?.firstOrNull { tor ->
                                tor.btih.equals(
                                    hash,
                                    ignoreCase = true
                                )
                            }
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
                }
                is TorrentEvent.CacheHit -> {
                    Timber.d("Found cache ${content.cache}")

                    binding.loadingLayout.visibility = View.INVISIBLE
                    binding.loadedLayout.visibility = View.VISIBLE

                    cachedTorrent = content.cache

                    if (content.cache.cachedAlternatives.isNotEmpty()) {
                        // cache found, enable tab swiping and clicking
                        binding.pickerPager.isUserInputEnabled = true

                        binding.pickerTabs.getTabAt(0)?.view?.isClickable = true
                        binding.pickerTabs.getTabAt(1)?.view?.isClickable = true
                    } else {
                        context?.showToast(R.string.cache_missing)
                    }
                }
                TorrentEvent.CacheMiss -> {
                    // fixme: here or above here got triggered but the views were still swipable
                    context?.showToast(R.string.cache_missing)

                    binding.loadingLayout.visibility = View.INVISIBLE
                    binding.loadedLayout.visibility = View.VISIBLE

                    Timber.d("Cached torrent not found")
                }
                is TorrentEvent.FilesSelected -> {
                    val action =
                        TorrentProcessingFragmentDirections.actionTorrentProcessingFragmentToTorrentDetailsDest(
                            item = content.torrent
                        )
                    findNavController().navigate(action)
                }
                TorrentEvent.DownloadAll -> {
                    binding.tvStatus.text = getString(R.string.selecting_all_files)
                    binding.tvLoadingTorrent.visibility = View.INVISIBLE
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.loadedLayout.visibility = View.INVISIBLE
                }
                is TorrentEvent.DownloadCache -> {
                    binding.tvStatus.text =
                        getString(R.string.selecting_picked_cache, content.files, content.position)
                    binding.tvLoadingTorrent.visibility = View.INVISIBLE
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.loadedLayout.visibility = View.INVISIBLE
                }
                TorrentEvent.DownloadedFileFailure -> {
                    binding.tvStatus.text = getString(R.string.error_loading_torrent)
                    binding.tvLoadingTorrent.visibility = View.INVISIBLE
                    binding.loadingCircle.isIndeterminate = false
                    binding.loadingCircle.progress = 100
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.loadedLayout.visibility = View.INVISIBLE

                }
                is TorrentEvent.DownloadedFileProgress -> {
                    binding.tvStatus.text = getString(R.string.downloading_torrent)
                    binding.loadingCircle.isIndeterminate = false
                    binding.loadingCircle.progress = content.progress

                }
                TorrentEvent.DownloadedFileSuccess -> {
                    // do nothing
                }
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        activityViewModel.clearCurrentTorrentCachePick()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tabLayout: TabLayout = view.findViewById(R.id.pickerTabs)
        val viewPager: ViewPager2 = view.findViewById(R.id.pickerPager)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            /**
             * disable clicking until the data and cache are loaded
             */
            tab.view.isClickable = false

            when (position) {
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

        // disable swiping until the data and cache are loaded
        binding.pickerPager.isUserInputEnabled = false

        binding.fabDownload.setOnClickListener {
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
                        downloadTorrentToCache(it)
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

        val pick = activityViewModel.getCurrentTorrentCachePick()
        if (pick == null)
            popup.menu.findItem(R.id.download_cache).isEnabled = false

        // todo:implement
        popup.menu.findItem(R.id.manual_pick).isEnabled = false

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.download_cache -> {
                    if (pick != null) {
                        val pickedCache: CachedAlternative? =
                            viewModel.getCache()?.cachedAlternatives?.getOrNull(pick.second)
                        if (pickedCache != null) {
                            viewModel.triggerTorrentEvent(
                                TorrentEvent.DownloadCache(
                                    pick.second + 1,
                                    pickedCache.cachedFiles.count()
                                )
                            )
                            val selectedFiles =
                                pickedCache.cachedFiles.joinToString(separator = ",") {
                                    it.id.toString()
                                }

                            viewModel.startSelectionLoop(selectedFiles)
                        } else {
                            Timber.e("No cache corresponding to index ${pick.first} found")
                        }

                    } else {
                        Timber.e("No cache pick found")
                    }
                }
                R.id.download_all -> {
                    viewModel.triggerTorrentEvent(TorrentEvent.DownloadAll)
                    viewModel.startSelectionLoop()
                }
                R.id.manual_pick -> {
                    // TODO()
                }
                else -> {
                    Timber.e("Unknown menu button pressed: $menuItem")
                }
            }

            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun downloadTorrentToCache(link: String) {
        val nameRegex = "/([^/]+\\.torrent)\$"
        val m: Matcher = Pattern.compile(nameRegex).matcher(link)
        val torrentName = if (m.find()) m.group(1) else null
        val cacheDir = context?.cacheDir
        if (!torrentName.isNullOrBlank() && cacheDir != null) {
            activityViewModel.downloadFileToCache(link, torrentName, cacheDir).observe(
                viewLifecycleOwner
            ) {
                when (it) {
                    is DownloadResult.End -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileSuccess)
                        loadCachedTorrent(cacheDir, it.fileName)
                    }
                    DownloadResult.Failure -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
                    }
                    is DownloadResult.Progress -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileProgress(it.percent))
                    }
                    DownloadResult.WrongURL -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
                    }
                }
            }
        }
    }


    private fun loadCachedTorrent(
        cacheDir: File,
        fileName: String
    ) {
        try {
            val cacheFile = File(cacheDir, fileName)
            cacheFile.inputStream().use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
            when (exception) {
                is java.io.FileNotFoundException -> {
                    Timber.e("Torrent conversion: file not found: ${exception.message}")
                }
                is IOException -> {
                    Timber.e("Torrent conversion: IOException error getting the file: ${exception.message}")
                }
                else -> {
                    Timber.e("Torrent conversion: Other error getting the file: ${exception.message}")
                }
            }
        }
    }

    companion object {
        const val POSITION_FILE_PICKER = 0
        const val POSITION_CACHE_PICKER = 1
    }
}


class TorrentFilePagerAdapter(
    fragment: Fragment,
    private val viewModel: TorrentProcessingViewModel
) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == POSITION_FILE_PICKER) {
            TorrentFilePickerFragment.newInstance(viewModel.getTorrentDetails())
        } else {
            TorrentCachePickerFragment.newInstance(viewModel.getCache(), viewModel.getTorrentID()!!)
        }
    }
}