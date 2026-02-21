package com.github.livingwithhippos.unchained.remotedevice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.di.ClassicClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber

@HiltViewModel
class DeviceViewModel
@Inject
constructor(
    private val deviceRepository: RemoteDeviceRepository,
    @param:ClassicClient private val client: OkHttpClient,
) : ViewModel() {

    val deviceLiveData = MutableLiveData<DeviceEvent>()

    fun testService(
        type: RemoteServiceType,
        address: String,
        port: Int,
        username: String?,
        password: String?,
        apiToken: String?,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (type) {
                    is RemoteServiceType.JACKETT -> {
                        val url: StringBuilder = StringBuilder()
                        if (
                            !address.startsWith("http://", ignoreCase = true) &&
                                !address.startsWith("https://", ignoreCase = true)
                        ) {
                            if (port == 443) url.append("https://") else url.append("http://")
                        }
                        url.append(address)
                        if (port != 80 && port != 443) url.append(":$port")
                        url.append("/api/v2.0/indexers/all/results/torznab/api?t=caps")
                        if (apiToken != null) url.append("&apikey=$apiToken")
                        val request = okhttp3.Request.Builder().url(url.toString()).build()
                        try {
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                Timber.d(response.body.toString())
                                deviceLiveData.postValue(DeviceEvent.ServiceWorking)
                            } else {
                                deviceLiveData.postValue(
                                    DeviceEvent.ServiceNotWorking(ServiceErrorType.ResponseError)
                                )
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error testing the service $url")
                            deviceLiveData.postValue(
                                DeviceEvent.ServiceNotWorking(ServiceErrorType.Generic)
                            )
                        }
                    }

                    is RemoteServiceType.KODI -> {
                        // todo: implement or manage from caller
                        deviceLiveData.postValue(
                            DeviceEvent.ServiceNotWorking(ServiceErrorType.InvalidService)
                        )
                    }

                    is RemoteServiceType.VLC -> {
                        deviceLiveData.postValue(
                            DeviceEvent.ServiceNotWorking(ServiceErrorType.InvalidService)
                        )
                    }
                }
            }
        }
    }

    fun fetchRemoteDevices() {
        viewModelScope.launch {
            deviceLiveData.postValue(DeviceEvent.AllDevices(deviceRepository.getAllDevices()))
        }
    }

    fun fetchDeviceServices(deviceId: Int) {
        viewModelScope.launch {
            deviceLiveData.postValue(
                DeviceEvent.DeviceServices(deviceId, deviceRepository.getDeviceServices(deviceId))
            )
        }
    }

    fun updateDevice(device: RemoteDevice) {
        viewModelScope.launch {
            val insertedRow = deviceRepository.upsertDevice(device)
            val deviceID = deviceRepository.getDeviceIDByRow(insertedRow)
            // if the default device is updated, remove the old preference
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

    fun deleteAllDeviceServices(deviceId: Int) {
        viewModelScope.launch {
            deviceRepository.deleteAllDeviceServices(deviceId)
            deviceLiveData.postValue(DeviceEvent.DeletedDeviceServices(deviceId))
        }
    }

    fun deleteDevice(deviceId: Int) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(deviceId)
            deviceLiveData.postValue(DeviceEvent.DeletedDevice)
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
                DeviceEvent.AllDevicesAndServices(deviceRepository.getDevicesAndServices())
            )
        }
    }
}

sealed class DeviceEvent {
    data object ServiceWorking : DeviceEvent()

    data class ServiceNotWorking(val errorType: ServiceErrorType) : DeviceEvent()

    data object DeletedAll : DeviceEvent()

    data object DeletedDevice : DeviceEvent()

    data class AllDevicesAndServices(val itemsMap: Map<RemoteDevice, List<RemoteService>>) :
        DeviceEvent()

    data class Device(val device: RemoteDevice) : DeviceEvent()

    data class AllDevices(val devices: List<RemoteDevice>) : DeviceEvent()

    data class DeviceServices(val deviceId: Int, val services: List<RemoteService>) : DeviceEvent()

    data class DeletedDeviceServices(val deviceId: Int) : DeviceEvent()

    data class Service(val service: RemoteService) : DeviceEvent()

    data class DeletedService(val service: RemoteService) : DeviceEvent()
}

sealed class ServiceErrorType {
    data object ResponseError : ServiceErrorType()

    data object InvalidService : ServiceErrorType()

    data object Generic : ServiceErrorType()
}
