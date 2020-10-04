package com.github.livingwithhippos.unchained.torrentdetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.observeOnce
import com.github.livingwithhippos.unchained.utilities.extension.showToast
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
    val loadingStatusList = listOf(
        "magnet_conversion",
        "waiting_files_selection",
        "queued",
        "compressing",
        "uploading"
    )

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

        torrentBinding.loadingStatusList = loadingStatusList
        torrentBinding.statusTranslation = statusTranslation
        torrentBinding.listener = this

        viewModel.torrentLiveData.observeOnce(viewLifecycleOwner, {
            activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_TORRENT)
        }, true)

        viewModel.torrentLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                torrentBinding.torrent = it
                if (loadingStatusList.contains(it.status) || it.status == "downloading")
                    fetchTorrent()
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
            it.getContentIfNotHandled()?.let {download ->
                val action =
                    TorrentDetailsFragmentDirections.actionTorrentDetailsToDownloadDetailsDest(download)
                findNavController().navigate(action)
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
        if (links.size>1)
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