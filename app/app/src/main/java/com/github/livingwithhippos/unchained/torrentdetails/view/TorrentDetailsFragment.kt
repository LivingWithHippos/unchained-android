package com.github.livingwithhippos.unchained.torrentdetails.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.FragmentDownloadDetailsBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewmodel

class TorrentDetailsFragment : Fragment() {

    private val viewModel: TorrentDetailsViewmodel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val torrentBinding = FragmentDownloadDetailsBinding.inflate(inflater, container, false)
        return torrentBinding.root
    }
}