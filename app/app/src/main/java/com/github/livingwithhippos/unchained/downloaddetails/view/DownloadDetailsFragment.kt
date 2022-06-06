package com.github.livingwithhippos.unchained.downloaddetails.view

import android.Manifest
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsMessage
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.RD_STREAMING_URL
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        detailsBinding.details = args.details
        detailsBinding.listener = this

        // set up streams and alternative (e.g. for youtube) links list
        val alternativeAdapter = AlternativeDownloadAdapter(this)
        detailsBinding.rvAlternativeList.adapter = alternativeAdapter

        if (!args.details.alternative.isNullOrEmpty()) {
            alternativeAdapter.submitList(args.details.alternative)
        }

        detailsBinding.showShare = viewModel.getButtonVisibilityPreference(SHOW_SHARE_BUTTON)
        detailsBinding.showOpen = viewModel.getButtonVisibilityPreference(SHOW_OPEN_BUTTON)
        detailsBinding.showCopy = viewModel.getButtonVisibilityPreference(SHOW_COPY_BUTTON)
        detailsBinding.showDownload = viewModel.getButtonVisibilityPreference(SHOW_DOWNLOAD_BUTTON)
        detailsBinding.showKodi = viewModel.getButtonVisibilityPreference(SHOW_KODI_BUTTON)
        detailsBinding.showLocalPlay = viewModel.getButtonVisibilityPreference(SHOW_MEDIA_BUTTON)
        detailsBinding.showLoadStream = viewModel.getButtonVisibilityPreference(
            SHOW_LOAD_STREAM_BUTTON)
        detailsBinding.showStreamBrowser = viewModel.getButtonVisibilityPreference(
            SHOW_STREAM_BROWSER_BUTTON)

        viewModel.streamLiveData.observe(
            viewLifecycleOwner
        ) {
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
        }

        viewModel.deletedDownloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.download_removed)
                // if deleted go back
                activity?.onBackPressed()
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_DOWNLOAD)
            }
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            // the delete operation is observed from the viewModel
            if (bundle.getBoolean("deleteConfirmation"))
                viewModel.deleteDownload(args.details.id)
        }

        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it.getContentIfNotHandled()) {
                is DownloadDetailsMessage.KodiError -> {
                    context?.showToast(R.string.kodi_connection_error)
                }
                is DownloadDetailsMessage.KodiSuccess -> {
                    context?.showToast(R.string.kodi_connection_successful)
                }
                DownloadDetailsMessage.KodiMissingCredentials -> {
                    context?.showToast(R.string.kodi_configure_credentials)
                }
                DownloadDetailsMessage.KodiMissingDefault -> {
                    context?.showToast(R.string.kodi_missing_default)
                }
                null -> {
                }
            }
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
                val bundle = Bundle()
                val title = getString(R.string.delete_item_title_format, args.details.filename)
                bundle.putString("title", title)
                dialog.arguments = bundle
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

    override fun onOpenWithKodi(url: String) {
        viewModel.openUrlOnKodi(url)
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

        when (Build.VERSION.SDK_INT) {
            22 -> {
                downloadFile(link, fileName)
            }
            in 23..28 -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            else -> {
                downloadFile(link, fileName)
            }
        }
    }

    private fun downloadFile(link: String, fileName: String) {
        val manager =
            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val queuedDownload = manager.downloadFile(
            link = link,
            title = fileName,
            description = getString(R.string.app_name)
        )
        when (queuedDownload) {
            is EitherResult.Failure -> {
                context?.showToast(R.string.download_not_started)
            }
            is EitherResult.Success -> {
                context?.showToast(R.string.download_started)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val link = args.details.download
                val fileName = args.details.filename
                downloadFile(link, fileName)
            } else {
                context?.showToast(R.string.needs_download_permission)
            }
        }

    override fun onShareClick(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
    }

    private fun tryStartExternalApp(intent: Intent) {

        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            context?.showToast(R.string.app_not_installed)
        }
    }

    private fun createMediaIntent(appPackage: String, url: String, component: ComponentName? = null, dataType: String = "video/*"): Intent {

        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage(appPackage)
        intent.setDataAndTypeAndNormalize(uri, dataType)
        if (component!=null)
            intent.component = component

        return intent
    }

    override fun onSendToPlayer(url: String) {
        when (viewModel.getDefaultPlayer()) {
            "vlc" -> {
                val vlcIntent = createMediaIntent("org.videolan.vlc", url, ComponentName(
                    "org.videolan.vlc",
                    "org.videolan.vlc.gui.video.VideoPlayerActivity"
                ))

                tryStartExternalApp(vlcIntent)
            }
            "mpv" -> {
                val mpvIntent = createMediaIntent("is.xyz.mpv", url)
                tryStartExternalApp(mpvIntent)
            }
            "mx_player" -> {
                val mxIntent = createMediaIntent("com.mxtech.videoplayer.pro", url)

                try {
                    startActivity(mxIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    mxIntent.setPackage("com.mxtech.videoplayer.ad")
                    tryStartExternalApp(mxIntent)
                }
            }
            "web_video_cast" -> {
                val wvcIntent = createMediaIntent("com.instantbits.cast.webvideo", url)
                tryStartExternalApp(wvcIntent)
            }
            else -> {
                context?.showToast(R.string.missing_default_player)
            }
        }
    }

    companion object {
        const val SHOW_SHARE_BUTTON = "show_share_button"
        const val SHOW_OPEN_BUTTON = "show_open_button"
        const val SHOW_COPY_BUTTON = "show_copy_button"
        const val SHOW_DOWNLOAD_BUTTON = "show_download_button"
        const val SHOW_MEDIA_BUTTON = "show_media_button"
        const val SHOW_KODI_BUTTON = "show_kodi"
        const val SHOW_LOAD_STREAM_BUTTON = "show_load_stream_button"
        const val SHOW_STREAM_BROWSER_BUTTON = "show_stream_browser_button"
    }
}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)
    fun onOpenClick(url: String)
    fun onOpenWithKodi(url: String)
    fun onLoadStreamsClick(id: String)
    fun onBrowserStreamsClick(id: String)
    fun onDownloadClick(link: String, fileName: String)
    fun onShareClick(url: String)
    fun onSendToPlayer(url: String)
}
