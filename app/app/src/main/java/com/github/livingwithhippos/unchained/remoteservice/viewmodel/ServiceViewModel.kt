package com.github.livingwithhippos.unchained.remoteservice.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.ServiceRepository
import com.github.livingwithhippos.unchained.di.ClassicClient
import com.github.livingwithhippos.unchained.remotedevice.viewmodel.ServiceErrorType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber

@HiltViewModel
class ServiceViewModel
@Inject
constructor(
    private val serviceRepository: ServiceRepository,
    private val kodiRepository: KodiRepository,
    @param:ClassicClient private val client: OkHttpClient,
) : ViewModel() {

    val serviceLiveData = MutableLiveData<ServiceEvent>()

    fun testService(
        type: RemoteServiceType,
        address: String,
        username: String?,
        password: String?,
        apiToken: String?,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (type) {
                    is RemoteServiceType.JACKETT -> {
                        val url: StringBuilder = StringBuilder()
                        // todo: check if ip and add http
                        /*
                        if (
                            !address.startsWith("http://", ignoreCase = true) &&
                            !address.startsWith("https://", ignoreCase = true)
                        ) {
                            if (port == 443) url.append("https://") else url.append("http://")
                        }
                         */
                        url.append(address)
                        url.append("/api/v2.0/indexers/all/results/torznab/api?t=caps")
                        if (apiToken != null) url.append("&apikey=$apiToken")
                        val request = okhttp3.Request.Builder().url(url.toString()).build()
                        try {
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                Timber.d(response.body.toString())
                                serviceLiveData.postValue(ServiceEvent.ServiceWorking)
                            } else {
                                serviceLiveData.postValue(
                                    ServiceEvent.ServiceNotWorking(ServiceErrorType.ResponseError)
                                )
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error testing the service $url")
                            serviceLiveData.postValue(
                                ServiceEvent.ServiceNotWorking(ServiceErrorType.Generic)
                            )
                        }
                    }

                    is RemoteServiceType.KODI -> {
                        val response = kodiRepository.getVolume(address, username, password)
                        serviceLiveData.postValue(
                            if (response != null) ServiceEvent.ServiceWorking else
                            ServiceEvent.ServiceNotWorking(ServiceErrorType.InvalidService)
                        )
                    }

                    is RemoteServiceType.VLC -> {
                        serviceLiveData.postValue(
                            ServiceEvent.ServiceNotWorking(ServiceErrorType.InvalidService)
                        )
                    }
                }
            }
        }
    }

    fun fetchAllServices() {
        viewModelScope.launch {
            serviceLiveData.postValue(ServiceEvent.AllServices(serviceRepository.getServices()))
        }
    }

    fun updateService(service: CompleteRemoteService) {
        viewModelScope.launch {
            val insertedRow = serviceRepository.upsertService(service)
            val serviceID = serviceRepository.getServiceIDByRow(insertedRow)
            // if the default service is updated, remove the old preference
            if (serviceID != null) {
                if (service.isDefault) {
                    // fixme: not resetting previous defaults
                    serviceRepository.setDefaultService(serviceID)
                }
                val newService = serviceRepository.getService(serviceID)
                if (newService != null) serviceLiveData.postValue(ServiceEvent.Service(newService))
            }
        }
    }

    fun deleteService(service: CompleteRemoteService) {
        viewModelScope.launch {
            serviceRepository.deleteService(service)
            serviceLiveData.postValue(ServiceEvent.DeletedService(service))
        }
    }

    fun deleteAllServices() {
        viewModelScope.launch {
            serviceRepository.deleteAll()
            serviceLiveData.postValue(ServiceEvent.DeletedAll)
        }
    }
}

sealed class ServiceEvent {
    data object ServiceWorking : ServiceEvent()

    data class ServiceNotWorking(val errorType: ServiceErrorType) : ServiceEvent()

    data object DeletedAll : ServiceEvent()

    data class AllServices(val items: List<CompleteRemoteService>) : ServiceEvent()

    data class Service(val service: CompleteRemoteService) : ServiceEvent()

    data class DeletedService(val service: CompleteRemoteService) : ServiceEvent()
}
