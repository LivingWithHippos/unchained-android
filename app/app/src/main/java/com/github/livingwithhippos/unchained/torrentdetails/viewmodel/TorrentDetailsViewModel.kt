package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.Event
import kotlinx.coroutines.launch

/**
 * a [ViewModel] subclass.
 * Retrieves a torrent's details
 */
class TorrentDetailsViewModel @ViewModelInject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val torrentsRepository: TorrentsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    val torrentLiveData = MutableLiveData<Event<TorrentItem?>>()
    val deletedTorrentLiveData = MutableLiveData<Event<Int?>>()
    val downloadLiveData = MutableLiveData<Event<DownloadItem?>>()


    fun fetchTorrentDetails(torrentID: String) {
        viewModelScope.launch {
            val token = getToken()
            val torrentData =
                torrentsRepository.getTorrentInfo(token, torrentID)
            torrentLiveData.postValue(Event(torrentData))
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

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val token = getToken()
            val deletedTorrentResponse = torrentsRepository.deleteTorrent(token, id)
            deletedTorrentLiveData.postValue(Event(deletedTorrentResponse))
        }
    }

    fun downloadTorrent() {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            torrentLiveData.value?.let {
                val links = it.peekContent()?.links
                if (links != null) {
                    val items = unrestrictRepository.getUnrestrictedLinkList(token, links)
                    downloadLiveData.postValue(Event(items.firstOrNull()))
                }
            }
        }
    }
}