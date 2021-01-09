package com.github.livingwithhippos.unchained.newdownload.viewmodel

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed while creating new downloads
 */
class NewDownloadViewModel @ViewModelInject constructor(
    private val credentialsRepository: CredentialsRepository,
    private val unrestrictRepository: UnrestrictRepository,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    // use Event since navigating back to this fragment would trigger this observable again
    val linkLiveData = MutableLiveData<Event<DownloadItem>>()
    val torrentLiveData = MutableLiveData<Event<UploadedTorrent>>()
    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        viewModelScope.launch {
            val token = getToken()
            val response =
                unrestrictRepository.getEitherUnrestrictedLink(token, link, password, remote)
            when (response) {
                is Either.Left -> networkExceptionLiveData.postEvent(response.a)
                is Either.Right -> linkLiveData.postEvent(response.b)
            }
        }
    }

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                if (BuildConfig.DEBUG)
                    Log.e("NewDownloadViewModel", "Error fetching available hosts")
            } else {
                val addedMagnet =
                    torrentsRepository.addMagnet(token, magnet, availableHosts.first().host)
                if (addedMagnet != null) {
                    // todo: add custom selection of files, this queues all the files
                    //todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    //todo: get file info and check if it has already been downloaded before doing a select files
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
                if (BuildConfig.DEBUG)
                    Log.e("NewDownloadViewModel", "Error fetching available hosts")
            } else {
                val uploadedTorrent =
                    torrentsRepository.addTorrent(token, binaryTorrent, availableHosts.first().host)
                if (uploadedTorrent != null) {
                    //todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    torrentLiveData.postEvent(uploadedTorrent)
                }
            }
        }
    }

    private suspend fun getToken(): String {
        val token = credentialsRepository.getToken()
        if (token.isBlank())
            throw IllegalArgumentException("Loaded token was null or empty: $token")
        return token
    }
}