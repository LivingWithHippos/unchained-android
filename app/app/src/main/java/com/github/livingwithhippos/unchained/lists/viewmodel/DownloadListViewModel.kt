package com.github.livingwithhippos.unchained.lists.viewmodel


import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
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
import arrow.core.Either
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.lists.model.TorrentPagingSource
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment.Companion.TAB_DOWNLOADS
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
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

    private val queryLiveData = MutableLiveData<String>()

    val downloadsLiveData: LiveData<PagingData<DownloadItem>> = Transformations.switchMap(queryLiveData) { query: String ->
        // note: this value (pageSize) is triplicated when the first call is made. Yes it does, no I don't know why.
        Pager(PagingConfig(pageSize = 50)) {
            DownloadPagingSource(downloadRepository, credentialsRepository, query)
        }.liveData.cachedIn(viewModelScope)
    }

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> = Transformations.switchMap(queryLiveData) { query: String ->
        Pager(PagingConfig(pageSize = 50)) {
            TorrentPagingSource(torrentsRepository, credentialsRepository, query)
        }.liveData.cachedIn(viewModelScope)
    }

    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem?>>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int?>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int?>>()

    fun downloadTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val items = unrestrictRepository.getUnrestrictedLinkList(token, torrent.links)
            val values = items.filterIsInstance<Either.Right<DownloadItem>>().map { it.b }
            val errors =
                items.filterIsInstance<Either.Left<UnchainedNetworkException>>().map { it.a }

            downloadItemLiveData.postValue(Event(values))
            if (errors.isNotEmpty())
                errorsLiveData.postValue(Event(errors))
        }
    }

    fun deleteTorrent(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = torrentsRepository.deleteTorrent(token, id)
            when (deleted) {
                is Either.Left -> {
                    errorsLiveData.postEvent(listOf(deleted.a))
                }
                is Either.Right -> {
                    deletedTorrentLiveData.postEvent(204)
                }
            }
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            val deleted = downloadRepository.deleteDownload(token, id)
            if (deleted == null)
                deletedDownloadLiveData.postEvent(-1)
            else
                deletedDownloadLiveData.postEvent(1)
        }
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle.set(KEY_SELECTED_TAB, tabID)
    }

    fun getSelectedTab(): Int {
        return savedStateHandle.get(KEY_SELECTED_TAB) ?: TAB_DOWNLOADS
    }

    fun setListFilter(query: String?) {
        // we don't check for cases but we could
        if (queryLiveData.value != query)
            queryLiveData.postValue(query?.trim() ?: "")
    }

    companion object {
        const val KEY_SELECTED_TAB = "selected_tab_key"
    }

}