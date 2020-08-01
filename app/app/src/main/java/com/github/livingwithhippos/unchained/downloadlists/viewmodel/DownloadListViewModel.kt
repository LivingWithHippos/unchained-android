package com.github.livingwithhippos.unchained.downloadlists.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.DownloadRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DownloadListViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    val downloadLiveData = MutableLiveData<List<DownloadItem>?>()
    val torrentLiveData = MutableLiveData<List<TorrentItem>?>()

    fun fetchAll() {
        scope.launch {
            val token = getToken()
            //todo: add user settings
            val torrents = torrentsRepository.getTorrentsList(token)
            val downloads = downloadRepository.getDownloads(token)

            downloadLiveData.postValue(downloads)
            torrentLiveData.postValue(torrents)
        }
    }


    private suspend fun getToken(): String {
        var token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty()){
            token = credentialsRepository.getCompleteCredentials().first().accessToken
            savedStateHandle.set(KEY_TOKEN, token)
        }
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")

        return token
    }
}