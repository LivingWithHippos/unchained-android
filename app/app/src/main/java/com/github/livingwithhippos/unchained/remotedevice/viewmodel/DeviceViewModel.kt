package com.github.livingwithhippos.unchained.remotedevice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class DeviceViewModel @Inject constructor(private val deviceRepository: RemoteDeviceRepository) :
    ViewModel() {

    val deviceLiveData = MutableLiveData<DeviceEvent>()

    fun fetchRemoteDevices() {
        viewModelScope.launch {
            deviceLiveData.postValue(DeviceEvent.AllDevices(deviceRepository.getAllDevices()))
        }
    }

    fun fetchDeviceServices(deviceId: Int) {
        viewModelScope.launch {
            deviceLiveData.postValue(
                DeviceEvent.DeviceServices(deviceId, deviceRepository.getDeviceServices(deviceId)))
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
                if (newDevice != null) deviceLiveData.postValue(DeviceEvent.Device(newDevice))
            }
        }
    }

    fun updateService(remoteService: RemoteService) {
        viewModelScope.launch {
            val insertedRow = deviceRepository.insertService(remoteService)
            val serviceID = deviceRepository.getServiceIDByRow(insertedRow)
            if (serviceID != null) {
                if (remoteService.isDefault) {
                    deviceRepository.setDefaultDeviceService(remoteService.device, serviceID)
                }
                val newService = deviceRepository.getService(serviceID)
                if (newService != null) deviceLiveData.postValue(DeviceEvent.Service(newService))
            } else {
                Timber.e("Service ID is null trying to save on the db")
            }
        }
    }

    fun deleteService(service: RemoteService) {
        viewModelScope.launch {
            deviceRepository.deleteService(service)
            deviceLiveData.postValue(DeviceEvent.DeletedService(service))
        }
    }

    fun deleteDeviceServices(deviceId: Int) {
        viewModelScope.launch {
            deviceRepository.deleteDeviceServices(deviceId)
            deviceLiveData.postValue(DeviceEvent.DeletedDeviceServices(deviceId))
        }
    }

    fun deleteAllDevices() {
        viewModelScope.launch {
            deviceRepository.deleteAll()
            deviceLiveData.postValue(DeviceEvent.DeletedAll)
        }
    }

    fun fetchDevicesAndServices() {
        viewModelScope.launch {
            deviceLiveData.postValue(
                DeviceEvent.AllDevicesAndServices(deviceRepository.getDevicesAndServices()))
        }
    }
}

sealed class DeviceEvent {
    data object DeletedAll : DeviceEvent()

    data class AllDevicesAndServices(val itemsMap: Map<RemoteDevice, List<RemoteService>>) :
        DeviceEvent()

    data class Device(val device: RemoteDevice) : DeviceEvent()

    data class AllDevices(val devices: List<RemoteDevice>) : DeviceEvent()

    data class DeviceServices(val deviceId: Int, val services: List<RemoteService>) : DeviceEvent()

    data class DeletedDeviceServices(val deviceId: Int) : DeviceEvent()

    data class Service(val service: RemoteService) : DeviceEvent()

    data class DeletedService(val service: RemoteService) : DeviceEvent()
}
