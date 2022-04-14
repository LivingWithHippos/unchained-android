package com.github.livingwithhippos.unchained.settings.view

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.settings.model.KodiDeviceAdapter
import com.github.livingwithhippos.unchained.settings.model.KodiDeviceListener
import com.github.livingwithhippos.unchained.settings.viewmodel.KodiManagementViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class KodiManagementDialog : DialogFragment(), KodiDeviceListener {

    private val viewModel: KodiManagementViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity!=null) {
            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(requireActivity())

            // Get the layout inflater
            val inflater = requireActivity().layoutInflater

            val view = inflater.inflate(R.layout.dialog_kodi_management, null)

            view.findViewById<Button>(R.id.bAddNew).setOnClickListener {
                showNewDeviceBottomSheet()
            }

            val adapter = KodiDeviceAdapter(this)
            val list = view.findViewById<RecyclerView>(R.id.rvKodiDeviceList)
            list.adapter = adapter

            viewModel.devices.observe(this) { devices ->
                adapter.submitList(devices)
            }

            builder.setView(view)
                .setNeutralButton(getString(R.string.close)) { dialog, _ ->
                    dialog.cancel()
                }
                .setTitle(getString(R.string.kodi))

            // Create the AlertDialog object and return it
            return builder.create()
        } else throw IllegalStateException("Activity cannot be null")

    }

    private fun showNewDeviceBottomSheet() {
        val sheet = KodiDeviceBottomSheet()
        sheet.show(parentFragmentManager, KodiDeviceBottomSheet.TAG)
    }

    override fun onEditClick(item: KodiDevice) {
        val sheet = KodiDeviceBottomSheet(item)
        sheet.show(parentFragmentManager, KodiDeviceBottomSheet.TAG)
    }
}