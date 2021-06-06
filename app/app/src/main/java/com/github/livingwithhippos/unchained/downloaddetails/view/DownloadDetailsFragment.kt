package com.github.livingwithhippos.unchained.downloaddetails.view

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.Alternative
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.downloaddetails.model.AlternativeDownloadAdapter
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.RD_STREAMING_URL
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of showing the details of a [DownloadItem]
 */
@AndroidEntryPoint
class DownloadDetailsFragment : UnchainedFragment(), DownloadDetailsListener {

    private val viewModel: DownloadDetailsViewModel by viewModels()

    private val args: DownloadDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        detailsBinding.details = args.details
        detailsBinding.listener = this
        detailsBinding.yatseInstalled = isYatseInstalled()

        // set up streams and alternative (e.g. for youtube) links list
        val alternativeAdapter = AlternativeDownloadAdapter(this)
        detailsBinding.rvAlternativeList.adapter = alternativeAdapter

        if (!args.details.alternative.isNullOrEmpty()) {
            alternativeAdapter.submitList(args.details.alternative)
        }

        viewModel.streamLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                detailsBinding.stream = it

                val streams = mutableListOf<Alternative>()
                // parameter mimetype gets shown as the name and "streaming" as title in the list, the other params don't matter
                streams.add(
                    Alternative(
                        "h264WebM",
                        "h264WebM",
                        it.h264WebM.link,
                        getString(R.string.h264_webm),
                        getString(R.string.streaming)
                    )
                )
                streams.add(
                    Alternative(
                        "liveMP4",
                        "liveMP4",
                        it.liveMP4.link,
                        getString(R.string.liveMP4),
                        getString(R.string.streaming)
                    )
                )
                streams.add(
                    Alternative(
                        "apple",
                        "apple",
                        it.apple.link,
                        getString(R.string.apple),
                        getString(R.string.streaming)
                    )
                )
                streams.add(
                    Alternative(
                        "dash",
                        "dash",
                        it.dash.link,
                        getString(R.string.dash),
                        getString(R.string.streaming)
                    )
                )

                if (!args.details.alternative.isNullOrEmpty())
                    streams.addAll(args.details.alternative!!)

                alternativeAdapter.submitList(streams)
            }
        })

        viewModel.deletedDownloadLiveData.observe(viewLifecycleOwner, EventObserver {
            // todo: check returned value (it)
            activity?.baseContext?.showToast(R.string.download_removed)
            // if deleted go back
            activity?.onBackPressed()
            activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_DOWNLOAD)
        })

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            // the delete operation is observed from the viewModel
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteDownload(args.details.id)
        }

        return detailsBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_details_bar, menu)
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

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        context?.showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        openExternalWebPage(url)
    }

    override fun onOpenWith(url: String) {

        val yatseIntent = Intent().apply {
            action = "org.leetzone.android.yatsewidget.ACTION_MEDIA_PLAYURI"
            component = ComponentName(
                "org.leetzone.android.yatsewidgetfree",
                "org.leetzone.android.yatsewidget.service.core.YatseCommandService"
            )
            putExtra("org.leetzone.android.yatsewidget.EXTRA_STRING_PARAMS", url)
        }

        // we already check once if it is installed, but this also takes care if yatse get uninstalled while this fragment is active
        if (isYatseInstalled()) {
            try {
                ContextCompat.startForegroundService(requireContext(), yatseIntent)
            } catch (e: IllegalStateException) {
                context?.showToast(R.string.limitations)
            }
        } else
            context?.showToast(R.string.app_not_installed)
    }

    //already added the query
    @SuppressLint("QueryPermissionsNeeded")
    private fun isYatseInstalled(): Boolean {
        return context?.packageManager
            ?.getInstalledPackages(PackageManager.GET_META_DATA)
            ?.firstOrNull { it.packageName == "org.leetzone.android.yatsewidgetfree" } != null
    }

    override fun onLoadStreamsClick(id: String) {
        lifecycleScope.launch {
            if (activityViewModel.isTokenPrivate()) {
                viewModel.fetchStreamingInfo(id)
            } else
                context?.showToast(R.string.api_needs_private_token)

        }
    }

    override fun onBrowserStreamsClick(id: String) {
        openExternalWebPage(RD_STREAMING_URL + id)
    }

    override fun onDownloadClick(link: String, fileName: String) {

        val manager =
            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(link))
            .setTitle(getString(R.string.app_name))
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val downloadID = manager.enqueue(request)
    }
}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)
    fun onOpenClick(url: String)
    fun onOpenWith(url: String)
    fun onLoadStreamsClick(id: String)
    fun onBrowserStreamsClick(id: String)
    fun onDownloadClick(link: String, fileName: String)
}