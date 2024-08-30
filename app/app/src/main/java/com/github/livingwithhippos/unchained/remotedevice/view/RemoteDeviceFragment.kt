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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteDeviceBinding
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceEvent
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.utilities.DataBindingDetailsLookup
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemoteDeviceFragment : UnchainedFragment(), ServiceListListener {

    private val args: RemoteDeviceFragmentArgs by navArgs()

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRemoteDeviceBinding.inflate(inflater, container, false)

        val serviceAdapter = RemoteServiceListAdapter(this)
        binding.rvServiceList.adapter = serviceAdapter

        val serviceTracker: SelectionTracker<RemoteService> =
            SelectionTracker.Builder(
                    "serviceListSelection",
                    binding.rvServiceList,
                    ServiceKeyProvider(serviceAdapter),
                    DataBindingDetailsLookup(binding.rvServiceList),
                    StorageStrategy.createParcelableStorage(RemoteService::class.java))
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        serviceAdapter.tracker = serviceTracker

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DeviceEvent.DeviceServices -> {
                    serviceAdapter.submitList(it.services)
                }
                is DeviceEvent.DeletedDeviceServices -> {
                    args.item?.let { device -> viewModel.fetchDeviceServices(device.id) }
                }
                is DeviceEvent.Device -> {
                    if (args.item == null) {
                        val action =
                            RemoteDeviceFragmentDirections.actionRemoteDeviceFragmentSelf(it.device)
                        findNavController().navigate(action)
                    } else {
                        context?.showToast(R.string.updated)
                    }
                }
                else -> {}
            }
        }

        val item: RemoteDevice? = args.item

        if (item == null) {
            // new device
        } else {
            // edit device
            binding.bSaveDevice.text = getString(R.string.update)

            binding.tiName.setText(item.name)
            binding.tiAddress.setText(item.address)
            binding.switchDefault.isChecked = item.isDefault

            viewModel.fetchDeviceServices(item.id)
        }

        binding.fabDeviceAction.setOnClickListener { showMenu(it, R.menu.device_page_action) }

        binding.bDeleteDevice.setOnClickListener {
            if (item != null) {
                showDeleteDeviceConfirmationDialog(item.id)
            }
        }

        binding.bSaveDevice.setOnClickListener {
            val name = binding.tiName.text.toString().trim()
            val address = binding.tiAddress.text.toString().trim()
            if (name.isBlank() || address.isBlank()) {
                context?.showToast(R.string.missing_parameter)
            } else {
                val updatedDevice =
                    if (item == null) {
                        // new device
                        RemoteDevice(
                            id = 0,
                            name = name,
                            address = address,
                            isDefault = binding.switchDefault.isChecked)
                    } else {
                        // edit device
                        RemoteDevice(
                            id = item.id,
                            name = name,
                            address = address,
                            isDefault = binding.switchDefault.isChecked)
                    }
                viewModel.updateDevice(updatedDevice)
            }
        }

        return binding.root
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        if (args.item == null) {
            // todo: we should allow this when creating a new device
            //  maybe we could just reopen this fragment and pop this from the stack passing the
            // created one as argument
            popup.menu.findItem(R.id.new_remote_service).isEnabled = false
            popup.menu.findItem(R.id.delete_all_services).isEnabled = false
        }

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.new_remote_service -> {
                    val action =
                        RemoteDeviceFragmentDirections
                            .actionRemoteDeviceFragmentToRemoteServiceFragment(
                                deviceID = args.item!!.id)
                    findNavController().navigate(action)
                    true
                }
                R.id.delete_all_services -> {

                    showDeleteServicesConfirmationDialog()
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

    private fun showDeleteServicesConfirmationDialog() {
        val builder: AlertDialog.Builder? = activity?.let { AlertDialog.Builder(it) }
        builder
            ?.setMessage(R.string.dialog_confirm_action)
            ?.setTitle(R.string.delete_all)
            ?.setPositiveButton(R.string.yes) { _, _ ->
                viewModel.deleteAllDeviceServices(args.item!!.id)
            }
            ?.setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }

    private fun showDeleteDeviceConfirmationDialog(deviceID: Int) {
        val builder: AlertDialog.Builder? = activity?.let { AlertDialog.Builder(it) }
        builder
            ?.setMessage(R.string.dialog_confirm_action)
            ?.setTitle(R.string.delete)
            ?.setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteDevice(deviceID) }
            ?.setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }

    override fun onServiceClick(item: RemoteService) {
        val action =
            RemoteDeviceFragmentDirections.actionRemoteDeviceFragmentToRemoteServiceFragment(
                item = item, deviceID = args.item!!.id)
        findNavController().navigate(action)
    }
}
