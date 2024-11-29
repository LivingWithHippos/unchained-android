package com.github.livingwithhippos.unchained.downloaddetails.view

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.local.serviceTypeMap
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
import com.github.livingwithhippos.unchained.utilities.extension.getAvailableSpace
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
        savedInstanceState: Bundle?,
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
            Lifecycle.State.RESUMED,
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
        detailsBinding.showStreaming =
            viewModel.getButtonVisibilityPreference(SHOW_STREAMING_BUTTON)
        detailsBinding.showLocalPlay = viewModel.getButtonVisibilityPreference(SHOW_MEDIA_BUTTON)
        detailsBinding.showLoadStream =
            viewModel.getButtonVisibilityPreference(SHOW_TRANSCODING_BUTTON)

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
                        getString(R.string.streaming),
                        "h264 WebM",
                    )
                )
                streams.add(
                    Alternative(
                        "liveMP4",
                        "liveMP4",
                        it.liveMP4.link,
                        getString(R.string.streaming),
                        "mp4",
                    )
                )
                streams.add(
                    Alternative(
                        "apple",
                        "m3u8",
                        it.apple.link,
                        getString(R.string.streaming),
                        "m3u8",
                    )
                )
                streams.add(
                    Alternative("dash", "mpd", it.dash.link, getString(R.string.streaming), "mpd")
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
                context?.showToast(R.string.download_removed)
                activityViewModel.setListState(ListState.UpdateDownload)
                // if deleted go back
                findNavController().popBackStack()
            },
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            // the delete operation is observed from the viewModel
            if (bundle.getBoolean("deleteConfirmation")) viewModel.deleteDownload(args.details.id)
        }

        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it.getContentIfNotHandled()) {
                is DownloadDetailsMessage.KodiError -> {
                    context?.showToast(R.string.connection_error)
                }
                is DownloadDetailsMessage.KodiSuccess -> {
                    context?.showToast(R.string.connection_successful)
                }
                DownloadDetailsMessage.KodiMissingCredentials -> {
                    context?.showToast(R.string.kodi_configure_credentials)
                }
                DownloadDetailsMessage.KodiMissingDefault -> {
                    context?.showToast(R.string.kodi_missing_default)
                }
                else -> {}
            }
        }

        lifecycle.coroutineScope.launch {
            viewModel.devicesAndServices().collect {
                // used to populate the menu
                deviceServiceMap.clear()
                deviceServiceMap.putAll(it)
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

        return detailsBinding.root
    }

    private fun showBasicStreamingPopup(v: View, url: String?) {

        val recentService: Int = viewModel.getRecentService()

        val defaultDevice: Map.Entry<RemoteDevice, List<RemoteService>>? =
            deviceServiceMap.firstNotNullOfOrNull { if (it.key.isDefault) it else null }
        val defaultService: RemoteService? = defaultDevice?.value?.firstOrNull { it.isDefault }
        val servicesNumber = deviceServiceMap.values.sumOf { it.size }
        val recentServiceItem: RemoteService? =
            deviceServiceMap.firstNotNullOfOrNull {
                it.value.firstOrNull { service -> service.id == recentService }
            }

        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(R.menu.basic_streaming_popup, popup.menu)

        if (
            recentService == -1 || recentService == defaultService?.id || recentServiceItem == null
        ) {
            popup.menu.findItem(R.id.recent_service).isVisible = false
        } else {
            val serviceName = getString(serviceTypeMap[recentServiceItem.type]!!.nameRes)
            popup.menu.findItem(R.id.recent_service).title =
                getString(R.string.recent_service_format, serviceName)
        }

        if (defaultDevice == null || defaultService == null) {
            popup.menu.findItem(R.id.default_service).isVisible = false
        } else {
            val serviceName = getString(serviceTypeMap[defaultService.type]!!.nameRes)
            popup.menu.findItem(R.id.default_service).title =
                getString(R.string.default_service_format, serviceName)
        }

        if (servicesNumber == 0) {
            popup.menu.findItem(R.id.pick_service).isVisible = false
        }

        if (url != null) {
            popup.menu.findItem(R.id.browser_streaming).isVisible = false
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // save the new sorting preference
            when (menuItem.itemId) {
                R.id.recent_service -> {
                    val recentDeviceItem =
                        deviceServiceMap.keys.firstOrNull { it.id == recentServiceItem?.device }

                    if (recentServiceItem != null && recentDeviceItem != null) {

                        val serviceType: RemoteServiceType =
                            getServiceType(recentServiceItem.type)!!
                        playOnDeviceService(
                            url ?: args.details.download,
                            recentDeviceItem,
                            recentServiceItem,
                            serviceType,
                        )
                    }
                }
                R.id.default_service -> {
                    if (defaultDevice != null && defaultService != null) {
                        val serviceType: RemoteServiceType = getServiceType(defaultService.type)!!
                        playOnDeviceService(
                            url ?: args.details.download,
                            defaultDevice.key,
                            defaultService,
                            serviceType,
                        )
                    }
                }
                R.id.pick_service -> {
                    val dialog = ServicePickerDialog()
                    val bundle = Bundle()
                    bundle.putString("downloadUrl", url ?: args.details.download)
                    dialog.arguments = bundle
                    dialog.show(parentFragmentManager, "ServicePickerDialog")
                }
                R.id.browser_streaming -> {
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

    @SuppressLint("SetTextI18n")
    private fun manageStreamingPopup(popView: View, url: String? = null) {
        val uiModeManager: UiModeManager =
            requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        // custom popup menu does not work on android tv (emulator at least), maybe it's the size
        // check
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            showBasicStreamingPopup(popView, url)
            return
        }
        val popup: PopupWindow = getFullStreamingPopupWindow(popView)

        val recentService: Int = viewModel.getRecentService()

        val defaultDevice: Map.Entry<RemoteDevice, List<RemoteService>>? =
            deviceServiceMap.firstNotNullOfOrNull { if (it.key.isDefault) it else null }
        val defaultService: RemoteService? = defaultDevice?.value?.firstOrNull { it.isDefault }

        // get all the services of the corresponding menu item
        // populate according to results
        val defaultLayout =
            popup.contentView.findViewById<ConstraintLayout>(R.id.defaultServiceLayout)

        if (defaultService != null) {
            val serviceType: RemoteServiceType? = getServiceType(defaultService.type)
            if (serviceType != null) {
                defaultLayout
                    .findViewById<ImageView>(R.id.serviceIcon)
                    .setImageResource(serviceType.iconRes)
                defaultLayout.findViewById<TextView>(R.id.serviceName).text = defaultService.name
                // ip from device, port from service
                defaultLayout.findViewById<TextView>(R.id.serviceAddress).text =
                    "${defaultDevice.key.address}:${defaultService.port}"

                defaultLayout.setOnClickListener {
                    if (popup.isShowing) popup.dismiss()
                    playOnDeviceService(
                        url ?: args.details.download,
                        defaultDevice.key,
                        defaultService,
                        serviceType,
                    )
                }
            } else {
                defaultLayout.visibility = View.GONE
            }
        } else {
            defaultLayout.visibility = View.GONE
        }

        val recentLayout =
            popup.contentView.findViewById<ConstraintLayout>(R.id.recentServiceLayout)
        if (recentService != -1 && recentService != defaultService?.id) {
            val recentServiceItem: RemoteService? =
                deviceServiceMap.firstNotNullOfOrNull {
                    it.value.firstOrNull { service -> service.id == recentService }
                }
            if (recentServiceItem != null) {
                val recentDeviceItem =
                    deviceServiceMap.keys.firstOrNull { it.id == recentServiceItem.device }
                val serviceType: RemoteServiceType? = getServiceType(recentServiceItem.type)
                if (recentDeviceItem != null && serviceType != null) {
                    recentLayout
                        .findViewById<ImageView>(R.id.recentServiceIcon)
                        .setImageResource(serviceType.iconRes)
                    recentLayout.findViewById<TextView>(R.id.recentServiceName).text =
                        recentServiceItem.name
                    // ip from device, port from service
                    recentLayout.findViewById<TextView>(R.id.recentServiceAddress).text =
                        "${recentDeviceItem.address}:${recentServiceItem.port}"

                    recentLayout.setOnClickListener {
                        if (popup.isShowing) popup.dismiss()
                        playOnDeviceService(
                            url ?: args.details.download,
                            recentDeviceItem,
                            recentServiceItem,
                            serviceType,
                        )
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

        val pickerLayout = popup.contentView.findViewById<ConstraintLayout>(R.id.pickServiceLayout)
        val servicesNumber = deviceServiceMap.values.sumOf { it.size }
        pickerLayout.findViewById<TextView>(R.id.servicesNumber).text =
            resources.getQuantityString(
                R.plurals.service_number_format,
                servicesNumber,
                servicesNumber,
            )
        pickerLayout.findViewById<TextView>(R.id.devicesNumber).text =
            resources.getQuantityString(
                R.plurals.device_number_format,
                deviceServiceMap.keys.size,
                deviceServiceMap.keys.size,
            )
        pickerLayout.setOnClickListener {
            if (popup.isShowing) popup.dismiss()

            val dialog = ServicePickerDialog()
            val bundle = Bundle()
            bundle.putString("downloadUrl", url ?: args.details.download)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "ServicePickerDialog")
        }

        val browserLayout =
            popup.contentView.findViewById<ConstraintLayout>(R.id.streamBrowserLayout)
        // the url is passed when clicking on the streaming button in the transcoding list
        // which has no "stream in browser" support
        if (url != null) {
            browserLayout.visibility = View.GONE
        } else {
            browserLayout.setOnClickListener {
                onBrowserStreamsClick(args.details.id)
                if (popup.isShowing) popup.dismiss()
            }
        }
    }

    private fun playOnDeviceService(
        link: String,
        device: RemoteDevice,
        service: RemoteService,
        serviceType: RemoteServiceType,
    ) {
        when (serviceType) {
            RemoteServiceType.KODI -> {
                viewModel.openUrlOnKodi(mediaURL = link, kodiDevice = device, kodiService = service)
            }
            RemoteServiceType.VLC -> {
                viewModel.openUrlOnVLC(mediaURL = link, vlcDevice = device, vlcService = service)
            }
            else -> {
                // should not happen
                Timber.e("Unknown service type $serviceType")
            }
        }
    }

    private fun getFullStreamingPopupWindow(parentView: View): PopupWindow {
        val screenDistances = getAvailableSpace(parentView)
        val popup =
            PopupWindow(parentView.context)
                .apply {
                    isOutsideTouchable = true
                    val inflater = LayoutInflater.from(parentView.context)
                    contentView =
                        inflater.inflate(R.layout.popup_streaming_window, null).apply {
                            measure(
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            )
                        }
                }
                .also { popupWindow ->
                    popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    if (screenDistances[2] < 600 && screenDistances[0] > 600) {
                        // Absolute location of the anchor view
                        val location = IntArray(2).apply { parentView.getLocationOnScreen(this) }
                        val size =
                            Size(
                                popupWindow.contentView.measuredWidth,
                                popupWindow.contentView.measuredHeight,
                            )
                        popupWindow.showAtLocation(
                            parentView,
                            Gravity.TOP or Gravity.START,
                            location[0] - (size.width - parentView.width) / 2,
                            location[1] - (size.height / 2),
                        )
                    } else {
                        popupWindow.showAsDropDown(parentView)
                    }
                }

        return popup
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        context?.showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        context?.openExternalWebPage(url)
    }

    override fun onOpenTranscodedStream(view: View, url: String) {
        manageStreamingPopup(view, url)
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
        dataType: String = "video/*",
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
            "play_it" -> {
                val wvcIntent = createMediaIntent("com.playit.videoplayer", url)
                tryStartExternalApp(wvcIntent)
            }
            "player_just_video" -> {
                val wvcIntent = createMediaIntent("com.brouken.player", url)
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
                Timber.e("Unknown service type $type")
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
        const val SHOW_STREAMING_BUTTON = "show_streaming"
        const val SHOW_TRANSCODING_BUTTON = "show_load_stream_button"
    }
}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)

    fun onOpenClick(url: String)

    fun onOpenTranscodedStream(view: View, url: String)

    fun onLoadStreamsClick(id: String)

    fun onBrowserStreamsClick(id: String)

    fun onDownloadClick(link: String, fileName: String)

    fun onShareClick(url: String)

    fun onSendToPlayer(url: String)
}
