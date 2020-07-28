package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictedLink
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TorrentDetailsViewmodel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)


    val torrentLiveData = MutableLiveData<TorrentItem?>()


    fun fetchTorrentDetails(torrentID: String) {
        scope.launch {
            val token = getToken()
            val torrentData =
                torrentsRepository.getTorrentInfo(token, torrentID)
            torrentLiveData.postValue(torrentData)
        }

    }

    //todo: move this to an extension, maybe passing CredentialsRepository and SavedStateHandle
    private suspend fun getToken(): String {
        var token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty())
            token = credentialsRepository.getCompleteCredentials().first().accessToken
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")

        savedStateHandle.set(KEY_TOKEN, token)
        return token
    }
}