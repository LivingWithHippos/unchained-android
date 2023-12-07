package com.github.livingwithhippos.unchained.lists.viewmodel

import TorrentPagingSource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.utilities.DOWNLOADS_TAB
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass. It offers LiveData to be observed to populate lists with paging support
 */
@HiltViewModel
class ListTabsViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    // stores the last query value
    private val queryLiveData = MutableLiveData<String>()

    // items are filtered returning only if their names contain the query
    val downloadsLiveData: LiveData<PagingData<DownloadItem>> =
        queryLiveData.switchMap { query: String ->
            Pager(PagingConfig(pageSize = 50, initialLoadSize = 100)) {
                DownloadPagingSource(downloadRepository, query)
            }
                .liveData
                .cachedIn(viewModelScope)
        }

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> =
        queryLiveData.switchMap { query: String ->
            Pager(PagingConfig(pageSize = 50, initialLoadSize = 100)) {
                TorrentPagingSource(torrentsRepository, query)
            }
                .liveData
                .cachedIn(viewModelScope)
        }

    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem>>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    val eventLiveData = MutableLiveData<Event<ListEvent>>()

    /**
     * Un restrict a torrent and move it to the download section
     *
     * @param torrent
     */
    fun unrestrictTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val items = unrestrictRepository.getUnrestrictedLinkList(torrent.links)
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }

    fun downloadTorrentFolder(torrent: TorrentItem) {
        viewModelScope.launch {
            val items = unrestrictRepository.getUnrestrictedLinkList(torrent.links)
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val deleted = torrentsRepository.deleteTorrent(id)
            when (deleted) {
                is EitherResult.Failure -> {
                    errorsLiveData.postEvent(listOf(deleted.failure))
                    deletedTorrentLiveData.postEvent(TORRENT_NOT_DELETED)
                }
                is EitherResult.Success -> {
                    deletedTorrentLiveData.postEvent(TORRENT_DELETED)
                }
            }
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val deleted = downloadRepository.deleteDownload(id)
            if (deleted == null) deletedDownloadLiveData.postEvent(DOWNLOAD_NOT_DELETED)
            else deletedDownloadLiveData.postEvent(DOWNLOAD_DELETED)
        }
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle.set(KEY_SELECTED_TAB, tabID)
    }

    fun getSelectedTab(): Int {
        return savedStateHandle.get(KEY_SELECTED_TAB) ?: DOWNLOADS_TAB
    }

    fun setListFilter(query: String?) {
        // Avoid updating the lists if the query hasn't changed. We don't check for cases but we
        // could
        if (queryLiveData.value != query) queryLiveData.postValue(query?.trim() ?: "")
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            deletedDownloadLiveData.postEvent(0)
            var page = 1
            val completeDownloadList = mutableListOf<DownloadItem>()
            do {
                val downloads = downloadRepository.getDownloads(0, page++, 50)
                completeDownloadList.addAll(downloads)
            } while (downloads.size >= 50)

            // post a message every 10% of the deletion progress if there are more than 10 items
            val progressIndicator: Int =
                if (completeDownloadList.size / 10 < 15) 15 else completeDownloadList.size / 10

            completeDownloadList.forEachIndexed { index, item ->
                downloadRepository.deleteDownload(item.id)
                if ((index + 1) % progressIndicator == 0)
                    deletedDownloadLiveData.postEvent(index + 1)
            }

            deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED_ALL)
        }
    }

    fun deleteAllTorrents() {
        viewModelScope.launch {
            do {
                val torrents = torrentsRepository.getTorrentsList(0, 1, 50)
                torrents.forEach { torrentsRepository.deleteTorrent(it.id) }
            } while (torrents.size >= 50)

            deletedTorrentLiveData.postEvent(TORRENTS_DELETED_ALL)
        }
    }

    fun deleteTorrents(torrents: List<TorrentItem>) {
        viewModelScope.launch {
            torrents.forEach { torrentsRepository.deleteTorrent(it.id) }
            if (torrents.size > 1) deletedTorrentLiveData.postEvent(TORRENTS_DELETED)
            else deletedTorrentLiveData.postEvent(TORRENT_DELETED)
        }
    }

    fun downloadItems(torrents: List<TorrentItem>) {
        torrents.filter { it.status == "downloaded" }.forEach { unrestrictTorrent(it) }
    }

    fun deleteDownloads(downloads: List<DownloadItem>) {
        viewModelScope.launch {
            downloads.forEach { downloadRepository.deleteDownload(it.id) }
            if (downloads.size > 1) deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED)
            else deletedDownloadLiveData.postEvent(DOWNLOAD_DELETED)
        }
    }

    fun postEventNotice(event: ListEvent) {
        eventLiveData.postEvent(event)
    }

    companion object {
        const val KEY_SELECTED_TAB = "selected_tab_key"
        const val TORRENT_DELETED = -1
        const val TORRENTS_DELETED = -2
        const val TORRENTS_DELETED_ALL = -3
        const val TORRENT_NOT_DELETED = -4
        const val DOWNLOAD_DELETED = -1
        const val DOWNLOADS_DELETED = -2
        const val DOWNLOADS_DELETED_ALL = -3
        const val DOWNLOAD_NOT_DELETED = -4
    }
}

sealed class ListEvent {
    data class DownloadItemClick(val item: DownloadItem) : ListEvent()
    data class TorrentItemClick(val item: TorrentItem) : ListEvent()
    data class OpenTorrent(val item: TorrentItem) : ListEvent()
    data class SetTab(val tab: Int) : ListEvent()
}
