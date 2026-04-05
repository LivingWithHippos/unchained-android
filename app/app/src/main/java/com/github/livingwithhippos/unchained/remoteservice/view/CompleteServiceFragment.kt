package com.github.livingwithhippos.unchained.remoteservice.view

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
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.local.serviceTypeMap
import com.github.livingwithhippos.unchained.databinding.FragmentCompleteServiceBinding
import com.github.livingwithhippos.unchained.remoteservice.viewmodel.ServiceEvent
import com.github.livingwithhippos.unchained.remoteservice.viewmodel.ServiceViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CompleteServiceFragment : Fragment() {

    private val args: CompleteServiceFragmentArgs by navArgs()

    private val viewModel: ServiceViewModel by viewModels()

    private var _binding: FragmentCompleteServiceBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCompleteServiceBinding.inflate(inflater, container, false)

        val item: CompleteRemoteService? = args.item

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
            binding.tiAddress.setText(item.address.trim())
            binding.tiUsername.setText(item.username ?: "")
            binding.tiPassword.setText(item.password ?: "")
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
            val address = binding.tiAddress.text.toString().trim()
            val apiToken = binding.tiApiToken.text.toString().trim()
            val serviceType = getServiceType(binding.servicePickerText.text.toString())

            if (address.isBlank() || serviceType == null) {
                context?.showToast(R.string.missing_parameter)
            } else {
                binding.bTestService.isEnabled = false
                viewModel.testService(
                    serviceType,
                    address,
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
            val address = binding.tiAddress.text.toString().trim()
            val apiToken = binding.tiApiToken.text.toString().trim()
            val serviceId = item?.id ?: 0

            if (name.isBlank() || address.isBlank()) {
                context?.showToast(R.string.missing_parameter)
                return@setOnClickListener
            }

            when (val serviceType = getServiceType(binding.servicePickerText.text.toString())) {
                RemoteServiceType.JACKETT -> {
                    val remoteService =
                        CompleteRemoteService(
                            id = serviceId,
                            name = name,
                            address = address,
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
                        CompleteRemoteService(
                            id = serviceId,
                            name = name,
                            address = address,
                            username = username.ifBlank { null },
                            password = password.ifBlank { null },
                            type = serviceType.value,
                            isDefault = binding.switchDefault.isChecked,
                        )
                    viewModel.updateService(remoteService)
                }

                RemoteServiceType.VLC -> {
                    val remoteService =
                        CompleteRemoteService(
                            id = serviceId,
                            name = name,
                            address = address,
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

        viewModel.serviceLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is ServiceEvent.AllServices -> {
                    // not happening here
                }
                ServiceEvent.DeletedAll -> {
                    // not happening here
                }
                is ServiceEvent.DeletedService -> {
                    context?.showToast(R.string.service_deleted)
                    // go back
                    findNavController().popBackStack()
                }
                is ServiceEvent.Service -> {
                    if (args.item == null) {
                        val action =
                            CompleteServiceFragmentDirections.actionCompleteServiceFragmentSelf(
                                item = it.service
                            )
                        findNavController().navigate(action)
                        /**
                         *                         context?.showToast(R.string.service_added)
                         *                         // todo: reload the page with the set service
                         *                         findNavController().popBackStack()
                         */
                    } else {
                        context?.showToast(R.string.updated)
                    }
                }
                is ServiceEvent.ServiceNotWorking -> {

                    context?.showToast(R.string.connection_error)
                    binding.bTestService.isEnabled = true
                }
                ServiceEvent.ServiceWorking -> {
                    context?.showToast(R.string.connection_successful)
                    binding.bTestService.isEnabled = true
                }
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

            else -> {
                null
            }
        }
    }

    private fun setupServiceType(
        binding: FragmentCompleteServiceBinding,
        type: Int,
        serviceDropdown: AutoCompleteTextView? = null,
    ) {
        // note: it gives no ui feedback
        binding.bDeleteService.isEnabled = args.item != null
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
