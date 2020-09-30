package com.github.livingwithhippos.unchained.lists.viewmodel


import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.lists.model.TorrentPagingSource
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment.Companion.TAB_DOWNLOADS
import com.github.livingwithhippos.unchained.utilities.Event
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed to populate lists with paging support
 */
class DownloadListViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val credentialsRepository: CredentialsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    // note: this value (pageSize) is triplicated when the first call is made. Yes it does, no I don't know why.
    val downloadsLiveData: LiveData<PagingData<DownloadItem>> = Pager(PagingConfig(pageSize = 10)) {
        DownloadPagingSource(downloadRepository, credentialsRepository)
    }.liveData.cachedIn(viewModelScope)

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> = Pager(PagingConfig(pageSize = 10)) {
        TorrentPagingSource(torrentsRepository, credentialsRepository)
    }.liveData.cachedIn(viewModelScope)

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem?>>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int?>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int?>>()

    fun downloadTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val items = unrestrictRepository.getUnrestrictedLinkList(token, torrent.links)
            downloadItemLiveData.postValue(Event(items))
        }
    }

    fun downloadTorrent(links: List<String>) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val items = unrestrictRepository.getUnrestrictedLinkList(token, links)
            downloadItemLiveData.postValue(Event(items))
        }
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = torrentsRepository.deleteTorrent(token, id)
            deletedTorrentLiveData.postValue(Event(deleted))
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = downloadRepository.deleteTorrent(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postValue(Event(-1))
            else
                deletedDownloadLiveData.postValue(Event(1))
        }
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle.set(KEY_SELECTED_TAB,tabID)
    }

    fun getSelectedTab(): Int {
        return savedStateHandle.get(KEY_SELECTED_TAB) ?: TAB_DOWNLOADS
    }

    companion object {
        const val KEY_SELECTED_TAB = "selected_tab_key"
    }

}