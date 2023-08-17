package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.RemoteDevice
import com.github.livingwithhippos.unchained.data.local.RemoteService
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.RemoteDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.StreamingRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/** A [ViewModel] subclass. It offers LiveData to observe the calls to the streaming endpoint */
@HiltViewModel
class DownloadDetailsViewModel
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val streamingRepository: StreamingRepository,
    private val downloadRepository: DownloadRepository,
    private val kodiRepository: KodiRepository,
    private val kodiDeviceRepository: KodiDeviceRepository,
    private val remoteDeviceRepository: RemoteDeviceRepository
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

    fun openUrlOnKodi(url: String, customDevice: KodiDevice? = null) {
        viewModelScope.launch {
            val device = customDevice ?: kodiDeviceRepository.getDefault()
            if (device != null) {
                val response =
                    kodiRepository.openUrl(
                        device.address,
                        device.port,
                        url,
                        device.username,
                        device.password
                    )
                if (response != null) messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            } else {
                val allDevices = kodiDeviceRepository.getDevices()
                if (allDevices.isNotEmpty())
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingDefault)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingCredentials)
            }
        }
    }

    fun openKodiPickerIfNeeded(url: String) {
        viewModelScope.launch {
            if (preferences.getBoolean(PreferenceKeys.Kodi.SERVER_PICKER, false)) {
                val devices = kodiDeviceRepository.getDevices()
                // if there is only one device do not show the picker
                if (devices.size == 1) openUrlOnKodi(url)
                else if (devices.isEmpty())
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingDefault)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiShowPicker(url))
            } else openUrlOnKodi(url)
        }
    }

    fun getKodiDevices() {

        viewModelScope.launch {
            val devices = kodiDeviceRepository.getDevices()
            eventLiveData.postEvent(DownloadEvent.KodiDevices(devices))
        }
    }

    fun getKodiPreference(): Boolean {
        return preferences.getBoolean("show_kodi", true)
    }

    fun getDefaultPlayer(): String? {
        return preferences.getString("default_media_player", null)
    }

    fun getDefaultPlayerButtonVisibility(): Boolean {
        return preferences.getBoolean("show_media_button", true)
    }

    fun getButtonVisibilityPreference(buttonKey: String, default: Boolean = true): Boolean {
        return preferences.getBoolean(buttonKey, default)
    }

    fun getCustomPlayerPreference(): String {
        return preferences.getString("custom_media_player", "") ?: ""
    }

    fun fetchDefaultService() {
        viewModelScope.launch {
            val devices: Map<RemoteDevice, List<RemoteService>> = remoteDeviceRepository.getDefaultDeviceWithServices()
            if (devices.isNotEmpty()) {
                val device = devices.keys.first()
                val services = devices[device]
                if (services.isNullOrEmpty().not()) {
                    eventLiveData.postEvent(DownloadEvent.DefaultDeviceService(device, services!!.first()))
                }
            }
        }
    }
}

sealed class DownloadDetailsMessage {
    object KodiError : DownloadDetailsMessage()

    object KodiSuccess : DownloadDetailsMessage()

    object KodiMissingCredentials : DownloadDetailsMessage()

    object KodiMissingDefault : DownloadDetailsMessage()

    data class KodiShowPicker(val url: String) : DownloadDetailsMessage()
}

sealed class DownloadEvent {
    data class KodiDevices(val devices: List<KodiDevice>) : DownloadEvent()
    data class DefaultDeviceService(val device: RemoteDevice, val service: RemoteService) : DownloadEvent()
}
