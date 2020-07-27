package com.github.livingwithhippos.unchained.newdownload.view

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onLoadTorrentClick() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/x-bittorrent"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }
}

interface NewDownloadListener {
    fun onLoadTorrentClick()
}