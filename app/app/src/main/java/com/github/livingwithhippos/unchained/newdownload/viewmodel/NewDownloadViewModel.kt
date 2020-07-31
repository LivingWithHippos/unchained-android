package com.github.livingwithhippos.unchained.newdownload.viewmodel

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.TorrentsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UnrestrictRepository
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictedLink
import com.github.livingwithhippos.unchained.newdownload.model.UploadedTorrent
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NewDownloadViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val unrestrictRepository: UnrestrictRepository,
    private val torrentsRepository: TorrentsRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    /**
     * We can't use a normal MutableLiveData here because while navigating back an event will be fired again
     * and the [NewDownloadFragment] observer will be called, creating a new [DownloadDetailsFragment]
     * and navigating there.
     * See https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150 for mode details
     */
    val linkLiveData = MutableLiveData<Event<UnrestrictedLink?>>()
    val torrentLiveData = MutableLiveData<Event<UploadedTorrent?>>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        scope.launch {
            //todo: add this to fragment's argument if possible
            val token = getToken()
            val unrestrictedData =
                unrestrictRepository.getUnrestrictedLink(token, link, password, remote)
            linkLiveData.postValue(Event(unrestrictedData))
        }
    }

    fun fetchAddedMagnet(magnet: String) {
        scope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                Log.e("NewDownloadViewModel", "Error fetching available hosts")
            } else {
                val addedMagnet =
                    torrentsRepository.addMagnet(token, magnet, availableHosts.first().host)
                if (addedMagnet != null) {
                    // todo: add custom selection of files, this queues all the files
                    //todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    //todo: get file info and check if it has already been downloaded before doing a select files
                    //todo: for magnets it's not possible to call selectFiles immediately because the file list is still missing, first we have magnet_conversion
                    // maybe calling it in the next fragment when the status is files_selection is right, remove selectFiles from here?
                    torrentsRepository.selectFiles(token, addedMagnet.id)
                    torrentLiveData.postValue(Event(addedMagnet))
                }
            }
        }
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        scope.launch {
            val token = getToken()
            val availableHosts = torrentsRepository.getAvailableHosts(token)
            if (availableHosts.isNullOrEmpty()) {
                Log.e("NewDownloadViewModel", "Error fetching available hosts")
            } else {
                val uploadedTorrent =
                    torrentsRepository.addTorrent(token, binaryTorrent, availableHosts.first().host)
                if (uploadedTorrent != null) {
                    // todo: add custom selection of files, this queues all the files
                    //todo: add checks for already chosen torrent/magnet (if possible), otherwise we get multiple downloads
                    torrentsRepository.selectFiles(token, uploadedTorrent.id)
                    torrentLiveData.postValue(Event(uploadedTorrent))
                }
            }
        }
    }

    private suspend fun getToken(): String {
        var token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty())
            token = credentialsRepository.getCompleteCredentials().first().accessToken
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")

        savedStateHandle.set(KEY_TOKEN, token)
        return token
    }
}