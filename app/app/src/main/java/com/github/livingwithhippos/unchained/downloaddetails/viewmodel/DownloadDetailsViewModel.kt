package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.KodiDeviceRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.StreamingRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [ViewModel] subclass.
 * It offers LiveData to observe the calls to the streaming endpoint
 */
@HiltViewModel
class DownloadDetailsViewModel @Inject constructor(
    private val preferences: SharedPreferences,
    private val streamingRepository: StreamingRepository,
    private val downloadRepository: DownloadRepository,
    private val kodiRepository: KodiRepository,
    private val kodiDeviceRepository: KodiDeviceRepository,
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()
    val messageLiveData = MutableLiveData<Event<DownloadDetailsMessage>>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            val streamingInfo = streamingRepository.getStreams(id)
            streamLiveData.postValue(streamingInfo)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val deleted = downloadRepository.deleteDownload(id)
            if (deleted == null)
                deletedDownloadLiveData.postEvent(-1)
            else
                deletedDownloadLiveData.postEvent(1)
        }
    }

    fun openUrlOnKodi(url: String) {
        viewModelScope.launch {
            val device = kodiDeviceRepository.getDefault()
            if (device != null) {
                val response = kodiRepository.openUrl(
                    device.address,
                    device.port,
                    url,
                    device.username,
                    device.password
                )
                if (response != null)
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            } else {
                val allDevices = kodiDeviceRepository.getDevices()
                if (allDevices.isNotEmpty())
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingDefault)
                else
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingCredentials)
            }
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
}

sealed class DownloadDetailsMessage {
    object KodiError : DownloadDetailsMessage()
    object KodiSuccess : DownloadDetailsMessage()
    object KodiMissingCredentials : DownloadDetailsMessage()
    object KodiMissingDefault : DownloadDetailsMessage()
}
