package com.github.livingwithhippos.unchained.remotedevice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteServiceBinding
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceEvent
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class RemoteServiceFragment : Fragment() {

    private val args: RemoteServiceFragmentArgs by navArgs()

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRemoteServiceBinding.inflate(inflater, container, false)

        val item: RemoteService? = args.item

        val serviceTypeView = binding.serviceTypePicker.editText as? AutoCompleteTextView

        val serviceTypeAdapter =
            ArrayAdapter(
                requireContext(),
                R.layout.basic_dropdown_list_item,
                resources.getStringArray(R.array.service_types)
            )
        serviceTypeView?.setAdapter(serviceTypeAdapter)

        if (item == null) {
            // new service
            serviceTypeView?.setText(getString(R.string.kodi), false)
        } else {
            // edit service
            binding.bSaveService.text = getString(R.string.update)

            binding.tiName.setText(item.name)
            binding.tiPort.setText(item.port.toString())
            binding.tiUsername.setText(item.username ?: "")
            binding.tiPassword.setText(item.password.toString())
            binding.switchDefault.isChecked = item.isDefault

            when (item.type) {
                RemoteServiceType.KODI.value -> {
                    serviceTypeView?.setText(getString(R.string.kodi), false)
                }
                RemoteServiceType.VLC.value -> {
                    serviceTypeView?.setText(getString(R.string.player_vlc), false)
                }
                RemoteServiceType.JACKETT.value -> {
                    serviceTypeView?.setText(getString(R.string.jackett), false)
                }
                else -> {
                    Timber.e("Unknown service type ${item.type}")
                }
            }
        }

        serviceTypeView?.setOnItemClickListener { _, _, position, _ ->
            when (val selectedTypeService: String? = serviceTypeAdapter.getItem(position)) {
                getString(R.string.kodi) -> {}
                getString(R.string.player_vlc) -> {}
                getString(R.string.jackett) -> {}
                null -> {
                    Timber.e("Service type picked null!")
                }
                else -> {
                    Timber.e("Unknown service type picked $selectedTypeService")
                }
            }
        }

        binding.bSaveService.setOnClickListener {
            val name = binding.tiName.text.toString().trim()
            val username = binding.tiUsername.text.toString().trim()
            val password = binding.tiPassword.text.toString().trim()
            val port = binding.tiPort.text.toString().toIntOrNull()
            val serviceId = item?.id ?: 0

            if (name.isBlank() || port == null) {
                context?.showToast(R.string.missing_parameter)
                return@setOnClickListener
            }

            when (val selectedService: String = binding.servicePickerText.text.toString()) {
                getString(R.string.kodi) -> {
                    val remoteService =
                        RemoteService(
                            id = serviceId,
                            device = args.deviceID,
                            name = name,
                            port = port,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = RemoteServiceType.KODI.value,
                            isDefault = binding.switchDefault.isChecked,
                        )
                    viewModel.updateService(remoteService)
                }
                getString(R.string.player_vlc) -> {}
                getString(R.string.jackett) -> {}
                else -> {
                    Timber.e("Unknown service type saving $selectedService")
                }
            }
        }

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DeviceEvent.Service -> {
                    if (args.item == null) {
                        val action =
                            RemoteServiceFragmentDirections.actionRemoteServiceFragmentSelf(
                                item = it.service,
                                deviceID = args.deviceID
                            )
                        findNavController().navigate(action)
                    } else {
                        context?.showToast(R.string.updated)
                    }
                }
                else -> {}
            }
        }

        return binding.root
    }
}
