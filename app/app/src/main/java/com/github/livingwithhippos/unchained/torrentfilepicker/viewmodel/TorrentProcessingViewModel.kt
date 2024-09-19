package com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.cache.CachedTorrent
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.INSTANT_AVAILABILITY_ENDPOINT
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TorrentProcessingViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val structureLiveData = MutableLiveData<Event<Node<TorrentFileItem>>>()


    fun fetchTorrentDetails(torrentID: String) {

        setTorrentID(torrentID)

        viewModelScope.launch {
            val torrentData: TorrentItem? = torrentsRepository.getTorrentInfo(torrentID)
            // todo: replace using either
            if (torrentData != null) {
                setTorrentDetails(torrentData)
                torrentLiveData.postEvent(TorrentEvent.TorrentInfo(torrentData))
            } else {
                Timber.e("Retrieved torrent info were null for id $torrentID")
            }
        }
    }

    private fun setTorrentDetails(item: TorrentItem) {
        savedStateHandle[KEY_CURRENT_TORRENT] = item
    }

    fun updateTorrentStructure(structure: Node<TorrentFileItem>?) {
        if (structure != null) structureLiveData.postEvent(structure)
    }

    fun triggerTorrentEvent(event: TorrentEvent) {
        torrentLiveData.postEvent(event)
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            val availableHosts = torrentsRepository.getAvailableHosts()
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
                torrentLiveData.postEvent(TorrentEvent.DownloadedFileFailure)
            } else {
                val uploadedTorrent =
                    torrentsRepository.addTorrent(binaryTorrent, availableHosts.first().host)
                when (uploadedTorrent) {
                    is EitherResult.Failure -> {
                        networkExceptionLiveData.postEvent(uploadedTorrent.failure)
                        torrentLiveData.postEvent(TorrentEvent.DownloadedFileFailure)
                    }
                    is EitherResult.Success -> {
                        fetchTorrentDetails(uploadedTorrent.success.id)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_CACHE = "cache_key"
        const val KEY_CURRENT_TORRENT = "current_torrent_key"
        const val KEY_CURRENT_TORRENT_ID = "current_torrent_id_key"
    }
}

sealed class TorrentEvent {
    data class Uploaded(val torrent: UploadedTorrent) : TorrentEvent()

    data class TorrentInfo(val item: TorrentItem) : TorrentEvent()

    data class CacheHit(val cache: CachedTorrent) : TorrentEvent()

    data object CacheMiss : TorrentEvent()

    data class FilesSelected(val torrent: TorrentItem) : TorrentEvent()

    data object DownloadAll : TorrentEvent()

    data class DownloadCache(val position: Int, val files: Int) : TorrentEvent()

    data class DownloadSelection(val filesNumber: Int) : TorrentEvent()

    data object DownloadedFileSuccess : TorrentEvent()

    data object DownloadedFileFailure : TorrentEvent()

    data class DownloadedFileProgress(val progress: Int) : TorrentEvent()
}
