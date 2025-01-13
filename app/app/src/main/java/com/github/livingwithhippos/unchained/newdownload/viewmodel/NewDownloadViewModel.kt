package com.github.livingwithhippos.unchained.newdownload.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

/** A [ViewModel] subclass. It offers LiveData to be observed while creating new downloads */
class NewDownloadViewModel(
    private val unrestrictRepository: UnrestrictRepository,
    private val torrentsRepository: TorrentsRepository,
    private val hostsRepository: HostsRepository,
) : ViewModel() {

    // use Event since navigating back to this fragment would trigger this observable again
    val downloadLiveData = MutableLiveData<Event<DownloadItem>>()
    val folderLiveData = MutableLiveData<Event<String>>()
    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val linkLiveData = MutableLiveData<Event<Link>>()
    val toastLiveData = MutableLiveData<Event<String>>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        viewModelScope.launch {
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
                    unrestrictRepository.getEitherUnrestrictedLink(link, password, remote)
                when (response) {
                    is EitherResult.Failure -> networkExceptionLiveData.postEvent(response.failure)
                    is EitherResult.Success -> downloadLiveData.postEvent(response.success)
                }
            }
        }
    }

    fun uploadContainer(container: ByteArray) {
        viewModelScope.launch {
            when (val fileList = unrestrictRepository.uploadContainer(container)) {
                is EitherResult.Failure -> {
                    networkExceptionLiveData.postEvent(fileList.failure)
                }
                is EitherResult.Success -> {
                    linkLiveData.postEvent(Link.Container(fileList.success))
                }
            }
        }
    }

    fun unrestrictContainer(link: String) {
        viewModelScope.launch {
            val links = unrestrictRepository.getContainerLinks(link)
            if (links != null) linkLiveData.postEvent(Link.Container(links))
            else linkLiveData.postEvent(Link.RetrievalError)
        }
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            val availableHosts = torrentsRepository.getAvailableHosts()
            if (availableHosts.isNullOrEmpty()) {
                Timber.e("Error fetching available hosts")
            } else {
                val uploadedTorrent =
                    torrentsRepository.addTorrent(binaryTorrent, availableHosts.first().host)
                when (uploadedTorrent) {
                    is EitherResult.Failure -> {
                        networkExceptionLiveData.postEvent(uploadedTorrent.failure)
                    }
                    is EitherResult.Success -> {
                        // todo: add checks for already chosen torrent/magnet (if possible),
                        // otherwise we get
                        // multiple downloads
                        linkLiveData.postEvent(Link.Torrent(uploadedTorrent.success))
                    }
                }
            }
        }
    }

    /**
     * This function is used to manage multiple toast spawning from different parts of the logic to
     * avoid the queue getting too long and a lot of messages being shown, see the collect on the
     * fragment
     */
    fun postMessage(message: String) {
        toastLiveData.postEvent(message)
    }
}

sealed class Link {
    data class Host(val link: String) : Link()

    data class Torrent(val upload: UploadedTorrent) : Link()

    data class Container(val links: List<String>) : Link()

    data object RetrievalError : Link()
}
