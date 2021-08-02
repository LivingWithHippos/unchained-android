package com.github.livingwithhippos.unchained.newdownload.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed while creating new downloads
 */
@HiltViewModel
class NewDownloadViewModel @Inject constructor(
    private val unrestrictRepository: UnrestrictRepository,
    private val torrentsRepository: TorrentsRepository,
    private val hostsRepository: HostsRepository,
    private val protoStore: ProtoStore,
) : ViewModel() {

    // use Event since navigating back to this fragment would trigger this observable again
    val linkLiveData = MutableLiveData<Event<DownloadItem>>()
    val folderLiveData = MutableLiveData<Event<String>>()
    val torrentLiveData = MutableLiveData<Event<UploadedTorrent>>()
    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val containerLiveData = MutableLiveData<Event<Link>>()
    val toastLiveData = MutableLiveData<Event<String>>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        viewModelScope.launch {
            val token = getToken()
            // check if it's a folder link
            var isFolder = false
            for (hostRegex in hostsRepository.getFoldersRegex()) {
                val m: Matcher = Pattern.compile(hostRegex.regex).matcher(link)
                if (m.matches()) {
                    isFolder = true
                    folderLiveData.postEvent(link)
                    break
                }
            }
            if (!isFolder) {
                val response =
                    unrestrictRepository.getEitherUnrestrictedLink(token, link, password, remote)
                when (response) {
                    is EitherResult.Failure -> networkExceptionLiveData.postEvent(response.failure)
                    is EitherResult.Success -> linkLiveData.postEvent(response.success)
                }
            }
        }
    }

    fun uploadContainer(container: ByteArray) {
        viewModelScope.launch {
            val token = getToken()
            val fileList = unrestrictRepository.uploadContainer(token, container)
            if (fileList != null)
                containerLiveData.postEvent(Link.Container(fileList))
            else
                containerLiveData.postEvent(Link.RetrievalError)
        }
    }

    fun unrestrictContainer(link: String) {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken
            val links = unrestrictRepository.getContainerLinks(token, link)
            if (links != null)
                containerLiveData.postEvent(Link.Container(links))
            else
                containerLiveData.postEvent(Link.RetrievalError)
        }
    }

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val addedMagnet =
                    torrentsRepository.addMagnet(token, magnet, availableHosts.first().host)
                if (addedMagnet != null) {
                    // todo: add custom selection of files, this queues all the files
                    // todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    // todo: get file info and check if it has already been downloaded before doing a select files
                    torrentLiveData.postEvent(addedMagnet)
                }
            }
        }
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val uploadedTorrent =
                    torrentsRepository.addTorrent(token, binaryTorrent, availableHosts.first().host)
                if (uploadedTorrent != null) {
                    // todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    torrentLiveData.postEvent(uploadedTorrent)
                }
            }
        }
    }

    private suspend fun getToken(): String {
        val token = protoStore.getCredentials().accessToken
        if (token.isBlank())
            throw IllegalArgumentException("Loaded token was null or empty: $token")
        return token
    }

    fun postMessage(message: String) {
       toastLiveData.postEvent(message)
    }
}

sealed class Link {
    data class Host(val link: String) : Link()
    data class Folder(val link: String) : Link()
    data class Magnet(val link: String) : Link()
    data class Torrent(val link: String) : Link()
    data class Container(val links: List<String>) : Link()
    object RetrievalError : Link()
}
