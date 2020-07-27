package com.github.livingwithhippos.unchained.newdownload.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.github.livingwithhippos.unchained.utilities.OPEN_DOCUMENT_REQUEST_CODE
import com.github.livingwithhippos.unchained.utilities.REMOTE_TRAFFIC_ON
import com.github.livingwithhippos.unchained.utilities.showToast
import dagger.hilt.android.AndroidEntryPoint

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
            it.getContentIfNotHandled()?.let{ linkDetails ->
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(linkDetails)
                findNavController().navigate(action)
            }
        })
        // add the unrestrict button listener
        downloadBinding.bUnrestrict.setOnClickListener {
            val link: String = downloadBinding.tiLink.text.toString().trim()
            if (Patterns.WEB_URL.matcher(link).matches()) {

                downloadBinding.bUnrestrict.isEnabled = false

                var password: String? = downloadBinding.etPassword.text.toString()
                // we don't pass the password if it is blank.
                // N.B. it won't work if your password is made up of spaces but then again you deserve it
                if (password == null || password.isBlank())
                    password = null
                val remote: Int? =
                    if (downloadBinding.switchRemote.isEnabled) REMOTE_TRAFFIC_ON else null

                viewModel.fetchUnrestrictedLink(
                    link,
                    password,
                    remote
                )

            } else
                showToast(R.string.invalid_url)

        }

        return downloadBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private val getTorrent: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Handle the returned Uri
    }

    override fun onLoadTorrentClick() {
        getTorrent.launch("application/x-bittorrent")
    }
}

interface NewDownloadListener {
    fun onLoadTorrentClick()
}