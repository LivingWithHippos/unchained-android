package com.github.livingwithhippos.unchained.remoteservice.view

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
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.databinding.FragmentServicesListBinding
import com.github.livingwithhippos.unchained.remoteservice.viewmodel.ServiceEvent
import com.github.livingwithhippos.unchained.remoteservice.viewmodel.ServiceViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CompleteServiceListFragment : UnchainedFragment(), CompleteServiceListListener {

    private val viewModel: ServiceViewModel by viewModels()
    private var _binding: FragmentServicesListBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentServicesListBinding.inflate(inflater, container, false)

        val serviceAdapter = CompleteServiceListAdapter(this)
        binding.rvServiceList.adapter = serviceAdapter

        val serviceTracker: SelectionTracker<CompleteRemoteService> =
            SelectionTracker.Builder(
                "serviceListSelection",
                binding.rvServiceList,
                CompleteServiceKeyProvider(serviceAdapter),
                CompleteServiceDetailsLookup(binding.rvServiceList),
                StorageStrategy.createParcelableStorage(CompleteRemoteService::class.java),
            )
                .withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()

        serviceAdapter.tracker = serviceTracker

        viewModel.serviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is ServiceEvent.AllServices -> {
                    serviceAdapter.submitList(it.items)
                }
                ServiceEvent.DeletedAll -> {
                    context?.showToast(R.string.service_deleted)
                }
                is ServiceEvent.DeletedService -> {
                    // not happening here
                    context?.showToast(R.string.service_deleted)
                }
                is ServiceEvent.Service -> {
                    // not happening here
                }
                is ServiceEvent.ServiceNotWorking -> {
                    // not happening here
                }
                ServiceEvent.ServiceWorking -> {
                    // not happening here
                }
            }
        }

        binding.bAddService.setOnClickListener {
            val action = CompleteServiceListFragmentDirections.actionCompleteServiceListFragmentToCompleteServiceFragment()
            findNavController().navigate(action)
        }

        viewModel.fetchAllServices()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int) {
        val popup = PopupMenu(requireContext(), v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when (menuItem.itemId) {
                R.id.new_remote_service -> {
                    val action = CompleteServiceListFragmentDirections.actionCompleteServiceListFragmentToCompleteServiceFragment()
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
                viewModel.deleteAllServices()
            }
            ?.setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }

    override fun onServiceClick(item: CompleteRemoteService) {
        val action = CompleteServiceListFragmentDirections.actionCompleteServiceListFragmentToCompleteServiceFragment(item = item)
        findNavController().navigate(action)
    }
}