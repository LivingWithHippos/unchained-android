package com.github.livingwithhippos.unchained.downloaddetails.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteServiceDetails
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
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

    // when true, the picked service is used to add the link as a subtitle to whatever is
    // currently playing on Kodi, instead of opening it as a new video
    private val addSubtitleMode: Boolean
        get() = arguments?.getBoolean("addSubtitle") ?: false

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
                    is DownloadEvent.AllServices -> {

                        val devSer: List<CompleteRemoteServiceDetails> =
                            content.services
                                // adding a subtitle to the currently playing video is a Kodi-only
                                // feature, so only Kodi services make sense here
                                .filter { serv -> !addSubtitleMode || serv.type == RemoteServiceType.KODI.value }
                                .map { serv ->
                                    CompleteRemoteServiceDetails(
                                        service = serv,
                                        type = serviceTypeMap[serv.type]!!,
                                    )
                                }
                        adapter.submitList(devSer)
                    }
                    else -> {}
                }
            }

            viewModel.fetchServices()

            builder.setView(view).setTitle(R.string.services).setNegativeButton(
                getString(R.string.close)
            ) { dialog, _ ->
                dialog.cancel()
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onServiceClick(serviceDetails: CompleteRemoteServiceDetails) {
        val link = arguments?.getString("downloadUrl")
        if (link == null) {
            Timber.e("Download url is null")
            context?.showToast(R.string.error)
        } else if (addSubtitleMode) {
            if (serviceDetails.type == RemoteServiceType.KODI) {
                viewModel.addSubtitleOnKodi(link, serviceDetails.service)
            } else {
                Timber.e("Adding a subtitle is only supported on Kodi")
                context?.showToast(R.string.error)
            }
        } else {
            viewModel.openOnRemoteService(serviceDetails, link)
            // show toast?
        }
        this.dismiss()
    }
}
