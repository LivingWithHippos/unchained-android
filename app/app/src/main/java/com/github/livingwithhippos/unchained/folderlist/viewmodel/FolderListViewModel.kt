package com.github.livingwithhippos.unchained.folderlist.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.DownloadRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderListViewModel @Inject constructor(
    private val unrestrictRepository: UnrestrictRepository,
    private val credentialsRepository: CredentialsRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val folderLiveData = MutableLiveData<List<DownloadItem>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    fun retrieveFileList(folderLink: String) {
        viewModelScope.launch {
            val token = credentialsRepository.getToken()

            val filesList: List<Either<UnchainedNetworkException, DownloadItem>> = unrestrictRepository.getEitherUnrestrictedFolder(token, folderLink)

            val hitList = mutableListOf<DownloadItem>()
            val missList = mutableListOf<UnchainedNetworkException>()

            filesList.forEach {
                if (it is Either.Right)
                    hitList.add(it.b)
                else
                    missList.add((it as Either.Left).a)
            }

            folderLiveData.postValue(hitList)

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
}