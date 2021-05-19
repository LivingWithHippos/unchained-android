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
import com.github.livingwithhippos.unchained.data.model.*
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
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
    ): View {
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

        viewModel.torrentLiveData.observe(viewLifecycleOwner, EventObserver {
            it?.let { torrent ->
                torrentBinding.torrent = torrent
                if (loadingStatusList.contains(torrent.status))
                    fetchTorrent()
            }
        })

        viewModel.deletedTorrentLiveData.observe(viewLifecycleOwner, EventObserver {
            // todo: check returned value (it)
            activity?.baseContext?.showToast(R.string.torrent_deleted)
            // if deleted go back
            activity?.onBackPressed()
            activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_TORRENT)
        })

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteTorrent(args.torrentID)
        }

        viewModel.downloadLiveData.observe(viewLifecycleOwner, EventObserver {
            it?.let { download ->
                val action =
                    TorrentDetailsFragmentDirections.actionTorrentDetailsToDownloadDetailsDest(
                        download
                    )
                findNavController().navigate(action)
            }
        })

        viewModel.errorsLiveData.observe(viewLifecycleOwner, EventObserver {
            for (error in it) {
                when (error) {
                    is APIError -> {
                        context?.let { c ->
                            c.showToast(c.getApiErrorMessage(error.errorCode))
                        }
                    }
                    is EmptyBodyError -> {
                    }
                    is NetworkError -> {
                        context?.let { c ->
                            c.showToast(R.string.network_error)
                        }
                    }
                    is ApiConversionError -> {
                        context?.let { c ->
                            c.showToast(R.string.parsing_error)
                        }
                    }
                }
            }
        })

        viewModel.fetchTorrentDetails(args.torrentID)

        return torrentBinding.root
    }

    // fetch the torrent info every 2 seconds
    private fun fetchTorrent(delay: Long = 2000) {
        lifecycleScope.launch {
            delay(delay)
            viewModel.fetchTorrentDetails(args.torrentID)
        }
    }

    override fun onDownloadClick(item: TorrentItem) {
        if (item.links.size > 1) {
            val action = TorrentDetailsFragmentDirections.actionTorrentDetailsDestToTorrentListFragment(folder = null, torrent = item)
            findNavController().navigate(action)
        } else {
            viewModel.downloadTorrent()
        }
    }

    override fun onDeleteClick(id: String) {
        viewModel.deleteTorrent(id)
    }
}

interface TorrentDetailsListener {
    fun onDownloadClick(item: TorrentItem)
    fun onDeleteClick(id: String)
}