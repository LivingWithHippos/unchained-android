package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
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
    private val protoStore: ProtoStore,
    private val kodiRepository: KodiRepository
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()
    val messageLiveData = MutableLiveData<Event<DownloadDetailsMessage>>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken
            if (token.isBlank())
                throw IllegalArgumentException("Loaded token was empty: $token")
            val streamingInfo = streamingRepository.getStreams(token, id)
            streamLiveData.postValue(streamingInfo)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken
            val deleted = downloadRepository.deleteDownload(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postEvent(-1)
            else
                deletedDownloadLiveData.postEvent(1)
        }
    }

    fun openUrlOnKodi(url: String) {

        val ip: String? = preferences.getString("kodi_ip_address", "")
        val port: Int = preferences.getString("kodi_port", null)?.toIntOrNull() ?: -1
        val username: String? = preferences.getString("kodi_username", "")
        val password: String? = preferences.getString("kodi_password", "")

        if (!ip.isNullOrBlank() && port > 0) {
            viewModelScope.launch {
                val response = kodiRepository.openUrl(ip, port, url, username, password)
                if (response != null)
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        } else {
            messageLiveData.postEvent(DownloadDetailsMessage.KodiMissingCredentials)
        }
    }

    fun getKodiPreference(): Boolean {
        return preferences.getBoolean("show_kodi", true)
    }
}

sealed class DownloadDetailsMessage {
    object KodiError : DownloadDetailsMessage()
    object KodiSuccess : DownloadDetailsMessage()
    object KodiMissingCredentials : DownloadDetailsMessage()
}
