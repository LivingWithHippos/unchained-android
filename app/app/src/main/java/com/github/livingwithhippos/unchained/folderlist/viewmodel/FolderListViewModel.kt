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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val progressLiveData = MutableLiveData<Int>()

    // used to simulate a debounce effect while typing on the search bar
    private var queryJob: Job? = null

    // stores the last query value
    val queryLiveData = MutableLiveData<String>()

    fun retrieveFolderFileList(folderLink: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()

            val filesList: Either<UnchainedNetworkException, List<String>> =
                unrestrictRepository.getEitherFolderLinks(token, folderLink)

            when (filesList) {
                is Either.Left -> errorsLiveData.postEvent(filesList.value)
                is Either.Right -> retrieveFiles(filesList.value)
            }
        }
    }

    fun retrieveFiles(links: List<String>) {
        viewModelScope.launch {

            val token = credentialsRepository.getToken()
            // either first time or there were some errors, re-download
            if (links.size != getRetrievedLinks()) {

                val hitList = mutableListOf<DownloadItem>()

                links.forEachIndexed { index, link ->
                    when (
                        val file =
                            unrestrictRepository.getEitherUnrestrictedLink(token, link)
                    ) {
                        is Either.Left -> {
                            errorsLiveData.postEvent(file.value)
                            progressLiveData.postValue((index + 1) * 100 / links.size)
                        }
                        is Either.Right -> {
                            hitList.add(file.value)
                            folderLiveData.postEvent(hitList)
                            setRetrievedLinks(hitList.size)
                            progressLiveData.postValue((index + 1) * 100 / links.size)
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

    fun filterList(query: String?) {
        // simulate debounce
        queryJob?.cancel()

        queryJob = viewModelScope.launch {
            delay(500)
            queryLiveData.postValue(query?.trim() ?: "")
        }
    }

    companion object {
        const val KEY_RETRIEVED_LINKS = "retrieve_links"
    }
}
