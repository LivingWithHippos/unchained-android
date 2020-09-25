package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import kotlinx.coroutines.launch

/**
 * a [ViewModel] SUBCLASS.
 * Retrieves a torrent's details
 */
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

    private suspend fun getToken(): String {
        val token = credentialsRepository.getToken()
        if (token.isBlank() || token.length < 5)
            throw IllegalArgumentException("Loaded token was empty or wrong: $token")

        return token
    }
}