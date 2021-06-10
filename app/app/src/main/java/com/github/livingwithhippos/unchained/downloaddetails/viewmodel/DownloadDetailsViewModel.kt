package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.KodiRepository
import com.github.livingwithhippos.unchained.data.repositoy.StreamingRepository
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
    private val credentialsRepository: CredentialsRepository,
    private val streamingRepository: StreamingRepository,
    private val downloadRepository: DownloadRepository,
    private val kodiRepository: KodiRepository
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            if (token.isBlank())
                throw IllegalArgumentException("Loaded token was empty: $token")
            val streamingInfo = streamingRepository.getStreams(token, id)
            streamLiveData.postValue(streamingInfo)
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = downloadRepository.deleteDownload(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postEvent(-1)
            else
                deletedDownloadLiveData.postEvent(1)
        }
    }

    fun openUrlOnKodi(url: String) {

        val ip: String? = preferences.getString("kodi_ip_address", "")
        val port: String? = preferences.getString("kodi_port", "")
        val username: String? = preferences.getString("kodi_username", "")
        val password: String? = preferences.getString("kodi_password", "")

        if (!ip.isNullOrBlank() && !port.isNullOrBlank()) {
            viewModelScope.launch {
                kodiRepository.openUrl("$ip:$port", url)
            }
        }
    }
}