package com.github.livingwithhippos.unchained.newdownload.view

import android.annotation.SuppressLint
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.newdownload.viewmodel.Link
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.CONTAINER_EXTENSION_PATTERN
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.getFileName
import com.github.livingwithhippos.unchained.utilities.extension.isContainerWebLink
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A simple [UnchainedFragment] subclass.
 * Allow the user to create a new download from a link or a torrent file.
 */
@AndroidEntryPoint
class NewDownloadFragment : UnchainedFragment() {

    // if we receive an intent and new download is already selected and showing a DownloadDetailsFragment, it may not trigger the observers in this class
    private val viewModel: NewDownloadViewModel by viewModels()

    private val args: NewDownloadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        setupObservers(binding)
        setupClickListeners(binding)
        setupArgs(binding)

        return binding.root
    }

    private fun setupObservers(binding: NewDownloadFragmentBinding) {

        viewModel.downloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver { linkDetails ->
                // new download item, alert the list fragment that it needs updating
                activityViewModel.setListState(ListState.UpdateDownload)
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
                // new folder list, alert the list fragment that it needs updating
                activityViewModel.setListState(ListState.UpdateDownload)
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                        folder = folder,
                        torrent = null,
                        linkList = null
                    )
                findNavController().navigate(action)
            }
        )

        viewModel.linkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { link ->
                when (link) {
                    is Link.Container -> {
                        // new container, alert the list fragment that it needs updating
                        activityViewModel.setListState(ListState.UpdateDownload)
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                                linkList = link.links.toTypedArray(),
                                folder = null,
                                torrent = null
                            )
                        findNavController().navigate(action)
                    }
                    is Link.RetrievalError -> {
                        viewModel.postMessage(getString(R.string.error_parsing_container))
                    }
                    is Link.Torrent -> {
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadFragmentToTorrentProcessingFragment(
                                torrentID = link.upload.id
                            )
                        findNavController().navigate(action)
                    }
                    else -> {
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
                    loadTorrent(binding, uri)
            }
        )

        viewModel.networkExceptionLiveData.observe(
            viewLifecycleOwner,
            EventObserver { exception ->

                // re-enable the buttons to allow the user to take new actions
                enableButtons(binding, true)

                when (exception) {
                    is APIError -> {
                        // error codes outside the known range will return unknown error
                        val errorCode = exception.errorCode ?: -2
                        val errorMessage = requireContext().getApiErrorMessage(errorCode)
                        // manage the api error result
                        when (exception.errorCode) {
                            -1, 1 -> {
                                viewModel.postMessage(errorMessage)
                            }
                            // since here we monitor new downloads, use a less generic, custom message
                            2 -> viewModel.postMessage(getString(R.string.unsupported_hoster))
                            in 3..7 -> viewModel.postMessage(errorMessage)
                            8 -> {
                                viewModel.postMessage(getString(R.string.refreshing_token))
                                // try refreshing the token
                                if (activityViewModel.getAuthenticationMachineState() is FSMAuthenticationState.AuthenticatedOpenToken)
                                    activityViewModel.transitionAuthenticationMachine(
                                        FSMAuthenticationEvent.OnExpiredOpenToken
                                    )
                                else
                                    Timber.e("Asked for a refresh while in a wrong state: ${activityViewModel.getAuthenticationMachineState()}")
                            }
                            in 10..15 -> {
                                viewModel.postMessage(errorMessage)
                                when (activityViewModel.getAuthenticationMachineState()) {
                                    FSMAuthenticationState.AuthenticatedOpenToken, FSMAuthenticationState.AuthenticatedPrivateToken, FSMAuthenticationState.RefreshingOpenToken -> {
                                        activityViewModel.transitionAuthenticationMachine(
                                            FSMAuthenticationEvent.OnAuthenticationError
                                        )
                                    }
                                    else -> {
                                        Timber.e("Asked for logout while in a wrong state: ${activityViewModel.getAuthenticationMachineState()}")
                                    }
                                }
                            }
                            9 -> {
                                // todo: check if permission denied (code 9) is related only to asking for magnet without premium or other stuff too
                                // we use this because permission denied is not clear
                                viewModel.postMessage(getString(R.string.premium_needed))
                            }
                            else -> {
                                viewModel.postMessage(errorMessage)
                            }
                        }
                    }
                    is EmptyBodyError -> {
                        // call successful, fit to singular api case
                    }
                    is NetworkError -> {
                        // todo: alert the user according to the different network error
                        viewModel.postMessage(getString(R.string.network_error))
                    }
                }
            }
        )

        @SuppressLint("ShowToast")
        val currentToast: Toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT)
        var lastToastTime = System.currentTimeMillis()

        viewModel.toastLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                lifecycleScope.launch {
                    currentToast.cancel()
                    // if we call this too soon between toasts we'll miss some
                    if (System.currentTimeMillis() - lastToastTime < 1000L)
                        delay(1000)
                    currentToast.setText(it)
                    currentToast.show()
                    lastToastTime = System.currentTimeMillis()
                }
            }
        )
    }

    private fun setupClickListeners(binding: NewDownloadFragmentBinding) {
        // add the unrestrict button listener
        binding.bUnrestrict.setOnClickListener {
            val authState = activityViewModel.getAuthenticationMachineState()
            if (authState is FSMAuthenticationState.AuthenticatedPrivateToken || authState is FSMAuthenticationState.AuthenticatedOpenToken) {
                val link: String = binding.tiLink.text.toString().trim()
                when {
                    // this must be before the link.isWebUrl() check or it won't trigger
                    link.isTorrent() -> {
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadFragmentToTorrentProcessingFragment(
                                link = link
                            )
                        findNavController().navigate(action)

                        // viewModel.postMessage(getString(R.string.loading_torrent))
                        // enableButtons(binding, false)
                        /**
                         * DownloadManager does not support insecure (https) links anymore
                         * to add support for it, follow these instructions
                         * [https://stackoverflow.com/a/50834600]
                         val secureLink = if (link.startsWith("http://")) link.replaceFirst(
                         "http:",
                         "https:"
                         ) else link
                         downloadTorrent(Uri.parse(secureLink))
                         */
                        // downloadTorrentToCache(binding, link)
                    }
                    link.isWebUrl() -> {
                        viewModel.postMessage(getString(R.string.loading_host_link))
                        enableButtons(binding, false)

                        var password: String? = binding.tePassword.text.toString()
                        // we don't pass the password if it is blank.
                        // N.B. it won't work if your password is made up of spaces but then again you deserve it
                        if (password.isNullOrBlank())
                            password = null
                        val remote: Int? =
                            if (binding.switchRemote.isChecked) REMOTE_TRAFFIC_ON else null

                        viewModel.fetchUnrestrictedLink(
                            link,
                            password,
                            remote
                        )
                    }
                    link.isMagnet() -> {
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadFragmentToTorrentProcessingFragment(
                                link = link
                            )
                        findNavController().navigate(action)
                        // viewModel.postMessage(getString(R.string.loading_magnet_link))
                        // enableButtons(binding, false)
                        // viewModel.fetchAddedMagnet(link)
                    }
                    link.isBlank() -> {
                        viewModel.postMessage(getString(R.string.please_insert_url))
                    }
                    link.isContainerWebLink() -> {
                        viewModel.unrestrictContainer(link)
                    }
                    link.split("\n").firstOrNull()?.trim()?.isWebUrl() == true -> {
                        // todo: support list of magnets/torrents
                        val splitLinks: List<String> =
                            link.split("\n").map { it.trim() }.filter { it.length > 10 }
                        viewModel.postMessage(getString(R.string.loading))
                        enableButtons(binding, false)

                        // new folder list, alert the list fragment that it needs updating
                        activityViewModel.setListState(ListState.UpdateDownload)
                        val action =
                            NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                                folder = null,
                                torrent = null,
                                linkList = splitLinks.toTypedArray()
                            )
                        findNavController().navigate(action)
                    }
                    else -> {
                        viewModel.postMessage(getString(R.string.invalid_url))
                    }
                }
            } else
                viewModel.postMessage(getString(R.string.premium_needed))
        }

        binding.bPasteLink.setOnClickListener {
            val pasteText = getClipboardText()

            if (
                pasteText.isWebUrl() ||
                pasteText.isMagnet() ||
                pasteText.isTorrent() ||
                pasteText.split("\n").firstOrNull()?.trim()?.isWebUrl() == true
            )
                binding.tiLink.setText(pasteText, TextView.BufferType.EDITABLE)
            else
                viewModel.postMessage(getString(R.string.invalid_url))
        }

        binding.bPastePassword.setOnClickListener {
            val pasteText = getClipboardText()
            binding.tePassword.setText(pasteText, TextView.BufferType.EDITABLE)
        }

        val filePicker: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    val fileName = uri.getFileName(requireContext())
                    if (fileName.endsWith(".torrent", ignoreCase = true))
                        loadTorrent(binding, uri)
                    else {
                        if (CONTAINER_EXTENSION_PATTERN.toRegex().containsMatchIn(fileName))
                            loadContainer(binding, uri)
                        else
                            viewModel.postMessage(getString(R.string.unsupported_file))
                    }
                }
                /*
                * if it's null the user didn't pick a file, no message needed
                else {
                context?.showToast(R.string.error_loading_file)
                }
                 */
            }

        binding.bUploadFile.setOnClickListener {

            when (activityViewModel.getAuthenticationMachineState()) {
                FSMAuthenticationState.AuthenticatedOpenToken, FSMAuthenticationState.AuthenticatedPrivateToken, FSMAuthenticationState.RefreshingOpenToken -> {
                    filePicker.launch("*/*")
                }
                else -> {
                    viewModel.postMessage(getString(R.string.premium_needed))
                }
            }
        }
    }

    private fun setupArgs(binding: NewDownloadFragmentBinding) {

        args.externalUri?.let { link ->
            when (link.scheme) {
                SCHEME_MAGNET -> {
                    viewModel.postMessage(getString(R.string.loading_magnet_link))
                    // set as text input text
                    binding.tiLink.setText(
                        link.toString(),
                        TextView.BufferType.EDITABLE
                    )
                    // simulate button click
                    binding.bUnrestrict.performClick()
                }
                SCHEME_CONTENT, SCHEME_FILE -> {

                    var handled = false

                    requireContext().contentResolver.query(
                        link,
                        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { metaCursor ->
                        if (metaCursor.moveToFirst()) {
                            val fileName = metaCursor.getString(0)
                            Timber.d("Torrent shared file found: $fileName")
                            when {
                                // check if it's a container
                                CONTAINER_EXTENSION_PATTERN.toRegex().matches(fileName) -> {
                                    handled = true
                                    loadContainer(binding, link)
                                }
                                fileName.endsWith(".torrent", ignoreCase = true) -> {
                                    handled = true
                                    loadTorrent(binding, link)
                                }
                            }
                        }
                    }

                    if (!handled) {
                        when {
                            // check if it's a container
                            CONTAINER_EXTENSION_PATTERN.toRegex().matches(link.path ?: "") -> {
                                loadContainer(binding, link)
                            }
                            link.path?.endsWith(".torrent", ignoreCase = true) == true -> {
                                loadTorrent(binding, link)
                            }
                            else -> Timber.e("Unsupported content/file passed to NewDownloadFragment: $link")
                        }
                    } else {
                        // do nothing
                    }
                }
                SCHEME_HTTP, SCHEME_HTTPS -> {
                    // set as text input text
                    binding.tiLink.setText(
                        link.toString(),
                        TextView.BufferType.EDITABLE
                    )
                    // simulate button click
                    binding.bUnrestrict.performClick()
                }
                else -> {
                    // shouldn't trigger
                    Timber.e("Unknown Uri shared to NewDownloadFragment: ${link.scheme} - ${link.path}")
                }
            }
        }
    }

    private fun loadCachedTorrent(
        binding: NewDownloadFragmentBinding,
        cacheDir: File,
        fileName: String
    ) {
        try {
            viewModel.postMessage(getString(R.string.loading_torrent_file))
            val cacheFile = File(cacheDir, fileName)
            cacheFile.inputStream().use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is java.io.FileNotFoundException -> {
                    Timber.e("Torrent conversion: file not found: ${exception.message}")
                }
                is IOException -> {
                    Timber.e("Torrent conversion: IOException error getting the file: ${exception.message}")
                }
                else -> {
                    Timber.e("Torrent conversion: Other error getting the file: ${exception.message}")
                }
            }
            enableButtons(binding, true)
            viewModel.postMessage(getString(R.string.error_loading_torrent))
        }
    }

    private fun loadTorrent(binding: NewDownloadFragmentBinding, uri: Uri) {
        // https://developer.android.com/training/data-storage/shared/documents-files#open
        try {
            viewModel.postMessage(getString(R.string.loading_torrent_file))
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is java.io.FileNotFoundException -> {
                    Timber.e("Torrent conversion: file not found: ${exception.message}")
                }
                is IOException -> {
                    Timber.e("Torrent conversion: IOException error getting the file: ${exception.message}")
                }
                else -> {
                    Timber.e("Torrent conversion: Other error getting the file: ${exception.message}")
                }
            }
            enableButtons(binding, true)
            viewModel.postMessage(getString(R.string.error_loading_torrent))
        }
    }

    private fun enableButtons(binding: NewDownloadFragmentBinding, enabled: Boolean = true) {
        binding.bUnrestrict.isEnabled = enabled
        binding.bUploadFile.isEnabled = enabled
    }

    private fun loadContainer(binding: NewDownloadFragmentBinding, uri: Uri) {
        try {
            viewModel.postMessage(getString(R.string.loading_container_file))
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.uploadContainer(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is java.io.FileNotFoundException -> {
                    Timber.e("Container conversion: file not found: ${exception.message}")
                }
                is IOException -> {
                    Timber.e("Container conversion: IOException error getting the file: ${exception.message}")
                }
                else -> {
                    Timber.e("Container conversion: Other error getting the file: ${exception.message}")
                }
            }
            enableButtons(binding, true)
            viewModel.postMessage(getString(R.string.error_loading_file))
        }
    }

    private fun downloadTorrentToCache(binding: NewDownloadFragmentBinding, link: String) {
        val nameRegex = "/([^/]+\\.torrent)\$"
        val m: Matcher = Pattern.compile(nameRegex).matcher(link)
        val torrentName = if (m.find()) m.group(1) else null
        val cacheDir = context?.cacheDir
        if (!torrentName.isNullOrBlank() && cacheDir != null) {
            lifecycleScope.launch {
                activityViewModel.downloadFileToCache(link, torrentName, cacheDir).observe(
                    viewLifecycleOwner
                ) {
                    when (it) {
                        is DownloadResult.End -> {
                            loadCachedTorrent(binding, cacheDir, it.fileName)
                        }
                        DownloadResult.Failure -> {
                            viewModel.postMessage(
                                getString(
                                    R.string.download_not_started_format,
                                    torrentName
                                )
                            )
                        }
                        is DownloadResult.Progress -> {
                            Timber.d("$torrentName progress: ${it.percent}")
                        }
                        DownloadResult.WrongURL -> {
                            viewModel.postMessage(
                                getString(
                                    R.string.download_not_started_format,
                                    torrentName
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
