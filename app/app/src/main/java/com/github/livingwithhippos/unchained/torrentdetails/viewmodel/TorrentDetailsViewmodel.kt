package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import kotlinx.coroutines.launch

class TorrentDetailsViewmodel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    val torrentLiveData = MutableLiveData<TorrentItem?>()


    fun fetchTorrentDetails(torrentID: String) {
        viewModelScope.launch {
            val token = getToken()
            val torrentData =
                torrentsRepository.getTorrentInfo(token, torrentID)
            torrentLiveData.postValue(torrentData)
            if (torrentData?.status == "waiting_files_selection")
                torrentsRepository.selectFiles(token, torrentID)
        }

    }

    //todo: move this to an extension, maybe passing CredentialsRepository and SavedStateHandle
    private suspend fun getToken(): String {
        var token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty()) {
            token = credentialsRepository.getCompleteCredentials().first().accessToken
            savedStateHandle.set(KEY_TOKEN, token)
        }
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")

        return token
    }
}