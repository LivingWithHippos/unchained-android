package com.github.livingwithhippos.unchained.settings.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class KodiManagementViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deviceRepository: KodiDeviceRepository,
    private val kodiNetworkRepository: KodiRepository
) : ViewModel() {

    val testLiveData = MutableLiveData<Event<Boolean>>()

    fun removeDevice(device: KodiDevice) {
        viewModelScope.launch {
            deviceRepository.remove(device)
        }
    }

    fun updateDevice(device: KodiDevice) {
        viewModelScope.launch {
            val inserted = deviceRepository.add(device)
            // if the device was default and now it's not the add above will overwrite it.
            // if the device was not default and now it is this will clear the old default
            if (device.isDefault) {
                deviceRepository.clearDefaultsExcept(device)
            }
        }
    }

    fun setDefaultDevice(device: KodiDevice) {
        viewModelScope.launch {
            deviceRepository.setDefault(device)
        }
    }

    fun testKodi(address: String, port: Int, username: String?, password: String?) {
        viewModelScope.launch {
            val response = kodiNetworkRepository.getVolume(address, port, username, password)
            testLiveData.postEvent(response != null)
        }
    }

    val devices: LiveData<List<KodiDevice>> = deviceRepository.devicesFlow.asLiveData()
}
