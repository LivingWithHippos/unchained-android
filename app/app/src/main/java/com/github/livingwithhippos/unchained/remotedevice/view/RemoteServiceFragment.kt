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
import com.github.livingwithhippos.unchained.data.local.serviceTypeMap
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

    private var _binding: FragmentRemoteServiceBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRemoteServiceBinding.inflate(inflater, container, false)

        val item: RemoteService? = args.item
        val deviceID: Int = args.device.id

        val serviceTypeView = binding.serviceTypePicker.editText as? AutoCompleteTextView

        if (serviceTypeView == null) {
            // should not happen, used just to avoid successive checks
            Timber.e("serviceTypeView is null")
            context?.showToast(R.string.error)
            return binding.root
        }

        val serviceTypeAdapter =
            ArrayAdapter(
                requireContext(),
                R.layout.basic_dropdown_list_item,
                resources.getStringArray(R.array.service_types),
            )
        serviceTypeView.setAdapter(serviceTypeAdapter)

        if (item == null) {
            // new service, default to kodi
            setupServiceType(binding, RemoteServiceType.KODI.value, serviceTypeView)
        } else {
            // edit service
            binding.bSaveService.text = getString(R.string.update)

            binding.tiName.setText(item.name)
            binding.tiPort.setText(item.port.toString())
            binding.tiUsername.setText(item.username ?: "")
            binding.tiPassword.setText(item.password.toString())
            binding.switchDefault.isChecked = item.isDefault
            binding.tiApiToken.setText(item.apiToken)

            setupServiceType(binding, item.type, serviceTypeView)
        }

        serviceTypeView.setOnItemClickListener { _, _, position, _ ->
            val selectedService = getServiceType(serviceTypeAdapter.getItem(position))
            if (selectedService != null) {
                setupServiceType(binding, selectedService.value)
            }
        }

        binding.bTestService.setOnClickListener {
            val username = binding.tiUsername.text.toString().trim()
            val password = binding.tiPassword.text.toString().trim()
            val port = binding.tiPort.text.toString().toIntOrNull()
            val apiToken = binding.tiApiToken.text.toString().trim()
            val serviceType = getServiceType(binding.servicePickerText.text.toString())

            if (args.device.address.isBlank() || port == null || serviceType == null) {
                context?.showToast(R.string.missing_parameter)
            } else {
                binding.bTestService.isEnabled = false
                viewModel.testService(
                    serviceType,
                    args.device.address,
                    port,
                    username.ifBlank { null },
                    password.ifBlank { null },
                    apiToken.ifBlank { null },
                )
            }
        }

        binding.bSaveService.setOnClickListener {
            val name = binding.tiName.text.toString().trim()
            val username = binding.tiUsername.text.toString().trim()
            val password = binding.tiPassword.text.toString().trim()
            val port = binding.tiPort.text.toString().toIntOrNull()
            val apiToken = binding.tiApiToken.text.toString().trim()
            val serviceId = item?.id ?: 0

            if (name.isBlank() || port == null) {
                context?.showToast(R.string.missing_parameter)
                return@setOnClickListener
            }

            when (val serviceType = getServiceType(binding.servicePickerText.text.toString())) {
                RemoteServiceType.JACKETT -> {
                    val remoteService =
                        RemoteService(
                            id = serviceId,
                            device = deviceID,
                            name = name,
                            port = port,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = serviceType.value,
                            apiToken = apiToken,
                            isDefault = false,
                        )
                    viewModel.updateService(remoteService)
                }
                RemoteServiceType.PROWLARR -> {
                    val remoteService =
                        RemoteService(
                            id = serviceId,
                            device = deviceID,
                            name = name,
                            port = port,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = serviceType.value,
                            apiToken = apiToken,
                            isDefault = false,
                        )
                    viewModel.updateService(remoteService)
                }

                RemoteServiceType.KODI -> {
                    val remoteService =
                        RemoteService(
                            id = serviceId,
                            device = deviceID,
                            name = name,
                            port = port,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = serviceType.value,
                            isDefault = binding.switchDefault.isChecked,
                        )
                    viewModel.updateService(remoteService)
                }

                RemoteServiceType.VLC -> {
                    val remoteService =
                        RemoteService(
                            id = serviceId,
                            device = deviceID,
                            name = name,
                            port = port,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = serviceType.value,
                            isDefault = binding.switchDefault.isChecked,
                        )
                    viewModel.updateService(remoteService)
                }

                null -> {
                    Timber.e("Unknown service type saving ${binding.servicePickerText.text}")
                }
            }
        }

        binding.bDeleteService.setOnClickListener {
            item?.let { rs -> viewModel.deleteService(rs) }
        }

        viewModel.deviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is DeviceEvent.Service -> {
                    if (args.item == null) {
                        val action =
                            RemoteServiceFragmentDirections.actionRemoteServiceFragmentSelf(
                                item = it.service,
                                device = args.device,
                            )
                        findNavController().navigate(action)
                    } else {
                        context?.showToast(R.string.updated)
                    }
                }

                is DeviceEvent.DeletedService -> {
                    context?.showToast(R.string.service_deleted)
                    // go back
                    findNavController().popBackStack()
                }

                is DeviceEvent.ServiceWorking -> {
                    context?.showToast(R.string.connection_successful)
                    binding.bTestService.isEnabled = true
                }

                is DeviceEvent.ServiceNotWorking -> {
                    context?.showToast(R.string.connection_error)
                    binding.bTestService.isEnabled = true
                }

                else -> {}
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getServiceType(text: String?): RemoteServiceType? {
        return when (text) {
            getString(R.string.kodi) -> {
                RemoteServiceType.KODI
            }

            getString(R.string.player_vlc) -> {
                RemoteServiceType.VLC
            }

            getString(R.string.jackett) -> {
                RemoteServiceType.JACKETT
            }

            getString(R.string.prowlarr) -> {
                RemoteServiceType.PROWLARR
            }

            else -> {
                null
            }
        }
    }

    private fun setupServiceType(
        binding: FragmentRemoteServiceBinding,
        type: Int,
        serviceDropdown: AutoCompleteTextView? = null,
    ) {
        if (args.item == null) {
            binding.bDeleteService.isEnabled = false
        }
        val serviceType = serviceTypeMap[type]
        // set up default switch, enable the button only for services that reproduce media
        when (serviceType) {
            RemoteServiceType.KODI -> {
                binding.switchDefault.isEnabled = true
                binding.tfApiToken.visibility = View.GONE
            }

            RemoteServiceType.VLC -> {
                binding.switchDefault.isEnabled = true
                binding.tfApiToken.visibility = View.GONE
            }

            RemoteServiceType.JACKETT -> {
                binding.switchDefault.isEnabled = false
                binding.switchDefault.isChecked = false
                binding.tfApiToken.visibility = View.VISIBLE
            }

            RemoteServiceType.PROWLARR -> {
                binding.switchDefault.isEnabled = false
                binding.switchDefault.isChecked = false
                binding.tfApiToken.visibility = View.VISIBLE
            }

            null -> {
                Timber.e("Unknown service type $type")
                return
            }
        }

        if (serviceType.isMediaPlayer) {
            binding.switchDefault.isEnabled = true
        } else {
            binding.switchDefault.isEnabled = false
            binding.switchDefault.isChecked = false
        }
        // set the text only if the view has been passed (when starting the fragment)
        serviceDropdown?.setText(getString(serviceType.nameRes), false)
    }
}
