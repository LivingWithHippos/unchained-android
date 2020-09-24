package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.base.model.repositories.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.StreamingRepository
import com.github.livingwithhippos.unchained.downloaddetails.model.Stream
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to observe the calls to the streaming endpoint
 */
class DownloadDetailsViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val streamingRepository: StreamingRepository
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()

    fun fetchStreamingInfo(id: String) {
        viewModelScope.launch {
            //todo: this code is repeating in viewmodels, find a better way
            var token = savedStateHandle.get<String>(KEY_TOKEN)
            if (token.isNullOrEmpty())
                token = credentialsRepository.getCompleteCredentials().first().accessToken
            if (token.isNullOrEmpty())
                throw IllegalArgumentException("Loaded token was null or empty: $token")

            savedStateHandle.set(KEY_TOKEN, token)
            val streamingInfo = streamingRepository.getStreams(token, id)
            streamLiveData.postValue(streamingInfo)
        }
    }
}