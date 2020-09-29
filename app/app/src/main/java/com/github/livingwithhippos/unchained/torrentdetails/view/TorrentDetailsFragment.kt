package com.github.livingwithhippos.unchained.torrentdetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * It is capable of showing the details of a [TorrentItem] and updating it.
 */
@AndroidEntryPoint
class TorrentDetailsFragment : Fragment(), TorrentDetailsListener {

    private val viewModel: TorrentDetailsViewModel by viewModels()

    private val args: TorrentDetailsFragmentArgs by navArgs()

    // possible status are magnet_error, magnet_conversion, waiting_files_selection,
    // queued, downloading, downloaded, error, virus, compressing, uploading, dead
    val loadingStatusList = listOf(
        "magnet_conversion",
        "waiting_files_selection",
        "queued",
        "compressing",
        "uploading"
    )
    val endedStatusList = listOf("magnet_error", "downloaded", "error", "virus", "dead")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val torrentBinding = FragmentTorrentDetailsBinding.inflate(inflater, container, false)


        val statusTranslation = mapOf(
            "magnet_error" to resources.getString(R.string.magnet_error),
            "magnet_conversion" to resources.getString(R.string.magnet_conversion),
            "waiting_files_selection" to resources.getString(R.string.waiting_files_selection),
            "queued" to resources.getString(R.string.queued),
            "downloading" to resources.getString(R.string.downloading),
            "downloaded" to resources.getString(R.string.downloaded),
            "error" to resources.getString(R.string.error),
            "virus" to resources.getString(R.string.virus),
            "compressing" to resources.getString(R.string.compressing),
            "uploading" to resources.getString(R.string.uploading),
            "dead" to resources.getString(R.string.dead)
        )

        torrentBinding.loadingStatusList = loadingStatusList
        torrentBinding.statusTranslation = statusTranslation
        torrentBinding.listener = this

        viewModel.torrentLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                torrentBinding.torrent = it
                if (loadingStatusList.contains(it.status) || it.status == "downloading")
                    fetchTorrent()
            }
        })

        viewModel.fetchTorrentDetails(args.torrentID)

        viewModel.deletedTorrentLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled().let {
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.torrent_deleted)
                // if deleted go back
                activity?.onBackPressed()
            }
        })

        return torrentBinding.root
    }

    // fetch the torrent info every 2 seconds
    private fun fetchTorrent(delay: Long = 2000) {
        lifecycleScope.launch {
            delay(delay)
            viewModel.fetchTorrentDetails(args.torrentID)
        }
    }

    override fun onDownloadClick(links: List<String>) {
        val action =
            TorrentDetailsFragmentDirections.actionTorrentDetailsFragmentToNewDownloadDest(links.toTypedArray())
        findNavController().navigate(action)
    }

    override fun onDeleteClick(id: String) {
        viewModel.deleteTorrent(id)
    }
}

interface TorrentDetailsListener {
    fun onDownloadClick(links: List<String>)
    fun onDeleteClick(id: String)
}