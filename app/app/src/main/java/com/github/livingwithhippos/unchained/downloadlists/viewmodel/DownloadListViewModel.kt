package com.github.livingwithhippos.unchained.downloadlists.viewmodel

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
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.DownloadRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import com.github.livingwithhippos.unchained.downloadlists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DownloadListViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    val listData: LiveData<PagingData<DownloadItem>> = Pager(PagingConfig(pageSize = 10)) {
        DownloadPagingSource(downloadRepository,credentialsRepository)
    }.liveData.cachedIn(viewModelScope)

}