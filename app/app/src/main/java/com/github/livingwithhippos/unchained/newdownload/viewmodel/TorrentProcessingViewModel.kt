package com.github.livingwithhippos.unchained.newdownload.viewmodel

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
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.INSTANT_AVAILABILITY_ENDPOINT
import com.github.livingwithhippos.unchained.utilities.beforeSelectionStatusList
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TorrentProcessingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val torrentLiveData = MutableLiveData<Event<TorrentEvent>>()

    private var job = Job()

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val availableHosts = torrentsRepository.getAvailableHosts()
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val addedMagnet =
                    torrentsRepository.addMagnet(magnet, availableHosts.first().host)
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
            val torrentData: TorrentItem? = torrentsRepository.getTorrentInfo(torrentID)
            // todo: replace using either
            if (torrentData != null) {
                savedStateHandle[KEY_CURRENT_TORRENT] = torrentData
                torrentLiveData.postEvent(TorrentEvent.TorrentInfo(torrentData))
            } else {
                Timber.e("Retrieved torrent info were null for id $torrentID")
            }
        }
    }

    fun checkTorrentCache(hash: String) {
        viewModelScope.launch {
            val builder = StringBuilder(BASE_URL)
            builder.append(INSTANT_AVAILABILITY_ENDPOINT)
            builder.append("/")
            builder.append(hash)
            when (val cache =
                torrentsRepository.getInstantAvailability(builder.toString())) {
                is EitherResult.Failure -> {
                    Timber.e("Failed getting cache for hash $hash ${cache.failure}")
                }
                is EitherResult.Success -> {
                    triggerCacheResult(cache.success.cachedTorrents.firstOrNull())
                }
            }
        }
    }

    fun triggerCacheResult(cache: CachedTorrent?) {
        if (cache != null) {
            setCache(cache)
            torrentLiveData.postEvent(TorrentEvent.CacheHit(cache))
        } else {
            torrentLiveData.postEvent(TorrentEvent.CacheMiss)
        }
    }

    fun getTorrentDetails(): TorrentItem? {
        return savedStateHandle[KEY_CURRENT_TORRENT]
    }

    fun setTorrentDetails(item: TorrentItem) {
        savedStateHandle[KEY_CURRENT_TORRENT] = item
    }

    fun getTorrentID(): String? {
        return savedStateHandle[KEY_CURRENT_TORRENT_ID]
    }

    fun setTorrentID(id: String) {
        savedStateHandle[KEY_CURRENT_TORRENT_ID] = id
    }

    fun getCache(): CachedTorrent? {
        return savedStateHandle[KEY_CACHE]
    }

    fun setCache(cache: CachedTorrent) {
        savedStateHandle[KEY_CACHE] = cache
    }

    fun startSelectionLoop(files: String = "all") {

        val id = getTorrentID()

        if (id == null) {
            Timber.e("Torrent files selection requested but torrent id was not ready")
            return
        }

        job.cancelIfActive()
        job = Job()

        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {

            var selected = false
            /// maybe job.isActive?
            while (isActive) {
                if (!selected) {
                    when (val selectResponse = torrentsRepository.selectFiles(id, files)) {
                        is EitherResult.Failure -> {
                            if (selectResponse.failure is EmptyBodyError) {
                                Timber.d("Select torrent files success returned ${selectResponse.failure.returnCode}")
                                selected = true
                            } else {
                                Timber.e("Exception during torrent files selection call: ${selectResponse.failure}")
                            }
                        }
                        is EitherResult.Success -> {
                            Timber.d("Select torrent files success")
                            selected = true
                        }
                    }
                } else {
                    val torrentItem: TorrentItem? = torrentsRepository.getTorrentInfo(id)
                    if (torrentItem != null) {
                        if (!beforeSelectionStatusList.contains(torrentItem.status)) {
                            job.cancelIfActive()
                            torrentLiveData.postEvent(TorrentEvent.FilesSelected(torrentItem))
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    fun pollTorrentStatus() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // check if it goes into select files
                // todo: create a service to do this, check the download one
            }
        }, 0, 1000)
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
    object CacheMiss : TorrentEvent()
    data class FilesSelected(val torrent: TorrentItem) : TorrentEvent()
}