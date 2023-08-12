package com.github.livingwithhippos.unchained.remotedevice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel
@Inject
constructor(private val deviceRepository: RemoteDeviceRepository) :
    ViewModel() {

    val deviceLiveData = MutableLiveData<DeviceEvent>()

    fun fetchRemoteDevices() {

        viewModelScope.launch {
            deviceLiveData.postValue(
                DeviceEvent.AllDevices(
                    deviceRepository.getAllDevices()
                )
            )
        }
    }

    fun fetchDeviceServices(deviceId: Int) {

        viewModelScope.launch {
            deviceLiveData.postValue(
                DeviceEvent.DeviceServices(
                    deviceId,
                    deviceRepository.getDeviceServices(deviceId)
                )
            )
        }
    }

    fun updateDevice(device: RemoteDevice) {
        viewModelScope.launch {
            val insertedRow = deviceRepository.insertDevice(device)
            val deviceID = deviceRepository.getDeviceIDByRow(insertedRow)
            if (deviceID != null) {
                val newDevice = RemoteDevice(
                    id = deviceID,
                    name = device.name,
                    address = device.address,
                    isDefault = device.isDefault
                )
                deviceLiveData.postValue(
                    DeviceEvent.Device(newDevice)
                )
            }
        }
    }
}

sealed class DeviceEvent {
    data class Device(val device: RemoteDevice) : DeviceEvent()
    data class AllDevices(val devices: List<RemoteDevice>) : DeviceEvent()
    data class DeviceServices(val deviceId: Int, val services: List<RemoteService>) : DeviceEvent()
    data class AllServices(val services: List<RemoteDevice>) : DeviceEvent()
}
