package com.github.livingwithhippos.unchained.newdownload.view

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.livingwithhippos.unchained.R

class NewDownloadFragment : Fragment() {

    companion object {
        fun newInstance() = NewDownloadFragment()
    }

    private lateinit var viewModel: NewDownloadViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.new_download_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(NewDownloadViewModel::class.java)
        // TODO: Use the ViewModel
    }

}