package com.github.livingwithhippos.unchained.settings.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.viewModels
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.settings.viewmodel.KodiManagementViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KodiDeviceBottomSheet() : BottomSheetDialogFragment() {

    private val viewModel: KodiManagementViewModel by viewModels()

    private var currentDevice: KodiDevice? = null

    constructor(device: KodiDevice) : this() {
        currentDevice = device
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.modal_kodi_device, container, false)

        currentDevice?.let {
            viewModel.setCurrentDevice(it)
        }

        val device = viewModel.getCurrentDevice()
        if (device != null)
            loadDeviceInfo(view, device)

        view.findViewById<Button>(R.id.bTest).setOnClickListener {
            val address =
                view.findViewById<TextInputEditText>(R.id.tiAddress).text.toString().trim()
            val port = view.findViewById<TextInputEditText>(R.id.tiPort).text.toString().trim()
                .toIntOrNull()
            if (address.isEmpty() || port == null) {
                context?.showToast(R.string.kodi_credentials_incomplete)
                return@setOnClickListener
            } else {
                val password =
                    view.findViewById<TextInputEditText>(R.id.tiPassword).text.toString().trim()
                val username =
                    view.findViewById<TextInputEditText>(R.id.tiUsername).text.toString().trim()
                viewModel.testKodi(address, port, username, password)
            }
        }

        view.findViewById<Button>(R.id.bSave).setOnClickListener {
            val name = view.findViewById<TextInputEditText>(R.id.tiName).text.toString().trim()
            val address =
                view.findViewById<TextInputEditText>(R.id.tiAddress).text.toString().trim()
            val port = view.findViewById<TextInputEditText>(R.id.tiPort).text.toString().trim()
                .toIntOrNull()

            if (name.isEmpty() || address.isEmpty() || port == null) {
                context?.showToast(R.string.kodi_credentials_incomplete)
                return@setOnClickListener
            } else {
                val password =
                    view.findViewById<TextInputEditText>(R.id.tiPassword).text.toString().trim()
                val username =
                    view.findViewById<TextInputEditText>(R.id.tiUsername).text.toString().trim()
                val isDefault = view.findViewById<CheckBox>(R.id.cbDefault).isChecked
                // todo: manage same device name being overwritten
                if (device != null) {
                    viewModel.updateDevice(
                        KodiDevice(
                            name, address, port, username, password, isDefault
                        ),
                        device.name
                    )
                } else {
                    viewModel.insertDevice(
                        KodiDevice(
                            name, address, port, username, password, isDefault
                        )
                    )
                }
                context?.showToast(R.string.device_added)
                dismiss()
            }
        }

        return view
    }

    private fun loadDeviceInfo(view: View, device: KodiDevice) {
        view.findViewById<TextInputEditText>(R.id.tiName).setText(device.name)
        view.findViewById<TextInputEditText>(R.id.tiAddress).setText(device.address)
        view.findViewById<TextInputEditText>(R.id.tiPort).setText(device.port.toString())
        view.findViewById<TextInputEditText>(R.id.tiUsername).setText(device.username)
        view.findViewById<TextInputEditText>(R.id.tiPassword).setText(device.password)
        view.findViewById<CheckBox>(R.id.cbDefault).isChecked = device.isDefault
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.testLiveData.observe(viewLifecycleOwner) {

            when (it.getContentIfNotHandled()) {
                true -> {
                    context?.showToast(R.string.kodi_connection_successful)
                }
                false -> {
                    context?.showToast(R.string.kodi_connection_error)
                }
                null -> {
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        const val TAG = "KodiDeviceModalBottomSheet"
    }
}