package com.github.livingwithhippos.unchained.remotedevice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteDeviceBinding
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.search.view.SearchItemFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteDeviceFragment : UnchainedFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRemoteDeviceBinding.inflate(inflater, container, false)


        return binding.root
    }
}