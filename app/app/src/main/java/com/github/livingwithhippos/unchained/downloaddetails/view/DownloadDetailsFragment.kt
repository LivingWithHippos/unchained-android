package com.github.livingwithhippos.unchained.downloaddetails.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DownloadDetailsFragment : Fragment() {

    private val viewModel: DownloadDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_details, container, false)
    }
}