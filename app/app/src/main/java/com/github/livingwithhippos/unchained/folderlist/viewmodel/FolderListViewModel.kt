package com.github.livingwithhippos.unchained.folderlist.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val unrestrictRepository: UnrestrictRepository,
    private val credentialsRepository: CredentialsRepository,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {

    val folderLiveData = MutableLiveData<Event<List<DownloadItem>>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()
    val errorsLiveData = MutableLiveData<Event<UnchainedNetworkException>>()

    fun retrieveFolderFileList(folderLink: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()

            val filesList: Either<UnchainedNetworkException, List<String>> =
                unrestrictRepository.getEitherFolderLinks(token, folderLink)

            when (filesList) {
                is Either.Left -> errorsLiveData.postEvent(filesList.a)
                is Either.Right -> retrieveFiles(token, filesList.b)
            }
        }
    }

    fun retrieveTorrentFileList(item: TorrentItem) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()

            retrieveFiles(token, item.links)
        }
    }

    private suspend fun retrieveFiles(token: String, links: List<String>) {
        // either first time or there were some errors, re-download
        if (links.size != getRetrievedLinks()) {

            val hitList = mutableListOf<DownloadItem>()

            links.forEach {
                when (val file =
                    unrestrictRepository.getEitherUnrestrictedLink(token, it)) {
                    is Either.Left -> {
                        errorsLiveData.postEvent(file.a)
                    }
                    is Either.Right -> {
                        hitList.add(file.b)
                        folderLiveData.postEvent(hitList)
                        setRetrievedLinks(hitList.size)
                    }
                }
            }
        } else {
            // I already downloaded all the files, repost the last value
            folderLiveData.value?.let {
                folderLiveData.postEvent(it.peekContent())
            }
        }

    }

    fun setRetrievedLinks(links: Int) {
        savedStateHandle.set(KEY_RETRIEVED_LINKS, links)
    }

    fun getRetrievedLinks(): Int {
        return savedStateHandle.get(KEY_RETRIEVED_LINKS) ?: -1
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

    companion object {
        const val KEY_RETRIEVED_LINKS = "retrieve_links"
    }
}