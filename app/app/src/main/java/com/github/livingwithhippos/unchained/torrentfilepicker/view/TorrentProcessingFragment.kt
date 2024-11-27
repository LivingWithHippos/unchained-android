package com.github.livingwithhippos.unchained.torrentfilepicker.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentProcessingBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem.Companion.TYPE_FOLDER
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import timber.log.Timber

/** This fragments is shown after a user uploads a torrent or a magnet. */
@AndroidEntryPoint
class TorrentProcessingFragment : UnchainedFragment() {

    private val args: TorrentProcessingFragmentArgs by navArgs()

    // https://developer.android.com/training/dependency-injection/hilt-jetpack#viewmodel-navigation
    private val viewModel: TorrentProcessingViewModel by
        hiltNavGraphViewModels(R.id.navigation_lists)

    /** Save the torrent/magnet has when loaded */
    private var torrentHash: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentTorrentProcessingBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.torrentLiveData.observe(viewLifecycleOwner) {
            when (val content = it.getContentIfNotHandled()) {
                is TorrentEvent.Uploaded -> {
                    binding.tvStatus.text = getString(R.string.loading_torrent)
                    // get torrent info
                    viewModel.fetchTorrentDetails(content.torrent.id)
                    // todo: add a loop so this is repeated if it fails, instead of wasting the
                    // fragment
                }
                is TorrentEvent.TorrentInfo -> {
                    if (
                        content.item.files?.size == 1 &&
                            "waiting_files_selection".equals(content.item.status, ignoreCase = true)
                    ) {
                        // todo: make this configurable in settings
                        context?.showToast(R.string.single_torrent_file_available)
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadAll)
                        viewModel.startSelectionLoop()
                    } else {
                        torrentHash = content.item.hash
                        // torrent loaded
                        // check if we are already beyond file selection
                        if (!beforeSelectionStatusList.contains(content.item.status)) {
                            val action =
                                TorrentProcessingFragmentDirections
                                    .actionTorrentProcessingFragmentToTorrentDetailsDest(
                                        item = content.item
                                    )
                            findNavController().navigate(action)
                        } else {
                            binding.loadingLayout.visibility = View.INVISIBLE
                            binding.loadedLayout.visibility = View.VISIBLE
                        }
                    }
                }
                is TorrentEvent.FilesSelected -> {
                    activityViewModel.setListState(ListState.UpdateTorrent)
                    val action =
                        TorrentProcessingFragmentDirections
                            .actionTorrentProcessingFragmentToTorrentDetailsDest(
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
                is TorrentEvent.DownloadSelection -> {
                    binding.tvStatus.text =
                        getString(R.string.selecting_picked_files, content.filesNumber)
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
                else -> {
                    Timber.d("Found unknown torrentLiveData event $content")
                    // reloaded fragment, close?
                }
            }
        }

        viewModel.networkExceptionLiveData.observe(viewLifecycleOwner) {
            when (val response = it.getContentIfNotHandled()) {
                null -> {}
                is APIError -> {
                    Timber.e("API error: ${response.errorCode}")
                    if (response.errorCode == 8) {
                        if (
                            activityViewModel.getAuthenticationMachineState()
                                is FSMAuthenticationState.AuthenticatedOpenToken
                        )
                            activityViewModel.transitionAuthenticationMachine(
                                FSMAuthenticationEvent.OnExpiredOpenToken
                            )
                        context?.showToast(R.string.refreshing_token)
                    } else {
                        context?.let { c -> c.showToast(c.getApiErrorMessage(response.errorCode)) }
                    }
                    findNavController().popBackStack()
                }
                is NetworkError -> {
                    context?.showToast(R.string.network_error)
                    findNavController().popBackStack()
                }
                is ApiConversionError -> {
                    context?.showToast(R.string.unknown_error)
                    findNavController().popBackStack()
                }
                is EmptyBodyError -> {
                    context?.showToast(R.string.network_error)
                    findNavController().popBackStack()
                }
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setup(binding: FragmentTorrentProcessingBinding) {

        binding.fabDownload.setOnClickListener { showMenu(it, R.menu.download_mode_picker) }

        if (args.torrentID != null) {
            // we are loading an already available torrent
            args.torrentID?.let { viewModel.fetchTorrentDetails(it) }
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
            throw IllegalArgumentException(
                "No torrent link or torrent id was passed to TorrentProcessingFragment"
            )
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        val lastSelection: Node<TorrentFileItem>? = viewModel.structureLiveData.value?.peekContent()
        if (lastSelection == null) popup.menu.findItem(R.id.manual_pick).isEnabled = false

        if (torrentHash == null) {
            popup.menu.findItem(R.id.copy_magnet).isVisible = false
            popup.menu.findItem(R.id.share_magnet).isVisible = false
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.download_all -> {
                    viewModel.triggerTorrentEvent(TorrentEvent.DownloadAll)
                    viewModel.startSelectionLoop()
                }
                R.id.manual_pick -> {

                    if (lastSelection != null) {
                        var counter = 0
                        val selectedFiles = StringBuffer()
                        Node.traverseBreadthFirst(lastSelection) {
                            if (it.selected && it.id != TYPE_FOLDER) {
                                selectedFiles.append(it.id)
                                selectedFiles.append(",")
                                counter++
                            }
                        }

                        if (counter == 0) {
                            context?.showToast(R.string.select_one_item)
                        } else {
                            if (selectedFiles.last() == ","[0])
                                selectedFiles.deleteCharAt(selectedFiles.lastIndex)

                            viewModel.triggerTorrentEvent(TorrentEvent.DownloadSelection(counter))
                            viewModel.startSelectionLoop(selectedFiles.toString())
                        }
                    } else {
                        Timber.e("Last files selection should not have been null")
                    }
                }
                R.id.copy_magnet -> {
                    copyToClipboard("Real-Debrid Magnet", "magnet:?xt=urn:btih:$torrentHash")
                    context?.showToast(R.string.link_copied)
                }
                R.id.share_magnet -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "magnet:?xt=urn:btih:$torrentHash")
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
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
                        viewModel.triggerTorrentEvent(
                            TorrentEvent.DownloadedFileProgress(it.percent)
                        )
                    }
                    DownloadResult.WrongURL -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
                    }
                }
            }
        }
    }

    private fun loadCachedTorrent(cacheDir: File, fileName: String) {
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
                    Timber.e(
                        "Torrent conversion: IOException error getting the file: ${exception.message}"
                    )
                }
                else -> {
                    Timber.e(
                        "Torrent conversion: Other error getting the file: ${exception.message}"
                    )
                }
            }
        }
    }
}
