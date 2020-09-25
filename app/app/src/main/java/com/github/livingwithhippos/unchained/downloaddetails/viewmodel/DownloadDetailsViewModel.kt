package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.StreamingRepository
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to observe the calls to the streaming endpoint
 */
class DownloadDetailsViewModel @ViewModelInject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val streamingRepository: StreamingRepository
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            if (token.isBlank())
                throw IllegalArgumentException("Loaded token was empty: $token")
            val streamingInfo = streamingRepository.getStreams(token, id)
            streamLiveData.postValue(streamingInfo)
        }
    }
}