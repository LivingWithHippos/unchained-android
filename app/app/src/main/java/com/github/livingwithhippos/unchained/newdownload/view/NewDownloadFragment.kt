package com.github.livingwithhippos.unchained.newdownload.view

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import com.github.livingwithhippos.unchained.utilities.extension.runRippleAnimation
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A simple [UnchainedFragment] subclass.
 * Allow the user to create a new download from a link or a torrent file.
 */
@AndroidEntryPoint
class NewDownloadFragment : UnchainedFragment(), NewDownloadListener {

    private val viewModel: NewDownloadViewModel by viewModels()

    private val args: NewDownloadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadBinding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        downloadBinding.listener = this

        viewModel.linkLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { linkDetails ->
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(
                        linkDetails
                    )
                findNavController().navigate(action)
            }
        })

        viewModel.torrentLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { torrent ->
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToTorrentDetailsFragment(
                        torrent.id
                    )
                findNavController().navigate(action)
            }
        })

        viewModel.apiErrorLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { error ->
                //todo: move this code outside onCreateView
                when (error.errorCode) {
                    -1 -> context?.showToast(R.string.internal_error)
                    1 -> context?.showToast(R.string.missing_parameter)
                    2 -> context?.showToast(R.string.bad_parameter_value)
                    3 -> context?.showToast(R.string.unknown_method)
                    4 -> context?.showToast(R.string.method_not_allowed)
                    // what is this error for?
                    5 -> context?.showToast(R.string.slow_down)
                    6 -> context?.showToast(R.string.resource_unreachable)
                    7 -> context?.showToast(R.string.resource_not_found)
                    //todo: check these
                    8 -> {
                        context?.showToast(R.string.bad_token)
                        activityViewModel.setUnauthenticated()
                    }
                    9 -> context?.showToast(R.string.permission_denied)
                    10 -> context?.showToast(R.string.tfa_needed)
                    11 -> context?.showToast(R.string.tfa_pending)
                    12 -> {
                        context?.showToast(R.string.invalid_login)
                        activityViewModel.setUnauthenticated()
                    }
                    13 -> context?.showToast(R.string.invalid_password)
                    14 -> {
                        context?.showToast(R.string.account_locked)
                        activityViewModel.setUnauthenticated()
                    }
                    15 -> context?.showToast(R.string.account_not_activated)
                    16 -> context?.showToast(R.string.unsupported_hoster)
                    17 -> context?.showToast(R.string.hoster_in_maintenance)
                    18 -> context?.showToast(R.string.hoster_limit_reached)
                    19 -> context?.showToast(R.string.hoster_temporarily_unavailable)
                    20 -> context?.showToast(R.string.hoster_not_available_for_free_users)
                    21 -> context?.showToast(R.string.too_many_active_downloads)
                    22 -> context?.showToast(R.string.ip_Address_not_allowed)
                    23 -> context?.showToast(R.string.traffic_exhausted)
                    24 -> context?.showToast(R.string.file_unavailable)
                    25 -> context?.showToast(R.string.service_unavailable)
                    26 -> context?.showToast(R.string.upload_too_big)
                    27 -> context?.showToast(R.string.upload_error)
                    28 -> context?.showToast(R.string.file_not_allowed)
                    29 -> context?.showToast(R.string.torrent_too_big)
                    30 -> context?.showToast(R.string.torrent_file_invalid)
                    31 -> context?.showToast(R.string.action_already_done)
                    32 -> context?.showToast(R.string.image_resolution_error)
                    33 -> context?.showToast(R.string.torrent_already_active)
                    else -> context?.showToast(R.string.error_unrestricting_download)
                }
                // re enable buttons to let the user take other actions
                //todo: this needs to be done also for other errors. maybe throw another error from the ViewModel
                downloadBinding.bUnrestrict.isEnabled = true
                downloadBinding.bLoadTorrent.isEnabled = true
            }
        })

        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {

            val authState = activityViewModel.authenticationState.value?.peekContent()
            if (authState == AuthenticationState.AUTHENTICATED) {
                val link: String = downloadBinding.tiLink.text.toString().trim()
                when {
                    link.isWebUrl()-> {
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false

                        var password: String? = downloadBinding.tePassword.text.toString()
                        // we don't pass the password if it is blank.
                        // N.B. it won't work if your password is made up of spaces but then again you deserve it
                        if (password.isNullOrBlank())
                            password = null
                        val remote: Int? =
                            if (downloadBinding.switchRemote.isEnabled) REMOTE_TRAFFIC_ON else null

                        viewModel.fetchUnrestrictedLink(
                            link,
                            password,
                            remote
                        )
                    }
                    link.isMagnet() -> {
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false
                        viewModel.fetchAddedMagnet(link)
                    }
                    link.isBlank()-> {
                        context?.showToast(R.string.please_insert_url)
                    }
                    else -> {
                        context?.showToast(R.string.invalid_url)
                    }
                }

            }
            else
                context?.showToast(R.string.premium_needed)
        }

        downloadBinding.bPasteLink.setOnClickListener {
            val pasteText = getClipboardText()

            if (pasteText.isWebUrl() || pasteText.isMagnet())
                downloadBinding.tiLink.setText(pasteText, TextView.BufferType.EDITABLE)
            else
                context?.showToast(R.string.invalid_url)
        }

        downloadBinding.bPastePassword.setOnClickListener {
            val pasteText = getClipboardText()
            downloadBinding.tePassword.setText(pasteText, TextView.BufferType.EDITABLE)
        }

        // we must make this value null by default because it's the first fragment of the nav graph
        if (args.links?.firstOrNull() != null) {
            downloadBinding.tiLink.setText(args.links!!.first(), TextView.BufferType.EDITABLE)
            // run the ripple animation on the unrestrict button
            downloadBinding.bUnrestrict.runRippleAnimation()
        }

        activityViewModel.externalLinkLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { link ->
                when (link.scheme) {
                    SCHEME_MAGNET -> {
                        context?.showToast(R.string.loading_magnet_link)
                        // set as text input text
                        downloadBinding.tiLink.setText(
                            link.toString(),
                            TextView.BufferType.EDITABLE
                        )
                        // simulate button click
                        downloadBinding.bUnrestrict.performClick()
                    }
                    SCHEME_CONTENT, SCHEME_FILE -> {
                        context?.showToast(R.string.loading_torrent_file)
                        loadTorrent(requireContext().contentResolver, link)
                    }
                    SCHEME_HTTP, SCHEME_HTTPS -> {
                        context?.showToast(R.string.loading_torrent_file)
                        downloadTorrent(link)
                    }
                }
            }
        })

        activityViewModel.downloadedTorrentLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { fileName ->
                val torrentFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),fileName)
                loadTorrent(requireContext().contentResolver, torrentFile.toUri())
            }
        })

        return downloadBinding.root
    }

    private val getTorrent: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                loadTorrent(requireContext().contentResolver, uri)
            } else {
                context?.showToast(R.string.error_loading_torrent)
            }
        }

    override fun onLoadTorrentClick() {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED)
            getTorrent.launch("application/x-bittorrent")
        else
            context?.showToast(R.string.premium_needed)
    }

    private fun loadTorrent(contentResolver: ContentResolver, uri: Uri) {
        // https://developer.android.com/training/data-storage/shared/documents-files#open
        val parcelFileDescriptor: ParcelFileDescriptor? =
            contentResolver.openFileDescriptor(uri, "r")
        if (parcelFileDescriptor != null) {
            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            val fileInputStream = FileInputStream(fileDescriptor)
            val buffer: ByteArray = fileInputStream.readBytes()
            fileInputStream.close()
            viewModel.fetchUploadedTorrent(buffer)
        } else {
            if (BuildConfig.DEBUG)
                Log.e(
                    "NewDownloadFragment",
                    "Torrent conversion: Error getting parcelFileDescriptor -> null"
                )
        }

    }

    private fun downloadTorrent(uri: Uri) {
        val nameRegex= "/([^/]+.torrent)\$"
        val m: Matcher = Pattern.compile(nameRegex).matcher(uri.toString())
        val torrentName = if(m.find())m.group(1) else null
        if (!torrentName.isNullOrBlank() && context!=null) {
            val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request: DownloadManager.Request = DownloadManager.Request(uri)
                .setTitle(getString(R.string.unchained_torrent_download))
                .setDescription(getString(R.string.temporary_torrent_download))
                .setDestinationInExternalFilesDir(requireContext(), Environment.DIRECTORY_DOWNLOADS,torrentName)
            val downloadID = manager.enqueue(request)
            activityViewModel.setDownload(downloadID, torrentName)
        }

    }
}

interface NewDownloadListener {
    fun onLoadTorrentClick()
}