package com.github.livingwithhippos.unchained.torrentdetails.view

import android.content.Intent
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
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * A simple [Fragment] subclass. It is capable of showing the details of a [TorrentItem] and
 * updating it.
 */
@AndroidEntryPoint
class TorrentDetailsFragment : UnchainedFragment(), TorrentContentListener {

    private val viewModel: TorrentDetailsViewModel by viewModels()

    private val args: TorrentDetailsFragmentArgs by navArgs()

    private var _binding: FragmentTorrentDetailsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTorrentDetailsBinding.inflate(inflater, container, false)

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
                                TorrentDetailsFragmentDirections
                                    .actionTorrentDetailsDestToTorrentProcessingFragment(
                                        link = link
                                    )
                            findNavController().navigate(action)
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        val statusTranslation =
            mapOf(
                "magnet_error" to getString(R.string.magnet_error),
                "magnet_conversion" to getString(R.string.magnet_conversion),
                "waiting_files_selection" to getString(R.string.waiting_files_selection),
                "queued" to getString(R.string.queued),
                "downloading" to getString(R.string.downloading),
                "downloaded" to getString(R.string.downloaded),
                "ready" to getString(R.string.ready),
                "error" to getString(R.string.error),
                "virus" to getString(R.string.virus),
                "compressing" to getString(R.string.compressing),
                "uploading" to getString(R.string.uploading),
                "dead" to getString(R.string.dead),
            )

        binding.tvStatus.text = statusTranslation[args.item.status] ?: args.item.status
        binding.fabShareMagnet.setOnClickListener { onShareMagnetClick() }
        binding.fabCopyMagnet.setOnClickListener { onCopyMagnetClick() }
        binding.bDownload.setOnClickListener { onDownloadClick(args.item) }

        val adapter = TorrentContentFilesAdapter()
        binding.rvFileList.adapter = adapter

        viewModel.torrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { torrent ->
                    val selectedFiles: Int =
                        torrent.files?.count { file -> file.selected == 1 } ?: 0
                    binding.tvSelectedFilesNumber.text = selectedFiles.toString()

                    binding.tvTotalFiles.text = (torrent.files?.count() ?: 0).toString()
                    binding.tvName.text = torrent.filename
                    binding.tvProgressPercent.text =
                        getString(R.string.percent_format, torrent.progress)
                    binding.tvProgress.text = getString(R.string.percent_format, torrent.progress)
                    if (torrent.progress >= 0 && torrent.progress < 100) {
                        binding.tvProgress.visibility = View.VISIBLE
                    } else {
                        binding.tvProgress.visibility = View.GONE
                    }
                    try {
                        val torrentSpeed = torrent.speed
                        if (torrentSpeed == null) {
                            binding.tvSpeed.text = ""
                        } else {
                            binding.tvSpeed.text =
                                when (torrent.speed.toString().length) {
                                    in 0..3 -> getString(R.string.speed_format_b, torrentSpeed)
                                    in 4..6 ->
                                        getString(R.string.speed_format_kb, torrentSpeed / 1000.0)
                                    in 7..15 ->
                                        getString(
                                            R.string.speed_format_mb,
                                            torrentSpeed / 1000000.0,
                                        )
                                    else -> getString(R.string.speed_error)
                                }
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error formatting speed from '${torrent.speed}'")
                        binding.tvSpeed.text = ""
                    }
                    if (torrent.seeders == null) {
                        binding.tvSeeders.visibility = View.GONE
                    } else {
                        binding.tvSeeders.text =
                            resources.getQuantityString(
                                R.plurals.seeders_format,
                                torrent.seeders,
                                torrent.seeders,
                            )
                        binding.tvSeeders.visibility = View.VISIBLE
                    }
                    binding.pbDownload.setProgressCompat(torrent.progress.toInt(), true)
                    if (
                        torrent.status.equals("downloaded", true) ||
                            torrent.status.equals("ready", true)
                    ) {
                        binding.bDownload.visibility = View.VISIBLE
                    } else {
                        binding.bDownload.visibility = View.GONE
                    }
                    context?.let { ctx ->
                        torrent.originalBytes?.let { size ->
                            binding.tvFileSize.text = getFileSizeString(ctx, size)
                        }
                        binding.tvSelectedSize.text = getFileSizeString(ctx, torrent.bytes)
                    }
                    binding.cvDownloadDetails.visibility =
                        if (torrent.status.equals("downloading", true)) View.VISIBLE else View.GONE

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
                                if (!skippedFirst) skippedFirst = true else filesList.add(item)
                            }
                            adapter.submitList(filesList)
                            binding.cvSelectedTorrentFiles.visibility = View.VISIBLE
                        }
                    }
                }
            },
        )

        viewModel.deletedTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                // todo: check returned value (it)
                context?.showToast(R.string.torrent_removed)
                activityViewModel.setListState(ListState.UpdateTorrent)
                // if deleted go back
                findNavController().popBackStack()
            },
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            if (bundle.getBoolean("deleteConfirmation")) viewModel.deleteTorrent(args.item.id)
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
            },
        )

        viewModel.errorsLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                for (error in it) {
                    when (error) {
                        is APIError -> {
                            context?.let { c -> c.showToast(c.getApiErrorMessage(error.errorCode)) }
                        }

                        is EmptyBodyError -> {}
                        is NetworkError -> {
                            context?.showToast(R.string.network_error)
                        }

                        is ApiConversionError -> {
                            context?.showToast(R.string.parsing_error)
                        }
                    }
                }
            },
        )

        // maybe load and save the latest retrieved one in the view-model?
        if (loadingStatusList.contains(args.item.status)) viewModel.pollTorrentStatus(args.item.id)
        else {
            viewModel.getFullTorrentInfo(args.item.id)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onDownloadClick(item: TorrentItem) {
        if (item.links.size > 1) {
            val action =
                TorrentDetailsFragmentDirections.actionTorrentDetailsToTorrentFolder(
                    folder = null,
                    torrent = item,
                    linkList = null,
                )
            findNavController().navigate(action)
        } else {
            viewModel.downloadTorrent(item)
        }
    }

    fun onDeleteClick(id: String) {
        viewModel.deleteTorrent(id)
    }

    fun onShareMagnetClick() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "magnet:?xt=urn:btih:${args.item.hash}")
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
    }

    fun onCopyMagnetClick() {
        copyToClipboard("Real-Debrid Magnet", "magnet:?xt=urn:btih:${args.item.hash}")
        context?.showToast(R.string.link_copied)
    }

    override fun onSelectedFile(item: TorrentFileItem) {
        // not used here
    }

    override fun onSelectedFolder(item: TorrentFileItem) {
        // not used here
    }
}
