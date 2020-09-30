package com.github.livingwithhippos.unchained.downloaddetails.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * A simple [UnchainedFragment] subclass.
 * It is capable of showing the details of a [DownloadItem]
 */
@AndroidEntryPoint
class DownloadDetailsFragment : UnchainedFragment(), DownloadDetailsListener {

    private val viewModel: DownloadDetailsViewModel by viewModels()

    private val args: DownloadDetailsFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.download_details_bar, menu)
        super.onCreateOptionsMenu(menu,inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                // the delete operation is observed from the viewModel
                viewModel.deleteDownload(args.details.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val detailsBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)

        detailsBinding.details = args.details
        detailsBinding.listener = this

        viewModel.streamLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                detailsBinding.stream = it
            }
        })

        viewModel.deletedDownloadLiveData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled().let {
                // todo: check returned value (it)
                activity?.baseContext?.showToast(R.string.download_removed)
                // if deleted go back
                activity?.onBackPressed()
            }
        })

        return detailsBinding.root
    }

    override fun onCopyClick(text: String) {
        copyToClipboard("Real-Debrid Download Link", text)
        context?.showToast(R.string.link_copied)
    }

    override fun onOpenClick(url: String) {
        openExternalWebPage(url)
    }

    override fun onLoadStreamsClick(id: String) {
        lifecycleScope.launch {
            if (activityViewModel.isTokenPrivate()) {
                viewModel.fetchStreamingInfo(id)
            } else
                context?.showToast(R.string.api_needs_private_token)

        }
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