package com.github.livingwithhippos.unchained.newdownload.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewDownloadFragment : Fragment() {

    private val viewModel: NewDownloadViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val downloadBinding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        downloadBinding.list = listOf(getString(R.string.yes),getString(R.string.no))
        return downloadBinding.root
    }

}