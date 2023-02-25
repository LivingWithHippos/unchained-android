package com.github.livingwithhippos.unchained.settings.viewmodel

import android.os.Parcelable
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
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@HiltViewModel
class KodiManagementViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deviceRepository: KodiDeviceRepository,
    private val kodiNetworkRepository: KodiRepository
) : ViewModel() {

    val testLiveData = MutableLiveData<Event<Boolean>>()

    val devices: LiveData<List<KodiDevice>> = deviceRepository.devicesFlow.asLiveData()

    fun removeDevice(device: KodiDevice) {
        viewModelScope.launch { deviceRepository.remove(device) }
    }

    fun updateDevice(device: KodiDevice, oldDeviceName: String) {
        viewModelScope.launch {
            deviceRepository.update(device, oldDeviceName)
            // if the device was default and now it's not the add above will overwrite it.
            // if the device was not default and now it is this will clear the old default
            if (device.isDefault) {
                deviceRepository.clearDefaultsExcept(device)
            }
        }
    }

    fun insertDevice(device: KodiDevice) {
        viewModelScope.launch {
            // if the device was default and now it's not the add above will overwrite it.
            // if the device was not default and now it is this will clear the old default
            deviceRepository.add(device)
        }
    }

    fun setDefaultDevice(device: KodiDevice) {
        viewModelScope.launch { deviceRepository.setDefault(device) }
    }

    fun testKodi(address: String, port: Int, username: String?, password: String?) {
        viewModelScope.launch {
            val response = kodiNetworkRepository.getVolume(address, port, username, password)
            testLiveData.postEvent(response != null)
        }
    }

    fun setCurrentDevice(device: KodiDevice) {
        savedStateHandle.set(
            KEY_SAVED_DEVICE,
            TempKodiDevice(
                device.name,
                device.address,
                device.port,
                device.username,
                device.password,
                device.isDefault
            )
        )
    }

    fun getCurrentDevice(): KodiDevice? {
        val device = savedStateHandle.get<TempKodiDevice>(KEY_SAVED_DEVICE)
        if (device != null) {
            return KodiDevice(
                device.name,
                device.address,
                device.port,
                device.username,
                device.password,
                device.isDefault
            )
        }
        return null
    }

    suspend fun deleteDevice(device: KodiDevice) {
        deviceRepository.remove(device)
    }

    companion object {
        const val KEY_SAVED_DEVICE = "saved_item_key"
    }
}

@Parcelize
data class TempKodiDevice(
    val name: String,
    val address: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val isDefault: Boolean = false,
) : Parcelable
