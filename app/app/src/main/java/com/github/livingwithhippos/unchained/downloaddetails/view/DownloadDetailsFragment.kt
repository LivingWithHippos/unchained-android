package com.github.livingwithhippos.unchained.downloaddetails.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.model.Alternative
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.downloaddetails.model.AlternativeDownloadAdapter
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsMessage
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadEvent
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.RD_STREAMING_URL
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass. It is capable of showing the details of a [DownloadItem]
 */
@AndroidEntryPoint
class DownloadDetailsFragment : UnchainedFragment(), DownloadDetailsListener {

    private val viewModel: DownloadDetailsViewModel by activityViewModels()

    private val args: DownloadDetailsFragmentArgs by navArgs()

    private val deviceServiceMap: MutableMap<RemoteDevice, List<RemoteService>> = mutableMapOf()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.download_details_bar, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.delete -> {
                            val dialog = DeleteDialogFragment()
                            val bundle = Bundle()
                            val title =
                                getString(R.string.delete_item_title_format, args.details.filename)
                            bundle.putString("title", title)
                            dialog.arguments = bundle
                            dialog.show(parentFragmentManager, "DeleteDialogFragment")
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

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
        detailsBinding.showLoadStream =
            viewModel.getButtonVisibilityPreference(SHOW_LOAD_STREAM_BUTTON)
        detailsBinding.showStreamBrowser =
            viewModel.getButtonVisibilityPreference(SHOW_STREAM_BROWSER_BUTTON)

        detailsBinding.fabPickStreaming.setOnClickListener { popView ->
            manageStreamingPopup(popView)
        }

        viewModel.streamLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                detailsBinding.stream = it

                val streams = mutableListOf<Alternative>()
                // parameter mimetype gets shown as the name and "streaming" as title in the list,
                // the other
                // params don't matter
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
                activityViewModel.setListState(ListState.UpdateDownload)
            }
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            // the delete operation is observed from the viewModel
            if (bundle.getBoolean("deleteConfirmation")) viewModel.deleteDownload(args.details.id)
        }

        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (val content = it.getContentIfNotHandled()) {
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

                is DownloadDetailsMessage.KodiShowPicker -> {
                    val dialog = KodiServerPickerDialog()
                    val bundle = Bundle()
                    bundle.putString("url", content.url)
                    dialog.arguments = bundle
                    dialog.show(parentFragmentManager, "KodiServerPickerDialog")
                }

                else -> {}
            }
        }

        viewModel.eventLiveData.observe(viewLifecycleOwner) {
            when (val content = it.peekContent()) {
                is DownloadEvent.DefaultDeviceService -> {
                    // send media to default device
                }

                is DownloadEvent.DeviceAndServices -> {
                    // used to populate the menu
                    deviceServiceMap.clear()
                    deviceServiceMap.putAll(content.devicesServices)
                }

                is DownloadEvent.KodiDevices -> {}
            }
        }

        viewModel.fetchDevicesAndServices()

        return detailsBinding.root
    }

    private fun manageStreamingPopup(popView: View) {
        val layoutInflater = LayoutInflater.from(requireContext())
        val popup = showStreamingPopupWindow(layoutInflater)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(popView)
        // popup.showAtLocation(it, Gravity.CENTER, 0, 0)

        val recentService: Int = viewModel.getRecentService()

        val defaultDevice: Map.Entry<RemoteDevice, List<RemoteService>>? =
            deviceServiceMap.firstNotNullOfOrNull {
                if (it.key.isDefault) it else null
            }
        val defaultService: RemoteService? = defaultDevice?.value?.firstOrNull { it.isDefault }


        // get all the services of the corresponding menu item
        // populate according to results
        val defaultLayout =
            popup.contentView.findViewById<ConstraintLayout>(R.id.defaultServiceLayout)

        if (defaultService != null) {
            val serviceType: RemoteServiceType? = getServiceType(defaultService.type)
            if (serviceType != null) {
                defaultLayout.findViewById<ImageView>(R.id.defaultServiceIcon)
                    .setImageResource(serviceType.iconRes)
                defaultLayout.findViewById<TextView>(R.id.defaultServiceName).text = defaultService.name
                // ip from device, port from service
                defaultLayout.findViewById<TextView>(R.id.defaultServiceAddress).text =
                    "${defaultDevice.key.address}:${defaultService.port}"

                defaultLayout.setOnClickListener {
                    playOnDeviceService(
                        args.details,
                        defaultDevice.key,
                        defaultService,
                        serviceType
                    )
                    if (popup.isShowing)
                        popup.dismiss()
                }
            } else {
                defaultLayout.visibility = View.GONE
            }
        } else {
            defaultLayout.visibility = View.GONE
        }


        val recentLayout = popup.contentView.findViewById<ConstraintLayout>(R.id.recentServiceLayout)
        if (recentService != -1) {
            val recentServiceItem: RemoteService? = deviceServiceMap.firstNotNullOfOrNull {
                it.value.firstOrNull { service -> service.id == recentService }
            }
            if (recentServiceItem != null) {
                val recentDeviceItem =
                    deviceServiceMap.keys.firstOrNull { it.id == recentServiceItem.device }
                val serviceType: RemoteServiceType? = getServiceType(recentServiceItem.type)
                if (recentDeviceItem != null && serviceType != null) {
                    recentLayout.findViewById<ImageView>(R.id.defaultServiceIcon)
                        .setImageResource(serviceType.iconRes)
                    recentLayout.findViewById<TextView>(R.id.defaultServiceName).text = recentServiceItem.name
                    // ip from device, port from service
                    recentLayout.findViewById<TextView>(R.id.defaultServiceAddress).text =
                        "${recentDeviceItem.address}:${recentServiceItem.port}"

                    recentLayout.setOnClickListener {
                        // todo: send to selected recent service
                        if (popup.isShowing)
                            popup.dismiss()
                    }
                } else {
                    recentLayout.visibility = View.GONE
                }
            } else {
                recentLayout.visibility = View.GONE
            }
        } else {
            recentLayout.visibility = View.GONE
        }

        // todo: distinguish between devices for streaming and for searching content
        val pickerLayout = popup.contentView.findViewById<ConstraintLayout>(R.id.pickServiceLayout)
        pickerLayout.findViewById<TextView>(R.id.servicesNumber).text = resources.getQuantityString(
            R.plurals.service_number_format,
            deviceServiceMap.values.size
        )
        pickerLayout.findViewById<TextView>(R.id.devicesNumber).text = resources.getQuantityString(
            R.plurals.device_number_format,
            deviceServiceMap.keys.size
        )
        pickerLayout.setOnClickListener {
            // todo: picker dialog
            if (popup.isShowing)
                popup.dismiss()
        }

        val browserLayout = popup.contentView.findViewById<ConstraintLayout>(R.id.streamBrowserLayout)
        browserLayout.setOnClickListener {
            onBrowserStreamsClick(args.details.id)
            if (popup.isShowing)
                popup.dismiss()
        }
    }

    private fun playOnDeviceService(
        item: DownloadItem,
        device: RemoteDevice,
        service: RemoteService,
        serviceType: RemoteServiceType
    ) {
        when (serviceType) {
            RemoteServiceType.KODI -> {
                viewModel.openUrlOnKodi(
                    mediaURL = item.download,
                    kodiDevice = device,
                    kodiService = service
                )
            }

            RemoteServiceType.VLC -> {
                viewModel.playOnVlc(
                    mediaURL = item.download,
                    vlcDevice = device,
                    vlcService = service
                )
            }

            RemoteServiceType.JACKETT -> {
                // todo: only streamable services allowed, rework db and queries to only get streaming services in this fragment
            }
        }
    }

    private fun showStreamingPopupWindow(inflater: LayoutInflater): PopupWindow {
        val view = inflater.inflate(R.layout.popup_window, null)
        return PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /*
    @SuppressLint("SetTextI18n")
    private fun showStreamingMenu(v: View, @MenuRes menuRes: Int) {

        // todo: https://medium.com/android-beginners/popupwindow-android-example-in-kotlin-5919245c8b8a
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        val recentService: Int = viewModel.getRecentService()

        val defaultDevice: Map.Entry<RemoteDevice, List<RemoteService>>? = deviceServiceMap.firstNotNullOfOrNull {
            if (it.key.isDefault) it else null
        }
        val defaultService: RemoteService? = defaultDevice?.value?.firstOrNull { it.isDefault }


        // get all the services of the corresponding menu item
        // populate according to results

        if (defaultService != null) {
            val serviceType: RemoteServiceType? = getServiceType(defaultService.type)
            if (serviceType != null) {
                popup.menu.findItem(R.id.default_service).actionView?.let {
                    it.findViewById<ImageView>(R.id.defaultServiceIcon).setImageResource(serviceType.iconRes)
                    it.findViewById<TextView>(R.id.defaultServiceName).text = getString(serviceType.nameRes)
                    // ip from device, port from service
                    it.findViewById<TextView>(R.id.defaultServiceAddress).text = "${defaultDevice.key.address}:${defaultService.port}"
                }
            } else {
                popup.menu.findItem(R.id.default_service).isVisible = false
            }
        } else {
            popup.menu.findItem(R.id.default_service).isVisible = false
        }

        if (recentService != -1) {
            val recentServiceItem: RemoteService? = deviceServiceMap.firstNotNullOfOrNull {
                it.value.firstOrNull { service -> service.id == recentService }
            }
            if (recentServiceItem != null) {
                val recentDeviceItem = deviceServiceMap.keys.firstOrNull { it.id == recentServiceItem.device }
                val serviceType: RemoteServiceType? = getServiceType(recentServiceItem.type)
                if (recentDeviceItem != null && serviceType!=null) {
                    popup.menu.findItem(R.id.recent_service).actionView?.let {
                        it.findViewById<ImageView>(R.id.defaultServiceIcon).setImageResource(serviceType.iconRes)
                        it.findViewById<TextView>(R.id.defaultServiceName).text = getString(serviceType.nameRes)
                        // ip from device, port from service
                        it.findViewById<TextView>(R.id.defaultServiceAddress).text = "${recentDeviceItem.address}:${recentServiceItem.port}"
                    }
                } else {
                    popup.menu.findItem(R.id.recent_service).isVisible = false
                }
            } else {
                popup.menu.findItem(R.id.recent_service).isVisible = false
            }
        } else {
            popup.menu.findItem(R.id.recent_service).isVisible = false
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.default_service -> {
                    viewModel.fetchDefaultService()
                }
                R.id.pick_service -> {
                    // todo: implement a picker
                }
                R.id.recent_service -> {
                }
                R.id.stream_browser -> {
                    onBrowserStreamsClick(args.details.id)
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
     */

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        context?.showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        try {
            context?.openExternalWebPage(url)
        } catch (e: ActivityNotFoundException) {
            Timber.e("Error opening externally a link ${e.message}")
            context?.showToast(R.string.no_supported_player_found)
        }
    }

    override fun onOpenTranscodedStream(url: String) {
        viewModel.openKodiPickerIfNeeded(url)
    }

    override fun onLoadStreamsClick(id: String) {
        lifecycleScope.launch {
            if (activityViewModel.isTokenPrivate()) {
                viewModel.fetchStreamingInfo(id)
            } else context?.showToast(R.string.api_needs_private_token)
        }
    }

    override fun onBrowserStreamsClick(id: String) {
        context?.openExternalWebPage(RD_STREAMING_URL + id)
    }

    override fun onDownloadClick(link: String, fileName: String) {
        activityViewModel.enqueueDownload(link, fileName)
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
        } catch (e: ActivityNotFoundException) {
            context?.showToast(R.string.app_not_installed)
        }
    }

    private fun createMediaIntent(
        appPackage: String,
        url: String,
        component: ComponentName? = null,
        dataType: String = "video/*"
    ): Intent {

        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setPackage(appPackage)
        intent.setDataAndTypeAndNormalize(uri, dataType)
        if (component != null) intent.component = component

        return intent
    }

    override fun onSendToPlayer(url: String) {
        when (viewModel.getDefaultPlayer()) {
            "vlc" -> {
                val vlcIntent = createMediaIntent("org.videolan.vlc", url)
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
                } catch (e: ActivityNotFoundException) {
                    mxIntent.setPackage("com.mxtech.videoplayer.ad")
                    tryStartExternalApp(mxIntent)
                }
            }

            "web_video_cast" -> {
                val wvcIntent = createMediaIntent("com.instantbits.cast.webvideo", url)
                tryStartExternalApp(wvcIntent)
            }

            "custom_player" -> {
                val customPlayerPackage = viewModel.getCustomPlayerPreference()
                if (customPlayerPackage.isBlank()) {
                    context?.showToast(R.string.invalid_package)
                } else {
                    val customIntent = createMediaIntent(customPlayerPackage, url)
                    tryStartExternalApp(customIntent)
                }
            }

            else -> {
                context?.showToast(R.string.missing_default_player)
            }
        }
    }

    private fun getServiceType(type: Int): RemoteServiceType? {
        return when (type) {
            RemoteServiceType.KODI.value -> RemoteServiceType.KODI
            RemoteServiceType.VLC.value -> RemoteServiceType.VLC
            RemoteServiceType.JACKETT.value -> RemoteServiceType.JACKETT
            else -> {
                Timber.e("Unknown service type ${type}")
                null
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

    fun onOpenTranscodedStream(url: String)

    fun onLoadStreamsClick(id: String)

    fun onBrowserStreamsClick(id: String)

    fun onDownloadClick(link: String, fileName: String)

    fun onShareClick(url: String)

    fun onSendToPlayer(url: String)
}
