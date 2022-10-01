package com.github.livingwithhippos.unchained.newdownload.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.cache.InstantAvailability
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.INSTANT_AVAILABILITY_ENDPOINT
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TorrentProcessingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val torrentsRepository: TorrentsRepository,
    private val protoStore: ProtoStore
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val torrentLiveData = MutableLiveData<Event<TorrentEvent>>()

    private suspend fun getToken(): String {
        val token = protoStore.getCredentials().accessToken
        if (token.isBlank() || token.length < 5)
            throw IllegalArgumentException("Loaded token was empty or wrong: $token")

        return token
    }

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val addedMagnet =
                    torrentsRepository.addMagnet(token, magnet, availableHosts.first().host)
                when (addedMagnet) {
                    is EitherResult.Failure -> {
                        networkExceptionLiveData.postEvent(addedMagnet.failure)
                    }
                    is EitherResult.Success -> {
                        torrentLiveData.postEvent(TorrentEvent.Uploaded(addedMagnet.success))
                    }
                }
            }
        }
    }

    fun fetchTorrentDetails(torrentID: String) {
        viewModelScope.launch {
            val token = getToken()
            val torrentData: TorrentItem? = torrentsRepository.getTorrentInfo(token, torrentID)
            // todo: replace using either
            if (torrentData != null) {
                savedStateHandle[KEY_CURRENT_TORRENT] = torrentData
                torrentLiveData.postEvent(TorrentEvent.Updated(torrentData))
                // todo: remove this for files selection
                //if (torrentData.status == "waiting_files_selection")
                //    torrentsRepository.selectFiles(token, torrentID)
            } else {
                Timber.e("Retrieved torrent info were null for id $torrentID")
            }
        }
    }

    fun checkTorrentCache(hash: String) {
        viewModelScope.launch {
            val token = getToken()
            val builder = StringBuilder(BASE_URL)
            builder.append(INSTANT_AVAILABILITY_ENDPOINT)
            builder.append("/")
            builder.append(hash)
            when (val cache =
                torrentsRepository.getInstantAvailability(token, builder.toString())) {
                is EitherResult.Failure -> {
                    Timber.e("Failed getting cache for hash $hash ${cache.failure}")
                }
                is EitherResult.Success -> {
                    if (cache.success.cachedTorrents.isNotEmpty()) {
                        torrentLiveData.postEvent(TorrentEvent.Availability(cache.success))
                        savedStateHandle[KEY_CACHE] = cache.success
                    } else {
                        torrentLiveData.postEvent(TorrentEvent.CacheMiss)
                    }
                }
            }
        }
    }

    fun getCache(): InstantAvailability? {
        return savedStateHandle[KEY_CACHE]
    }

    fun setCacheIndex(index: Int) {
        savedStateHandle[KEY_CACHE_INDEX] = index
    }

    fun getCacheIndex(): Int {
        return savedStateHandle[KEY_CACHE_INDEX] ?: 0
    }

    fun downloadCache() {
        viewModelScope.launch {
            val cache = getCache()
            val item: TorrentItem? = savedStateHandle[KEY_CURRENT_TORRENT]
            if (cache!=null && item != null) {
                val token = getToken()
                val index = getCacheIndex()
                val fileIDs = cache.cachedTorrents.first().cachedAlternatives[index].cachedFiles.joinToString(separator = ",") {
                    it.id.toString()
                }
                // todo: add polling to check this
                torrentsRepository.selectFiles(
                    token,
                    item.id,
                    fileIDs
                )
            }
        }
    }

    fun downloadAll() {
        viewModelScope.launch {
            val cache = getCache()
            val item: TorrentItem? = savedStateHandle[KEY_CURRENT_TORRENT]
            if (cache!=null && item != null) {
                val token = getToken()
                val index = getCacheIndex()
                // todo: add polling to check this
                torrentsRepository.selectFiles(
                    token,
                    item.id
                )
            }
        }
    }

    fun selectFiles() {
        // todo: implement this, use /torrents/info/{id}, create a dialog/sheet/fragment and start the download
    }

    fun pollTorrentStatus() {
        Timer().scheduleAtFixedRate( object : TimerTask() {
            override fun run() {
                // check if it goes into select files
                // todo: create a service to do this, check the download one
            }
        }, 0, 1000)
    }

    companion object {
        const val KEY_CACHE_INDEX = "cache_index_key"
        const val KEY_CACHE = "cache_key"
        const val KEY_CURRENT_TORRENT = "current_torrent_key"
    }
}

sealed class TorrentEvent {
    data class Uploaded(val torrent: UploadedTorrent) : TorrentEvent()
    data class Updated(val item: TorrentItem) : TorrentEvent()
    data class Availability(val cache: InstantAvailability) : TorrentEvent()
    object CacheMiss: TorrentEvent()
}