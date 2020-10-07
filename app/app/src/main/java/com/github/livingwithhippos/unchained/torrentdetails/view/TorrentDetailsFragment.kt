package com.github.livingwithhippos.unchained.torrentdetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedApplication.Companion.CHANNEL_ID
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * It is capable of showing the details of a [TorrentItem] and updating it.
 */
@AndroidEntryPoint
class TorrentDetailsFragment : UnchainedFragment(), TorrentDetailsListener {

    private val viewModel: TorrentDetailsViewModel by viewModels()

    private val args: TorrentDetailsFragmentArgs by navArgs()

    // possible status are magnet_error, magnet_conversion, waiting_files_selection,
    // queued, downloading, downloaded, error, virus, compressing, uploading, dead
    private val loadingStatusList = listOf(
        "magnet_conversion",
        "waiting_files_selection",
        "queued",
        "compressing",
        "uploading"
    )

    private lateinit var builder: NotificationCompat.Builder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val torrentBinding = FragmentTorrentDetailsBinding.inflate(inflater, container, false)


        val statusTranslation = mapOf(
            "magnet_error" to getString(R.string.magnet_error),
            "magnet_conversion" to getString(R.string.magnet_conversion),
            "waiting_files_selection" to getString(R.string.waiting_files_selection),
            "queued" to getString(R.string.queued),
            "downloading" to getString(R.string.downloading),
            "downloaded" to getString(R.string.downloaded),
            "error" to getString(R.string.error),
            "virus" to getString(R.string.virus),
            "compressing" to getString(R.string.compressing),
            "uploading" to getString(R.string.uploading),
            "dead" to getString(R.string.dead)
        )

        // the priority here is needed before Android 7. On Android >= 8, the channel priority is used (see UnchainedApp)
        // since it is not possible to change the priority of a channel, we manually vibrate the phone once when the download is terminated.
        builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID).apply {
            setContentTitle(getString(R.string.torrent_in_progress))
            setContentText(getString(R.string.loading))
            // fixme: using the logo from R.mipmap.icon_launcher gives a painted icon, this one does not. Maybe because this is missing a background?
            setSmallIcon(R.drawable.ic_logo_no_bg)
            priority = NotificationCompat.PRIORITY_LOW
        }

        torrentBinding.loadingStatusList = loadingStatusList
        torrentBinding.statusTranslation = statusTranslation
        torrentBinding.listener = this

        viewModel.torrentLiveData.observeOnce(viewLifecycleOwner, {
            if (it != null) {
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_TORRENT)
                updateNotificationText(it.id, it.filename)
            }
        }, true)

        var wasDownloading = false
        viewModel.torrentLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                torrentBinding.torrent = it
                when (it.status) {
                    // before the download starts
                    "magnet_conversion", "waiting_files_selection", "queued",  -> {
                        fetchTorrent()
                        updateNotificationStatus(it.id, statusTranslation[it.status])
                        wasDownloading = true
                    }
                    // download is in progress
                    "downloading" -> {
                        fetchTorrent()
                        updateNotificationProgress(it.id, it.filename, it.progress)
                        wasDownloading = true
                    }
                    // after the download has finished
                    "compressing", "uploading" -> {
                        fetchTorrent()
                        updateNotificationStatus(it.id, statusTranslation[it.status], true)
                        wasDownloading = true
                    }
                    // these won't require anymore updates
                    "downloaded", "error", "virus", "dead" -> {
                        if(wasDownloading) {
                            // gives a last message with a sound/vibration this time
                            completeNotification(it.id, it.filename, statusTranslation[it.status])
                            // shouldn't be needed
                            wasDownloading = false
                        }

                    }
                }
            }
        })

        viewModel.fetchTorrentDetails(args.torrentID)

        viewModel.deletedTorrentLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {
                //fixme: list does not update
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_TORRENT)
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.torrent_deleted)
                // if deleted go back
                activity?.onBackPressed()
            }
        })

        setFragmentResultListener("deleteActionKey") { key, bundle ->
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteTorrent(args.torrentID)
        }

        viewModel.downloadLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { download ->
                val action =
                    TorrentDetailsFragmentDirections.actionTorrentDetailsToDownloadDetailsDest(
                        download
                    )
                findNavController().navigate(action)
            }
        })

        return torrentBinding.root
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.torrent_details_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                val dialog = DeleteDialogFragment()
                dialog.show(parentFragmentManager, "DeleteDialogFragment")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateNotificationText(id: String, fileName: String) {
        context?.let {

            NotificationManagerCompat.from(it).apply {
                builder.setContentText(fileName)
                notify(id.hashCode(), builder.build())
            }
        }
    }

    private fun updateNotificationProgress(id: String, fileName: String, currentProgress: Int) {
        context?.let {

            NotificationManagerCompat.from(it).apply {
                builder.setProgress(100, currentProgress, false)
                    .setContentTitle(getString(R.string.torrent_in_progress_format, currentProgress))
                    .setContentText(fileName)
                notify(id.hashCode(), builder.build())
            }
        }
    }

    private fun updateNotificationStatus(id: String, status: String?, removeStatusBar: Boolean = false) {
        context?.let {
            val currentStatus = status ?: getString(R.string.status_unknown)
            NotificationManagerCompat.from(it).apply {
                builder.setContentTitle(currentStatus)
                if (removeStatusBar)
                    builder.setProgress(0, 0, false)
                notify(id.hashCode(), builder.build())
            }
        }
    }

    private fun completeNotification(id: String, fileName: String, status: String?){
        context?.let {
            val currentStatus = status ?: getString(R.string.status_unknown)

            NotificationManagerCompat.from(it).apply {
                builder.setContentTitle(currentStatus)
                    // if the file is already downloaded the second row will not be set elsewhere
                    .setContentText(fileName)
                    // remove the progressbar if present
                    .setProgress(0, 0, false)
                notify(id.hashCode(), builder.build())
            }

            context?.vibrate()
        }
    }

    // fetches the torrent info every 2 seconds
    //todo: move fetchTorrent to ViewModel automatizing there
    private fun fetchTorrent(delay: Long = 2000) {
        lifecycleScope.launch {
            delay(delay)
            viewModel.fetchTorrentDetails(args.torrentID)
        }
    }

    override fun onDownloadClick(links: List<String>) {
        if (links.size > 1)
            context?.showToast(R.string.multiple_links_warning)

        viewModel.downloadTorrent()
    }

    override fun onDeleteClick(id: String) {
        viewModel.deleteTorrent(id)
    }
}

interface TorrentDetailsListener {
    fun onDownloadClick(links: List<String>)
    fun onDeleteClick(id: String)
}