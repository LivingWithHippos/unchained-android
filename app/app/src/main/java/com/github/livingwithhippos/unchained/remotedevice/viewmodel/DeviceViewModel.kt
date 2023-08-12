package com.github.livingwithhippos.unchained.remotedevice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
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
                if (device.isDefault) {
                    deviceRepository.setDefaultDevice(deviceID)
                }
                val newDevice = deviceRepository.getDevice(deviceID)
                if (newDevice != null)
                    deviceLiveData.postValue(
                        DeviceEvent.Device(newDevice)
                    )
            }
        }
    }

    fun updateService(remoteService: RemoteService) {
        viewModelScope.launch {
            val insertedRow = deviceRepository.insertService(remoteService)
            val serviceID = deviceRepository.getDeviceIDByRow(insertedRow)
            if (serviceID != null) {
                if (remoteService.isDefault) {
                    deviceRepository.setDefaultDeviceService(remoteService.device, serviceID)
                }
                val newService = deviceRepository.getService(serviceID)
                if (newService != null)
                    deviceLiveData.postValue(
                        DeviceEvent.Service(newService)
                    )
            }
        }
    }
}

sealed class DeviceEvent {
    data class Device(val device: RemoteDevice) : DeviceEvent()
    data class Service(val service: RemoteService) : DeviceEvent()
    data class AllDevices(val devices: List<RemoteDevice>) : DeviceEvent()
    data class DeviceServices(val deviceId: Int, val services: List<RemoteService>) : DeviceEvent()
    data class AllServices(val services: List<RemoteDevice>) : DeviceEvent()
}
