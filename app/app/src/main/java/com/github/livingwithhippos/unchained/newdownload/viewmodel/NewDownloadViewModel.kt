package com.github.livingwithhippos.unchained.newdownload.viewmodel

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.APIException
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UnrestrictRepository
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.utilities.Event
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

    /**
     * We can't use a normal MutableLiveData here because while navigating back an event will be fired again
     * and the [NewDownloadFragment] observer will be called, creating a new [DownloadDetailsFragment]
     * and navigating there.
     * See https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150 for mode details
     */
    val linkLiveData = MutableLiveData<Event<DownloadItem?>>()
    val torrentLiveData = MutableLiveData<Event<UploadedTorrent?>>()
    val apiErrorLiveData = MutableLiveData<Event<APIError?>>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        viewModelScope.launch {
            val token = getToken()
            try {
                val unrestrictedData =
                    unrestrictRepository.getUnrestrictedLink(token, link, password, remote)
                linkLiveData.postValue(Event(unrestrictedData))
            } catch (e: APIException) {
                apiErrorLiveData.postValue(Event(e.apiError))
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
                    torrentLiveData.postValue(Event(addedMagnet))
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
                    torrentLiveData.postValue(Event(uploadedTorrent))
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