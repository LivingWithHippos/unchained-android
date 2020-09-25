package com.github.livingwithhippos.unchained.newdownload.view

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import com.github.livingwithhippos.unchained.utilities.extension.runRippleAnimation
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.FileInputStream

/**
 * A simple [UnchainedFragment] subclass.
 * Allow the user to create a new download from a link or a torrent file.
 */
@AndroidEntryPoint
class NewDownloadFragment : UnchainedFragment(), NewDownloadListener {

    private val viewModel: NewDownloadViewModel by viewModels()

    val args: NewDownloadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadBinding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        downloadBinding.listener = this

        viewModel.linkLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { linkDetails ->
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(
                        linkDetails
                    )
                findNavController().navigate(action)
            }
        })

        viewModel.torrentLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { torrent ->
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToTorrentDetailsFragment(
                        torrent.id
                    )
                findNavController().navigate(action)
            }
        })

        viewModel.apiErrorLiveData.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { error ->
                when (error.errorCode) {
                    -1 -> showToast(R.string.internal_error)
                    1 -> showToast(R.string.missing_parameter)
                    2 -> showToast(R.string.bad_parameter_value)
                    3 -> showToast(R.string.unknown_method)
                    4 -> showToast(R.string.method_not_allowed)
                    // what is this error for?
                    5 -> showToast(R.string.slow_down)
                    6 -> showToast(R.string.resource_unreachable)
                    7 -> showToast(R.string.resource_not_found)
                    //todo: check these
                    8 -> {
                        showToast(R.string.bad_token)
                        activityViewModel.setUnauthenticated()
                    }
                    9 -> showToast(R.string.permission_denied)
                    10 -> showToast(R.string.tfa_needed)
                    11 -> showToast(R.string.tfa_pending)
                    12 -> {
                        showToast(R.string.invalid_login)
                        activityViewModel.setUnauthenticated()
                    }
                    13 -> showToast(R.string.invalid_password)
                    14 -> {
                        showToast(R.string.account_locked)
                        activityViewModel.setUnauthenticated()
                    }
                    15 -> showToast(R.string.account_not_activated)
                    16 -> showToast(R.string.unsupported_hoster)
                    17 -> showToast(R.string.hoster_in_maintenance)
                    18 -> showToast(R.string.hoster_limit_reached)
                    19 -> showToast(R.string.hoster_temporarily_unavailable)
                    20 -> showToast(R.string.hoster_not_available_for_free_users)
                    21 -> showToast(R.string.too_many_active_downloads)
                    22 -> showToast(R.string.ip_Address_not_allowed)
                    23 -> showToast(R.string.traffic_exhausted)
                    24 -> showToast(R.string.file_unavailable)
                    25 -> showToast(R.string.service_unavailable)
                    26 -> showToast(R.string.upload_too_big)
                    27 -> showToast(R.string.upload_error)
                    28 -> showToast(R.string.file_not_allowed)
                    29 -> showToast(R.string.torrent_too_big)
                    30 -> showToast(R.string.torrent_file_invalid)
                    31 -> showToast(R.string.action_already_done)
                    32 -> showToast(R.string.image_resolution_error)
                    33 -> showToast(R.string.torrent_already_active)
                    else -> showToast(R.string.error_unrestricting_download)
                }
                // re enable buttons to let the user take other actions
                //todo: this needs to be done also for other errors. maybe throw another error from the viewmodel
                downloadBinding.bUnrestrict.isEnabled = true
                downloadBinding.bLoadTorrent.isEnabled = true
            }
        })

        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {

            val authState = activityViewModel.authenticationState.value?.peekContent()
            if (authState == MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                val link: String = downloadBinding.tiLink.text.toString().trim()
                if (link.isWebUrl()) {

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

                } else {
                    if (link.isMagnet()) {
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false
                        viewModel.fetchAddedMagnet(link)
                    } else {
                        if (link.isBlank())
                            showToast(R.string.please_insert_url)
                        else
                            showToast(R.string.invalid_url)

                    }
                }
            }
            else
                showToast(R.string.premium_needed)
        }

        downloadBinding.bPasteLink.setOnClickListener {
            val pasteText = getClipboardText()

            if (pasteText.isWebUrl() || pasteText.isMagnet())
                downloadBinding.tiLink.setText(pasteText, TextView.BufferType.EDITABLE)
            else
                showToast(R.string.invalid_url)
        }

        downloadBinding.bPastePassword.setOnClickListener {
            val pasteText = getClipboardText()
            downloadBinding.tePassword.setText(pasteText, TextView.BufferType.EDITABLE)
        }

        // we must make this value null by default because it's the first fragment of the nav graph
        if (!args.links.isNullOrEmpty()) {
            //todo: check for multiple files
            downloadBinding.tiLink.setText(args.links!!.first(), TextView.BufferType.EDITABLE)
            // run the ripple animation on the unrestrict button
            downloadBinding.bUnrestrict.runRippleAnimation()
        }

        return downloadBinding.root
    }

    private val getTorrent: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // todo: check the context instead of using requireContext
                loadTorrent(requireContext().contentResolver, uri)
            } else {
                showToast(R.string.error_loading_torrent)
            }
        }

    override fun onLoadTorrentClick() {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == MainActivityViewModel.AuthenticationState.AUTHENTICATED)
            getTorrent.launch("application/x-bittorrent")
        else
            showToast(R.string.premium_needed)
    }

    private fun loadTorrent(contentResolver: ContentResolver, uri: Uri) {
        // todo: load torrent and call put function from torrents api
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
}

interface NewDownloadListener {
    fun onLoadTorrentClick()
}