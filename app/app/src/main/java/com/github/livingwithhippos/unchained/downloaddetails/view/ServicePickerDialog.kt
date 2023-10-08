package com.github.livingwithhippos.unchained.downloaddetails.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteServiceDetails
import com.github.livingwithhippos.unchained.data.local.serviceTypeMap
import com.github.livingwithhippos.unchained.downloaddetails.model.ServicePickerAdapter
import com.github.livingwithhippos.unchained.downloaddetails.model.ServicePickerListener
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadDetailsViewModel
import com.github.livingwithhippos.unchained.downloaddetails.viewmodel.DownloadEvent
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ServicePickerDialog : DialogFragment(), ServicePickerListener {

    private val viewModel: DownloadDetailsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->

            // Use the Builder class for convenient dialog construction
            val builder = MaterialAlertDialogBuilder(activity)

            // Get the layout inflater
            val inflater = activity.layoutInflater

            val view = inflater.inflate(R.layout.dialog_service_picker, null)

            val adapter = ServicePickerAdapter(this)
            val list = view.findViewById<RecyclerView>(R.id.rvServiceList)
            list.adapter = adapter

            viewModel.eventLiveData.observe(this) { event ->
                when (val content = event.getContentIfNotHandled()) {
                    is DownloadEvent.DeviceAndServices -> {

                        val devSer: List<RemoteServiceDetails> =
                            content.devicesServices.flatMap {
                                it.value.map { serv ->
                                    RemoteServiceDetails(
                                        service = serv,
                                        device = it.key,
                                        type = serviceTypeMap[serv.type]!!
                                    )
                                }
                            }
                        adapter.submitList(devSer)
                    }
                    else -> {}
                }
            }

            viewModel.fetchDevicesAndServices()

            builder.setView(view).setTitle(R.string.services).setNegativeButton(
                getString(R.string.close)
            ) { dialog, _ ->
                dialog.cancel()
            }

            builder.create()
        }
            ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onServiceClick(serviceDetails: RemoteServiceDetails) {
        val link = arguments?.getString("downloadUrl")
        if (link == null) {
            Timber.e("Download url is null")
            context?.showToast(R.string.error)
        } else {
            viewModel.openOnRemoteService(serviceDetails, link)
            // show toast?
        }
        this.dismiss()
    }
}
