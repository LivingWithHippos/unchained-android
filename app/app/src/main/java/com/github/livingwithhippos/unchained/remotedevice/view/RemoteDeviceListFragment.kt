package com.github.livingwithhippos.unchained.remotedevice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.customview.StatItem
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteDeviceListBinding
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceEvent
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteDeviceListFragment : UnchainedFragment(), DeviceListListener {

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
                    StorageStrategy.createParcelableStorage(RemoteDevice::class.java))
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        deviceAdapter.tracker = deviceTracker

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DeviceEvent.AllDevicesAndServices -> {
                    deviceAdapter.submitList(it.itemsMap.keys.toList())
                    val stats = deviceToStats(it.itemsMap)
                    binding.remoteDeviceStats.adapter.submitList(stats)
                }
                is DeviceEvent.AllDevices -> deviceAdapter.submitList(it.devices)
                is DeviceEvent.DeletedAll -> viewModel.fetchRemoteDevices()
                else -> {}
            }
        }

        viewModel.fetchDevicesAndServices()

        binding.fabDevicesAction.setOnClickListener { showMenu(it, R.menu.devices_list_action) }

        return binding.root
    }

    private fun deviceToStats(dataMap: Map<RemoteDevice, List<RemoteService>>): List<StatItem> {
        return listOf(
            StatItem(
                label = getString(R.string.remote_devices),
                content = dataMap.size.toString(),
                caption = "",
                icon = R.drawable.icon_devices),
            StatItem(
                label = getString(R.string.services),
                content = dataMap.values.size.toString(),
                caption = "",
                icon = R.drawable.icon_service))
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.new_remote_device -> {
                    val action =
                        RemoteDeviceListFragmentDirections
                            .actionRemoteDeviceListFragmentToRemoteDeviceFragment()
                    findNavController().navigate(action)
                    true
                }
                R.id.delete_all_devices -> {
                    showConfirmationDialog()
                    true
                }
                else -> {
                    false
                }
            }
        }

        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    private fun showConfirmationDialog() {
        val builder: AlertDialog.Builder? = activity?.let { AlertDialog.Builder(it) }
        builder
            ?.setMessage(R.string.dialog_confirm_action)
            ?.setTitle(R.string.delete_all)
            ?.setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteAllDevices() }
            ?.setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }

    override fun onDeviceClick(item: RemoteDevice) {
        val action =
            RemoteDeviceListFragmentDirections.actionRemoteDeviceListFragmentToRemoteDeviceFragment(
                item)
        findNavController().navigate(action)
    }
}