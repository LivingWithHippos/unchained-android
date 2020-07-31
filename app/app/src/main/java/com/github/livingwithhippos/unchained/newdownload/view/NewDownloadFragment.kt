package com.github.livingwithhippos.unchained.newdownload.view

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.FileInputStream
import java.util.regex.Matcher
import java.util.regex.Pattern


@AndroidEntryPoint
class NewDownloadFragment : Fragment(), NewDownloadListener {

    private val viewModel: NewDownloadViewModel by viewModels()

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
        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {
            val link: String = downloadBinding.tiLink.text.toString().trim()
            if (Patterns.WEB_URL.matcher(link).matches()) {

                downloadBinding.bUnrestrict.isEnabled = false
                downloadBinding.bLoadTorrent.isEnabled = false

                var password: String? = downloadBinding.etPassword.text.toString()
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
                val m: Matcher = Pattern.compile(MAGNET_PATTERN).matcher(link)
                if (m.lookingAt()) {
                    downloadBinding.bUnrestrict.isEnabled = false
                    downloadBinding.bLoadTorrent.isEnabled = false
                    viewModel.fetchAddedMagnet(link)
                }
                else
                    showToast(R.string.invalid_url)
            }

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
        getTorrent.launch("application/x-bittorrent")
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