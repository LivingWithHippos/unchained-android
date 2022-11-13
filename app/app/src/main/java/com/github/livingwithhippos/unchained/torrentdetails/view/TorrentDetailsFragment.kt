package com.github.livingwithhippos.unchained.torrentdetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentFilesAdapter
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [Fragment] subclass.
 * It is capable of showing the details of a [TorrentItem] and updating it.
 */
@AndroidEntryPoint
class TorrentDetailsFragment : UnchainedFragment(), TorrentDetailsListener {

    private val viewModel: TorrentDetailsViewModel by viewModels()

    private val args: TorrentDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val torrentBinding = FragmentTorrentDetailsBinding.inflate(inflater, container, false)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // Add menu items here
                    menuInflater.inflate(R.menu.torrent_details_bar, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.delete -> {
                            val dialog = DeleteDialogFragment()
                            dialog.show(parentFragmentManager, "DeleteDialogFragment")
                            true
                        }
                        R.id.reselect -> {
                            val link = "magnet:?xt=urn:btih:${args.item.hash}"
                            val action =
                                TorrentDetailsFragmentDirections.actionTorrentDetailsDestToTorrentProcessingFragment(
                                    link = link
                                )
                            findNavController().navigate(action)
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner, Lifecycle.State.RESUMED
        )

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

        val adapter = TorrentContentFilesAdapter()
        torrentBinding.rvFileList.adapter = adapter

        viewModel.torrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { torrent ->
                    torrentBinding.torrent = torrent
                    val selectedFiles: Int =
                        torrent.files?.count { file -> file.selected == 1 } ?: 0
                    torrentBinding.tvSelectedFilesNumber.text = selectedFiles.toString()
                    torrentBinding.tvTotalFiles.text = (torrent.files?.count() ?: 0).toString()

                    // Data should not change between updates so we should just populate it once
                    if (adapter.itemCount == 0) {
                        val torrentStructure: Node<TorrentFileItem> =
                            getFilesNodes(torrent, selectedOnly = true)
                        // show list only if it's populated enough
                        if (torrentStructure.children.size > 0) {
                            val filesList = mutableListOf<TorrentFileItem>()
                            var skippedFirst = false
                            Node.traverseDepthFirst(torrentStructure) { item ->
                                // avoid root item "/"
                                if (!skippedFirst)
                                    skippedFirst = true
                                else
                                    filesList.add(item)
                            }
                            adapter.submitList(filesList)
                            torrentBinding.cvSelectedTorrentFiles.visibility = View.VISIBLE
                        }
                    }
                }
            }
        )

        viewModel.deletedTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.torrent_removed)
                // if deleted go back
                activity?.onBackPressed()
                activityViewModel.setListState(ListState.UpdateTorrent)
            }
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteTorrent(args.item.id)
        }

        viewModel.downloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { download ->
                    val action =
                        TorrentDetailsFragmentDirections.actionTorrentDetailsToDownloadDetailsDest(
                            download
                        )
                    findNavController().navigate(action)
                }
            }
        )

        viewModel.errorsLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
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
                            context?.showToast(R.string.network_error)
                        }
                        is ApiConversionError -> {
                            context?.showToast(R.string.parsing_error)
                        }
                    }
                }
            }
        )

        torrentBinding.torrent = args.item

        // maybe load and save the latest retrieved one in the view-model?
        if (loadingStatusList.contains(args.item.status))
            viewModel.pollTorrentStatus(args.item.id)
        else {
            viewModel.getFullTorrentInfo(args.item.id)
        }

        return torrentBinding.root
    }

    override fun onDownloadClick(item: TorrentItem) {
        if (item.links.size > 1) {
            val action = TorrentDetailsFragmentDirections.actionTorrentDetailsToTorrentFolder(
                folder = null,
                torrent = item,
                linkList = null
            )
            findNavController().navigate(action)
        } else {
            viewModel.downloadTorrent(item)
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
