package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
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
    private val torrentsRepository: TorrentsRepository,
    private val protoStore: ProtoStore,
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
        val token = protoStore.getCredentials().accessToken
        if (token.isBlank() || token.length < 5)
            throw IllegalArgumentException("Loaded token was empty or wrong: $token")

        return token
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val token = getToken()
            val deleted = torrentsRepository.deleteTorrent(token, id)
            when (deleted) {
                is EitherResult.Failure -> {
                    errorsLiveData.postEvent(listOf(deleted.failure))
                }
                is EitherResult.Success -> {
                    deletedTorrentLiveData.postEvent(204)
                }
            }
        }
    }

    fun downloadTorrent() {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken
            torrentLiveData.value?.let { torrent ->
                val links = torrent.peekContent()?.links
                if (links != null) {
                    val items = unrestrictRepository.getUnrestrictedLinkList(token, links)

                    val values =
                        items.filterIsInstance<EitherResult.Success<DownloadItem>>()
                            .map { it.success }
                    val errors =
                        items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>()
                            .map { it.failure }

                    // since the torrent want to open a download details page we oen only the first link
                    downloadLiveData.postEvent(values.firstOrNull())
                    if (errors.isNotEmpty())
                        errorsLiveData.postEvent(errors)
                }
            }
        }
    }
}
