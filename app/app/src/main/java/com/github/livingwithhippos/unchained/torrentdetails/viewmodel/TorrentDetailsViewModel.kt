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
import com.github.livingwithhippos.unchained.utilities.endedStatusList
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

/**
 * a [ViewModel] subclass.
 * Retrieves a torrent's details
 */
@HiltViewModel
class TorrentDetailsViewModel @Inject constructor(
    private val torrentsRepository: TorrentsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    val torrentLiveData = MutableLiveData<Event<TorrentItem?>>()
    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val downloadLiveData = MutableLiveData<Event<DownloadItem?>>()
    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    private var job = Job()

    fun pollTorrentStatus(id: String) {
        // todo: test if I need to recreate a job when it is cancelled
        job.cancelIfActive()
        job = Job()

        val scope = CoroutineScope(job + Dispatchers.IO)

        scope.launch {
            /// maybe job.isActive?
            while (isActive) {
                val torrentData = torrentsRepository.getTorrentInfo(id)
                if (endedStatusList.contains(torrentData?.status))
                    job.cancelIfActive()

                delay(2000)
            }
        }
    }

    fun stopPolling() {
        job.cancelIfActive()
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            when (val deleted = torrentsRepository.deleteTorrent(id)) {
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
            torrentLiveData.value?.let { torrent ->
                val links = torrent.peekContent()?.links
                if (links != null) {
                    val items = unrestrictRepository.getUnrestrictedLinkList(links)

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
