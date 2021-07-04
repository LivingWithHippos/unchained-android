package com.github.livingwithhippos.unchained.lists.viewmodel

import TorrentPagingSource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment.Companion.TAB_DOWNLOADS
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed to populate lists with paging support
 */
@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    // stores the last query value
    private val queryLiveData = MutableLiveData<String>()

    // items are filtered returning only if their names contain the query
    val downloadsLiveData: LiveData<PagingData<DownloadItem>> =
        Transformations.switchMap(queryLiveData) { query: String ->
            Pager(PagingConfig(pageSize = 50, initialLoadSize = 100)) {
                DownloadPagingSource(downloadRepository, credentialsRepository, query)
            }.liveData.cachedIn(viewModelScope)
        }

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> =
        Transformations.switchMap(queryLiveData) { query: String ->
            Pager(PagingConfig(pageSize = 50, initialLoadSize = 100)) {
                TorrentPagingSource(torrentsRepository, credentialsRepository, query)
            }.liveData.cachedIn(viewModelScope)
        }

    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem>>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    fun downloadTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val items = unrestrictRepository.getUnrestrictedLinkList(token, torrent.links)
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>()
                    .map { it.failure }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty())
                errorsLiveData.postEvent(errors)
        }
    }

    fun downloadTorrentFolder(torrent: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val items = unrestrictRepository.getUnrestrictedLinkList(token, torrent.links)
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>()
                    .map { it.failure }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty())
                errorsLiveData.postEvent(errors)
        }
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = torrentsRepository.deleteTorrent(token, id)
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
            val token = credentialsRepository.getToken()
            val deleted = downloadRepository.deleteDownload(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postEvent(DOWNLOAD_NOT_DELETED)
            else
                deletedDownloadLiveData.postEvent(DOWNLOAD_DELETED)
        }
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle.set(KEY_SELECTED_TAB, tabID)
    }

    fun getSelectedTab(): Int {
        return savedStateHandle.get(KEY_SELECTED_TAB) ?: TAB_DOWNLOADS
    }

    fun setListFilter(query: String?) {
        // Avoid updating the lists if the query hasn't changed. We don't check for cases but we could
        if (queryLiveData.value != query)
            queryLiveData.postValue(query?.trim() ?: "")
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {

            deletedDownloadLiveData.postEvent(0)

            val token = credentialsRepository.getToken()
            val completeDownloadList = mutableListOf<DownloadItem>()
            do {
                val downloads = downloadRepository.getDownloads(token, 0, 1, 50)
                completeDownloadList.addAll(downloads)
            } while (downloads.size >= 50)

            // post a message every 10% of the deletion progress if there are more than 10 items
            val progressIndicator: Int =
                if (completeDownloadList.size / 10 < 15) 15 else completeDownloadList.size / 10

            completeDownloadList.forEachIndexed { index, item ->
                downloadRepository.deleteDownload(token, item.id)
                if ((index + 1) % progressIndicator == 0)
                    deletedDownloadLiveData.postEvent(index + 1)
            }

            deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED_ALL)
        }
    }

    fun deleteAllTorrents() {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            do {
                val torrents = torrentsRepository.getTorrentsList(token, 0, 1, 50)
                torrents.forEach {
                    torrentsRepository.deleteTorrent(token, it.id)
                }
            } while (torrents.size >= 50)

            deletedTorrentLiveData.postEvent(TORRENTS_DELETED_ALL)
        }
    }

    fun deleteTorrents(torrents: List<TorrentItem>) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            torrents.forEach {
                torrentsRepository.deleteTorrent(token, it.id)
            }
            if (torrents.size > 1)
                deletedTorrentLiveData.postEvent(TORRENTS_DELETED)
            else
                deletedTorrentLiveData.postEvent(TORRENT_DELETED)
        }
    }

    fun downloadItems(torrents: List<TorrentItem>) {
        torrents.filter { it.status == "downloaded" }
            .forEach {
                downloadTorrent(it)
            }
    }

    fun deleteDownloads(downloads: List<DownloadItem>) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            downloads.forEach {
                downloadRepository.deleteDownload(token, it.id)
            }
            if (downloads.size > 1)
                deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED)
            else
                deletedDownloadLiveData.postEvent(DOWNLOAD_DELETED)
        }
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
