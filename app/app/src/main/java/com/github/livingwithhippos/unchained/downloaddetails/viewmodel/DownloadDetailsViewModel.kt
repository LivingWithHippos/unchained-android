package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.StreamingRepository
import com.github.livingwithhippos.unchained.utilities.Event
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to observe the calls to the streaming endpoint
 */
class DownloadDetailsViewModel @ViewModelInject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val streamingRepository: StreamingRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int?>>()

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
            val deleted = downloadRepository.deleteTorrent(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postValue(Event(-1))
            else
                deletedDownloadLiveData.postValue(Event(1))
        }
    }
}