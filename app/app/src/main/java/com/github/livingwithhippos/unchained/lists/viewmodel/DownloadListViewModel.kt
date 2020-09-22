package com.github.livingwithhippos.unchained.lists.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.*
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.DownloadRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UnrestrictRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadItem
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.lists.model.TorrentPagingSource
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.utilities.Event
import kotlinx.coroutines.launch

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

    val downloadItemLiveData = MutableLiveData<Event<DownloadItem?>>()

    fun downloadTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()
            // todo: manage torrents with multiple links (huge ones?)
            val item = unrestrictRepository.getUnrestrictedLink(token, torrent.links[0])
            downloadItemLiveData.postValue(Event(item))
        }

    }

}