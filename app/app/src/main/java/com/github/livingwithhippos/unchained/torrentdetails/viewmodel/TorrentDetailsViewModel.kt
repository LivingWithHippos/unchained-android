package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * a [ViewModel] subclass.
 * Retrieves a torrent's details
 */
@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val torrentsRepository: TorrentsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    val torrentLiveData = MutableLiveData<Event<TorrentItem?>>()
    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val downloadLiveData = MutableLiveData<Event<DownloadItem?>>()
    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()


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
            val deleted = torrentsRepository.deleteTorrent(token, id)
            when (deleted) {
                is Either.Left -> {
                    errorsLiveData.postEvent(listOf(deleted.value))
                }
                is Either.Right -> {
                    deletedTorrentLiveData.postEvent(204)
                }
            }
        }
    }

    fun downloadTorrent() {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            torrentLiveData.value?.let { torrent ->
                val links = torrent.peekContent()?.links
                if (links != null) {
                    val items = unrestrictRepository.getUnrestrictedLinkList(token, links)

                    val values = items.filterIsInstance<Either.Right<DownloadItem>>().map { it.value }
                    val errors = items.filterIsInstance<Either.Left<UnchainedNetworkException>>()
                        .map { it.value }

                    // since the torrent want to open a download details page we oen only the first link
                    downloadLiveData.postEvent(values.firstOrNull())
                    if (errors.isNotEmpty())
                        errorsLiveData.postEvent(errors)
                }
            }
        }
    }
}