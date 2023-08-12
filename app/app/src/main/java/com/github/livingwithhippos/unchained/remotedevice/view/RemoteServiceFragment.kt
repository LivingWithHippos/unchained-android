package com.github.livingwithhippos.unchained.remotedevice.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.databinding.FragmentRemoteServiceBinding
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.DeviceViewModel
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import timber.log.Timber


class RemoteServiceFragment : Fragment() {

    private val args: RemoteServiceFragmentArgs by navArgs()

    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRemoteServiceBinding.inflate(inflater, container, false)

        val item: RemoteService? = args.item


        val serviceTypeView = binding.serviceTypePicker.editText as? AutoCompleteTextView

        val serviceTypeAdapter = ArrayAdapter(
            requireContext(),
            R.layout.basic_dropdown_list_item,
            resources.getStringArray(R.array.service_types)
        )
        serviceTypeView?.setAdapter(serviceTypeAdapter)

        if (item == null) {
            // new service
            serviceTypeView?.setText(getString(R.string.kodi), false)

            binding.cvJackett.visibility = View.GONE
            binding.cvVlc.visibility = View.GONE
            binding.cvKodi.visibility = View.VISIBLE
        } else {
            // edit service
            binding.bSaveService.text = getString(R.string.update)

            binding.tiName.setText(item.name)
            binding.tiPort.setText(item.port)
            binding.switchDefault.isChecked = item.isDefault

            when (item.type) {
                RemoteServiceType.KODI.value -> {
                    serviceTypeView?.setText(getString(R.string.kodi), false)

                    binding.cvJackett.visibility = View.GONE
                    binding.cvVlc.visibility = View.GONE
                    binding.cvKodi.visibility = View.VISIBLE
                }
                RemoteServiceType.VLC.value -> {
                    serviceTypeView?.setText(getString(R.string.player_vlc), false)

                    binding.cvJackett.visibility = View.GONE
                    binding.cvKodi.visibility = View.GONE
                    binding.cvVlc.visibility = View.VISIBLE
                }
                RemoteServiceType.JACKETT.value -> {
                    serviceTypeView?.setText(getString(R.string.jackett), false)

                    binding.cvVlc.visibility = View.GONE
                    binding.cvKodi.visibility = View.GONE
                    binding.cvJackett.visibility = View.VISIBLE
                }
                else -> {
                    Timber.e("Unknown service type ${item.type}")
                }
            }

        }

        serviceTypeView?.setOnItemClickListener { _, _, position, _ ->
            when (val selectedTypeService: String? = serviceTypeAdapter.getItem(position)) {
                getString(R.string.kodi) -> {
                    binding.cvJackett.visibility = View.GONE
                    binding.cvVlc.visibility = View.GONE
                    binding.cvKodi.visibility = View.VISIBLE
                }
                getString(R.string.player_vlc) -> {
                    binding.cvJackett.visibility = View.GONE
                    binding.cvKodi.visibility = View.GONE
                    binding.cvVlc.visibility = View.VISIBLE
                }
                getString(R.string.jackett) -> {
                    binding.cvVlc.visibility = View.GONE
                    binding.cvKodi.visibility = View.GONE
                    binding.cvJackett.visibility = View.VISIBLE
                }
                null -> {
                    Timber.e("Service type picked null!")
                }
                else -> {
                    Timber.e("Unknown service type picked $selectedTypeService")
                }
            }
        }

        return binding.root
    }
}