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
import java.util.Timer
import java.util.TimerTask
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
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val torrentLiveData = MutableLiveData<Event<TorrentEvent>>()
    val structureLiveData = MutableLiveData<Event<Node<TorrentFileItem>>>()

    private var job = Job()

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val availableHosts = torrentsRepository.getAvailableHosts()
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val addedMagnet = torrentsRepository.addMagnet(magnet, availableHosts.first().host)
                when (addedMagnet) {
                    is EitherResult.Failure -> {
                        networkExceptionLiveData.postEvent(addedMagnet.failure)
                    }
                    is EitherResult.Success -> {
                        setTorrentID(addedMagnet.success.id)
                        torrentLiveData.postEvent(TorrentEvent.Uploaded(addedMagnet.success))
                    }
                }
            }
        }
    }

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

    fun checkTorrentCache(hash: String) {
        viewModelScope.launch {
            val builder = StringBuilder(BASE_URL)
            builder.append(INSTANT_AVAILABILITY_ENDPOINT)
            builder.append("/")
            builder.append(hash)
            when (val cache = torrentsRepository.getInstantAvailability(builder.toString())) {
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

    private fun setTorrentDetails(item: TorrentItem) {
        savedStateHandle[KEY_CURRENT_TORRENT] = item
    }

    fun getTorrentID(): String? {
        return savedStateHandle[KEY_CURRENT_TORRENT_ID]
    }

    private fun setTorrentID(id: String) {
        savedStateHandle[KEY_CURRENT_TORRENT_ID] = id
    }

    fun getCache(): CachedTorrent? {
        return savedStateHandle[KEY_CACHE]
    }

    private fun setCache(cache: CachedTorrent) {
        savedStateHandle[KEY_CACHE] = cache
    }

    fun updateTorrentStructure(structure: Node<TorrentFileItem>?) {
        if (structure != null) structureLiveData.postEvent(structure)
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
            // / maybe job.isActive?
            while (isActive) {
                if (!selected) {
                    when (val selectResponse = torrentsRepository.selectFiles(id, files)) {
                        is EitherResult.Failure -> {
                            if (selectResponse.failure is EmptyBodyError) {
                                Timber.d(
                                    "Select torrent files success returned ${selectResponse.failure.returnCode}")
                                selected = true
                            } else {
                                Timber.e(
                                    "Exception during torrent files selection call: ${selectResponse.failure}")
                            }
                        }
                        is EitherResult.Success -> {
                            Timber.d("Select torrent files success")
                            selected = true
                        }
                    }
                }

                if (selected) {
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
        Timer()
            .scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        // check if it goes into select files
                        // todo: create a service to do this, check the download one
                    }
                },
                0,
                1000)
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
        const val KEY_CURRENT_TORRENT_STRUCTURE = "current_torrent_structure_key"
    }
}

sealed class TorrentEvent {
    data class Uploaded(val torrent: UploadedTorrent) : TorrentEvent()

    data class TorrentInfo(val item: TorrentItem) : TorrentEvent()

    data class CacheHit(val cache: CachedTorrent) : TorrentEvent()

    object CacheMiss : TorrentEvent()

    data class FilesSelected(val torrent: TorrentItem) : TorrentEvent()

    object DownloadAll : TorrentEvent()

    data class DownloadCache(val position: Int, val files: Int) : TorrentEvent()

    data class DownloadSelection(val filesNumber: Int) : TorrentEvent()

    object DownloadedFileSuccess : TorrentEvent()

    object DownloadedFileFailure : TorrentEvent()

    data class DownloadedFileProgress(val progress: Int) : TorrentEvent()
}
