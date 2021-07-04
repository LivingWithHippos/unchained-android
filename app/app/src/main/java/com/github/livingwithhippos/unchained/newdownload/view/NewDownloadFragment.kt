package com.github.livingwithhippos.unchained.newdownload.view

import android.app.DownloadManager
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.newdownload.viewmodel.Link
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.utilities.CONTAINER_EXTENSION_PATTERN
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.downloadFile
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.isContainerWebLink
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import com.github.livingwithhippos.unchained.utilities.extension.runRippleAnimation
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import timber.log.Timber
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A simple [UnchainedFragment] subclass.
 * Allow the user to create a new download from a link or a torrent file.
 */
@AndroidEntryPoint
class NewDownloadFragment : UnchainedFragment(), NewDownloadListener {

    // todo: switch to the navigation scoped viewmodel to manage the transition between fragments
    // if we receive an intent and new download is already selected and showing a DownloadDetailsFragment, it may not trigger the observers in this class
    private val viewModel: NewDownloadViewModel by viewModels()

    private val args: NewDownloadFragmentArgs by navArgs()

    private var _binding: NewDownloadFragmentBinding? = null
    val downloadBinding get() = _binding!!

    // used to simulate a debounce effect while typing on the search bar
    var queryJob: Job? = null

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        downloadBinding.listener = this

        viewModel.linkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { linkDetails ->
                // new download item, alert the list fragment that it needs updating
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_DOWNLOAD)
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(
                        linkDetails
                    )
                findNavController().navigate(action)
            }
        )

        viewModel.folderLiveData.observe(
            viewLifecycleOwner,
            EventObserver { folder ->

                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                        folder = folder,
                        torrent = null,
                        linkList = null
                    )
                findNavController().navigate(action)
            }
        )

        viewModel.torrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver { torrent ->
                // new torrent item, alert the list fragment that it needs updating
                activityViewModel.setListState(ListsTabFragment.ListState.UPDATE_TORRENT)
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToTorrentDetailsFragment(
                        torrent.id
                    )
                findNavController().navigate(action)
            }
        )

        viewModel.containerLiveData.observe(
            viewLifecycleOwner,
            EventObserver { link ->
                when (link) {
                    is Link.Container -> {
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                                linkList = link.links.toTypedArray(),
                                folder = null,
                                torrent = null
                            )
                        findNavController().navigate(action)
                    }
                    is Link.RetrievalError -> {
                        context?.showToast(R.string.error_parsing_container)
                    }
                    else -> {
                    }
                }
            }
        )

        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {

            val authState = activityViewModel.authenticationState.value?.peekContent()
            if (authState == AuthenticationState.AUTHENTICATED) {
                val link: String = downloadBinding.tiLink.text.toString().trim()
                when {
                    // this must be before the link.isWebUrl() check
                    link.isTorrent() -> {
                        context?.showToast(R.string.loading_torrent)
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false
                        /**
                         * DownloadManager does not support insecure (https) links anymore
                         * to add support for it, follow these instructions
                         * [https://stackoverflow.com/a/50834600]
                         */
                        val secureLink = if (link.startsWith("http://")) link.replaceFirst(
                            "http:",
                            "https:"
                        ) else link
                        downloadTorrent(Uri.parse(secureLink))
                    }
                    link.isWebUrl() -> {
                        context?.showToast(R.string.loading_host_link)
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false

                        var password: String? = downloadBinding.tePassword.text.toString()
                        // we don't pass the password if it is blank.
                        // N.B. it won't work if your password is made up of spaces but then again you deserve it
                        if (password.isNullOrBlank())
                            password = null
                        val remote: Int? =
                            if (downloadBinding.switchRemote.isChecked) REMOTE_TRAFFIC_ON else null

                        viewModel.fetchUnrestrictedLink(
                            link,
                            password,
                            remote
                        )
                    }
                    link.isMagnet() -> {
                        context?.showToast(R.string.loading_magnet_link)
                        downloadBinding.bUnrestrict.isEnabled = false
                        downloadBinding.bLoadTorrent.isEnabled = false
                        viewModel.fetchAddedMagnet(link)
                    }
                    link.isBlank() -> {
                        context?.showToast(R.string.please_insert_url)
                    }
                    link.isContainerWebLink() -> {
                        viewModel.unrestrictContainer(link)
                    }
                    else -> {
                        context?.showToast(R.string.invalid_url)
                    }
                }
            } else
                context?.showToast(R.string.premium_needed)
        }

        downloadBinding.bPasteLink.setOnClickListener {
            val pasteText = getClipboardText()

            if (pasteText.isWebUrl() || pasteText.isMagnet() || pasteText.isTorrent())
                downloadBinding.tiLink.setText(pasteText, TextView.BufferType.EDITABLE)
            else
                context?.showToast(R.string.invalid_url)
        }

        downloadBinding.bPastePassword.setOnClickListener {
            val pasteText = getClipboardText()
            downloadBinding.tePassword.setText(pasteText, TextView.BufferType.EDITABLE)
        }

        // we must make this value null by default because it's the first fragment of the nav graph
        if (args.link != null) {
            downloadBinding.tiLink.setText(args.link, TextView.BufferType.EDITABLE)
            // run the ripple animation on the unrestrict button
            downloadBinding.bUnrestrict.runRippleAnimation()
        }

        // we don't need to update the list fragment because when opened externally it gets loaded for the first time anyway
        // this will change when we'll be able to use the already loaded activity instead of creating a new one
        activityViewModel.externalLinkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { link ->
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
                        when {
                            // check if it's a container
                            CONTAINER_EXTENSION_PATTERN.toRegex().matches(link.path ?: "") -> {
                                context?.showToast(R.string.loading_container_file)
                                loadContainer(link)
                            }
                            link.path?.endsWith(".torrent") == true -> {
                                context?.showToast(R.string.loading_torrent_file)
                                loadTorrent(link)
                            }
                            else -> Timber.e("Unsupported content/file passed to NewDownloadFragment")
                        }
                    }
                    SCHEME_HTTP, SCHEME_HTTPS -> {
                        if (!link.toString().endsWith(".torrent"))
                            context?.showToast(R.string.loading_host_link)

                        // set as text input text
                        downloadBinding.tiLink.setText(
                            link.toString(),
                            TextView.BufferType.EDITABLE
                        )
                        // simulate button click
                        downloadBinding.bUnrestrict.performClick()
                    }
                }
            }
        )

        activityViewModel.downloadedFileLiveData.observe(
            viewLifecycleOwner,
            EventObserver { fileID ->
                val uri = requireContext().getDownloadedFileUri(fileID)
                // no need to recheck the extension since it was checked on download
                // if (uri?.path?.endsWith(".torrent") == true)
                if (uri?.path != null)
                    loadTorrent(uri)
            }
        )

        viewModel.networkExceptionLiveData.observe(
            viewLifecycleOwner,
            EventObserver { exception ->

                // re-enable the buttons to allow the user to take new actions
                downloadBinding.bUnrestrict.isEnabled = true
                downloadBinding.bLoadTorrent.isEnabled = true

                when (exception) {
                    is APIError -> {
                        // error codes outside the known range will return unknown error
                        val errorCode = exception.errorCode ?: -2
                        // manage the api error result
                        when (exception.errorCode) {
                            -1, 1 -> context?.let {
                                it.showToast(it.getApiErrorMessage(errorCode))
                            }
                            // since here we monitor new downloads, use a less generic, custom message
                            2 -> context?.showToast(R.string.unsupported_hoster)
                            in 3..7 -> context?.let {
                                it.showToast(it.getApiErrorMessage(errorCode))
                            }
                            8 -> {
                                // try refreshing the token
                                context?.let {
                                    it.showToast(it.getApiErrorMessage(errorCode))
                                }
                                activityViewModel.setBadToken()
                                context?.showToast(R.string.refreshing_token)
                            }
                            in 9..15 -> {
                                context?.let {
                                    it.showToast(it.getApiErrorMessage(errorCode))
                                }
                                activityViewModel.setUnauthenticated()
                            }
                            else -> {
                                context?.let {
                                    it.showToast(it.getApiErrorMessage(errorCode))
                                }
                            }
                        }
                    }
                    is EmptyBodyError -> {
                        // call successful, fit to singular api case
                    }
                    is NetworkError -> {
                        // todo: alert the user according to the different network error
                        context?.showToast(R.string.network_error)
                    }
                }
            }
        )

        activityViewModel.notificationTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver { torrentID ->
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToTorrentDetailsFragment(
                        torrentID
                    )
                findNavController().navigate(action)
            }
        )

        return downloadBinding.root
    }

    private val torrentPicker: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                loadTorrent(uri)
            } else {
                context?.showToast(R.string.error_loading_file)
            }
        }

    private val containerPicker: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                loadContainer(uri)
            } else {
                context?.showToast(R.string.error_loading_file)
            }
        }

    override fun onLoadTorrentClick() {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED)
            torrentPicker.launch("application/x-bittorrent")
        else
            context?.showToast(R.string.premium_needed)
    }

    override fun onLoadContainerClick() {
        val authState = activityViewModel.authenticationState.value?.peekContent()
        if (authState == AuthenticationState.AUTHENTICATED)
            containerPicker.launch("*/*")
        else
            context?.showToast(R.string.premium_needed)
    }

    private fun loadTorrent(uri: Uri) {
        // https://developer.android.com/training/data-storage/shared/documents-files#open
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is IOException -> {
                    Timber.e("Torrent conversion: IOException error getting the file: ${exception.message}")
                }
                is java.io.FileNotFoundException -> {
                    Timber.e("Torrent conversion: file not found: ${exception.message}")
                }
                else -> {
                    Timber.e("Torrent conversion: Other error getting the file: ${exception.message}")
                }
            }
            downloadBinding.bUnrestrict.isEnabled = true
            downloadBinding.bLoadTorrent.isEnabled = true
            requireContext().showToast(R.string.error_loading_torrent)
        }
    }

    private fun loadContainer(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.uploadContainer(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is IOException -> {
                    Timber.e("Container conversion: IOException error getting the file: ${exception.message}")
                }
                is java.io.FileNotFoundException -> {
                    Timber.e("Container conversion: file not found: ${exception.message}")
                }
                else -> {
                    Timber.e("Container conversion: Other error getting the file: ${exception.message}")
                }
            }
            downloadBinding.bUnrestrict.isEnabled = true
            downloadBinding.bLoadTorrent.isEnabled = true
            requireContext().showToast(R.string.error_loading_file)
        }
    }

    private fun downloadTorrent(uri: Uri) {
        val nameRegex = "/([^/]+.torrent)\$"
        val m: Matcher = Pattern.compile(nameRegex).matcher(uri.toString())
        val torrentName = if (m.find()) m.group(1) else null
        if (!torrentName.isNullOrBlank() && context != null) {
            val manager =
                requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val queuedDownload = manager.downloadFile(
                uri = uri,
                title = getString(R.string.unchained_torrent_download),
                description = getString(R.string.temporary_torrent_download),
                fileName = torrentName
            )
            when (queuedDownload) {
                is EitherResult.Failure -> {
                    requireContext().showToast(
                        getString(
                            R.string.download_not_started_format,
                            torrentName
                        )
                    )
                }
                is EitherResult.Success -> {
                    activityViewModel.setDownload(queuedDownload.success)
                }
            }
        }
    }
}

interface NewDownloadListener {
    fun onLoadTorrentClick()
    fun onLoadContainerClick()
}
