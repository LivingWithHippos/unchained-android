package com.github.livingwithhippos.unchained.downloaddetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadDetailsFragment : Fragment(), DownloadDetailsListener {

    private val viewModel: DownloadDetailsViewModel by viewModels()

    val args: DownloadDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        detailsBinding.details = args.details
        detailsBinding.listener = this

        //todo: change other livedata observation like this or in their own method
        viewModel.streamLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                detailsBinding.stream = it
            }
        })

        return detailsBinding.root
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        openExternalWebPage(url)
    }

    override fun onLoadStreamsClick(id: String) {
        viewModel.fetchStreamingInfo(id)
    }

    override fun onPlayStreamsClick(link: String) {
        openExternalWebPage(link)
    }
}

interface DownloadDetailsListener {
    fun onCopyClick(text: String)
    fun onOpenClick(url: String)
    fun onLoadStreamsClick(id: String)
    fun onPlayStreamsClick(link: String)
}