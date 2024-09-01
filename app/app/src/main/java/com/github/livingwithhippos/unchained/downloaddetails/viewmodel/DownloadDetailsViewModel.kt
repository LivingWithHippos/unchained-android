package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.local.RemoteServiceDetails
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.RemoteRepository
import com.github.livingwithhippos.unchained.data.repository.StreamingRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/** A [ViewModel] subclass. It offers LiveData to observe the calls to the streaming endpoint */
@HiltViewModel
class DownloadDetailsViewModel
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val streamingRepository: StreamingRepository,
    private val downloadRepository: DownloadRepository,
    private val kodiRepository: KodiRepository,
    private val remoteServiceRepository: RemoteRepository,
    private val remoteDeviceRepository: RemoteDeviceRepository,
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()
    val messageLiveData = MutableLiveData<Event<DownloadDetailsMessage>>()
    val eventLiveData = MutableLiveData<Event<DownloadEvent>>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            val streamingInfo = streamingRepository.getStreams(id)
            streamLiveData.postValue(streamingInfo)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val deleted = downloadRepository.deleteDownload(id)
            if (deleted == null) deletedDownloadLiveData.postEvent(-1)
            else deletedDownloadLiveData.postEvent(1)
        }
    }

    fun openUrlOnKodi(mediaURL: String, kodiDevice: RemoteDevice, kodiService: RemoteService) {
        viewModelScope.launch {
            try {
                val response =
                    kodiRepository.openUrl(
                        kodiDevice.address,
                        kodiService.port,
                        mediaURL,
                        kodiService.username,
                        kodiService.password,
                    )
                if (response != null) messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            } catch (e: Exception) {
                Timber.e("Error playing on Kodi: ${e.message}")
                messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        }
    }

    fun openUrlOnVLC(mediaURL: String, vlcDevice: RemoteDevice, vlcService: RemoteService) {

        viewModelScope.launch {
            try {
                val response =
                    remoteServiceRepository.openUrl(
                        vlcDevice.address,
                        vlcService.port,
                        mediaURL,
                        vlcService.username,
                        vlcService.password,
                    )
                // todo: use a single message valid for all players
                if (response is EitherResult.Failure)
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
            } catch (e: Exception) {
                Timber.e("Error playing on VLC: ${e.message}")
                messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        }
    }

    fun getDefaultPlayer(): String? {
        return preferences.getString("default_media_player", null)
    }

    fun getButtonVisibilityPreference(buttonKey: String, default: Boolean = true): Boolean {
        return preferences.getBoolean(buttonKey, default)
    }

    fun getCustomPlayerPreference(): String {
        return preferences.getString("custom_media_player", "") ?: ""
    }

    fun fetchDevicesAndServices(mediaPlayerOnly: Boolean = true) {
        // todo: replace other uses with [devicesAndServices]
        viewModelScope.launch {
            val devices: Map<RemoteDevice, List<RemoteService>> =
                if (mediaPlayerOnly) remoteDeviceRepository.getMediaPlayerDevicesAndServices()
                else remoteDeviceRepository.getDevicesAndServices()

            eventLiveData.postEvent(DownloadEvent.DeviceAndServices(devices))
        }
    }

    suspend fun devicesAndServices(): Flow<Map<RemoteDevice, List<RemoteService>>> {
        return remoteDeviceRepository.getMediaPlayerDevicesAndServicesFlow()
    }

    /**
     * returns the IDs of the most recently used service, which also has its device ID the IDs are
     * the DB entities' IDs
     */
    fun getRecentService(): Int {
        return preferences.getInt(RECENT_SERVICE_KEY, -1)
    }

    private fun setRecentService(serviceId: Int) {
        with(preferences.edit()) {
            putInt(RECENT_SERVICE_KEY, serviceId).apply()
            apply()
        }
    }

    fun openOnRemoteService(serviceDetails: RemoteServiceDetails, link: String) {
        setRecentService(serviceDetails.service.id)
        when (serviceDetails.service.type) {
            RemoteServiceType.KODI.value -> {
                openUrlOnKodi(link, serviceDetails.device, serviceDetails.service)
            }
            RemoteServiceType.VLC.value -> {
                openUrlOnVLC(link, serviceDetails.device, serviceDetails.service)
            }
            else -> {
                Timber.e("Unknown service type: ${serviceDetails.service.type}")
            }
        }
    }

    companion object {
        const val RECENT_SERVICE_KEY = "RECENT_SERVICE"
    }
}

sealed class DownloadDetailsMessage {
    data object KodiError : DownloadDetailsMessage()

    data object KodiSuccess : DownloadDetailsMessage()

    data object KodiMissingCredentials : DownloadDetailsMessage()

    data object KodiMissingDefault : DownloadDetailsMessage()
}

sealed class DownloadEvent {
    data class KodiDevices(val devices: List<KodiDevice>) : DownloadEvent()

    data class DeviceAndServices(val devicesServices: Map<RemoteDevice, List<RemoteService>>) :
        DownloadEvent()

    data class DefaultDeviceService(val device: RemoteDevice, val service: RemoteService) :
        DownloadEvent()
}
