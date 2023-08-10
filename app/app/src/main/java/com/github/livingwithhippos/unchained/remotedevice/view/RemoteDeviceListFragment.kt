package com.github.livingwithhippos.unchained.remotedevice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteDeviceListBinding
import com.github.livingwithhippos.unchained.lists.view.DownloadKeyProvider
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.user.viewmodel.UserProfileViewModel
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteDeviceListFragment : UnchainedFragment(), DeviceListListener {

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRemoteDeviceListBinding.inflate(inflater, container, false)

        val deviceAdapter = RemoteDeviceListAdapter(this)
        binding.rvDeviceList.adapter = deviceAdapter

        val deviceTracker: SelectionTracker<RemoteDevice> =
            SelectionTracker.Builder(
                "deviceListSelection",
                binding.rvDeviceList,
                DeviceKeyProvider(deviceAdapter),
                DataBindingDetailsLookup(binding.rvDeviceList),
                StorageStrategy.createParcelableStorage(RemoteDevice::class.java)
            )
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        deviceAdapter.tracker = deviceTracker

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            deviceAdapter.submitList(it)
        }
        
        viewModel.fetchRemoteDevices()

        return binding.root
    }

    override fun onClick(item: RemoteDevice) {
        TODO("Not yet implemented")
    }
}