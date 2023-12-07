package com.github.livingwithhippos.unchained.downloaddetails.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadEvent
import com.github.livingwithhippos.unchained.settings.model.KodiDeviceAdapter
import com.github.livingwithhippos.unchained.settings.model.KodiDeviceListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KodiServerPickerDialog : DialogFragment(), KodiDeviceListener {

    private val viewModel: DownloadDetailsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(activity)

            // Get the layout inflater
            val inflater = activity.layoutInflater

            val view = inflater.inflate(R.layout.dialog_kodi_server_selection, null)

            val adapter = KodiDeviceAdapter(this)
            val list = view.findViewById<RecyclerView>(R.id.rvKodiDeviceList)
            list.adapter = adapter

            viewModel.eventLiveData.observe(this) { event ->
                when (val content = event.getContentIfNotHandled()) {
                    is DownloadEvent.KodiDevices -> {
                        // populate the list
                        adapter.submitList(content.devices)
                    }
                    null -> {}
                }
            }

            viewModel.getKodiDevices()

            builder.setView(view).setTitle(R.string.kodi).setNegativeButton(
                getString(R.string.close)
            ) { dialog, _ ->
                dialog.cancel()
            }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onEditClick(item: KodiDevice) {
        val url: String =
            arguments?.getString("url") ?: throw IllegalArgumentException("Url cannot be null")
        viewModel.openUrlOnKodi(url, item)
        this.dismiss()
    }
}
