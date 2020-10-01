package com.github.livingwithhippos.unchained.start.viewmodel

import android.annotation.SuppressLint
import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import com.github.livingwithhippos.unchained.lists.view.ListsTabFragment
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * a [ViewModel] subclass.
 * Shared between the fragments to observe the authentication status and update it.
 */
class MainActivityViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authenticationState = MutableLiveData<Event<AuthenticationState>>()

    val userLiveData = MutableLiveData<User?>()

    val externalLinkLiveData = MutableLiveData<Event<Uri?>>()

    val downloadedTorrentLiveData = MutableLiveData<Event<String?>>()

    val listStateLiveData = MutableLiveData<Event<ListsTabFragment.ListState>>()

    // fixme: this is here because userLiveData.postValue(user) is throwing an unsafe error
    //  but auto-correcting it changes the value of val authenticationState = MutableLiveData<Event<AuthenticationState>>() to a nullable one
    @SuppressLint("NullSafeMutableLiveData")
    fun fetchFirstWorkingCredentials() {
        viewModelScope.launch {

            var user: User? = null

            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId != null && it.clientSecret != null && it.deviceCode.isNotBlank() && it.refreshToken != null }


            if (completeCredentials.isNotEmpty()) {

                // step #1: test for private API token
                completeCredentials.firstOrNull { it.deviceCode == PRIVATE_TOKEN }?.let{
                    user = checkCredentials(it)
                }
                // step #2: test for open source credentials
                if (user == null) {
                    completeCredentials.firstOrNull { it.deviceCode != PRIVATE_TOKEN }?.let{
                        user = checkCredentials(it)
                    }
                }
                // step #3: try to refresh open source credentials
                if (user == null) {
                    // to check values refreshToken was ported here
                    completeCredentials.firstOrNull { it.deviceCode != PRIVATE_TOKEN }?.let{
                        // refresh the token
                        authRepository.refreshToken(it)?.let {token->
                            val newCredentials = Credentials(
                                it.deviceCode,
                                it.clientId,
                                it.clientSecret,
                                token.accessToken,
                                token.refreshToken
                            )

                            user = userRepository.getUserInfo(token.accessToken)

                            if (user != null) {
                                // update the credentials
                                credentialRepository.updateCredentials(newCredentials)
                                // program the refresh of the token
                                programTokenRefresh(token.expiresIn)
                            }

                        }
                    }
                }

            }

            // pass whatever user was retrieved, or null if none was found
            userLiveData.postValue(user)

        }
    }

    private suspend fun checkCredentials(credentials: Credentials): User? {
        if (credentials.accessToken != null) {
            return userRepository.getUserInfo(credentials.accessToken)
        } else
            throw IllegalArgumentException("Credentials parameter has null access token")
        // todo: needs to check if it's a network error or if token has expired etc.
    }

    fun setAuthenticated() {
        authenticationState.postValue(Event(AuthenticationState.AUTHENTICATED))
    }

    fun setAuthenticatedNoPremium() {
        authenticationState.postValue(Event(AuthenticationState.AUTHENTICATED_NO_PREMIUM))
    }

    fun setUnauthenticated() {
        authenticationState.postValue(Event(AuthenticationState.UNAUTHENTICATED))
    }

    fun logout() {
        viewModelScope.launch {
            credentialRepository.deleteAllCredentials()
            setUnauthenticated()
        }
    }

    fun invalidateOpenSourceToken() {
        viewModelScope.launch {
            credentialRepository.getFirstCredentials()?.let {
                if (it.refreshToken != null && it.refreshToken != PRIVATE_TOKEN) {
                    //setUnauthenticated()
                    authRepository.disableToken(it.accessToken!!)
                }

            }
        }
    }

    suspend fun isTokenPrivate(): Boolean {
        val credentials = credentialRepository.getFirstCredentials()
        return credentials?.refreshToken == PRIVATE_TOKEN
    }

    suspend fun deleteIncompleteCredentials() = credentialRepository.deleteIncompleteCredentials()

    fun refreshToken() {

        viewModelScope.launch {

            // get the old credentials
            val oldCredentials = credentialRepository.getFirstCredentials()
            // check if they are private API token credentials
            if (oldCredentials != null && oldCredentials.refreshToken != PRIVATE_TOKEN) {
                // refresh the token
                authRepository.refreshToken(oldCredentials)?.let {
                    val newCredentials = Credentials(
                        oldCredentials.deviceCode,
                        oldCredentials.clientId,
                        oldCredentials.clientSecret,
                        it.accessToken,
                        it.refreshToken
                    )
                    // update the credentials
                    credentialRepository.updateCredentials(newCredentials)

                    // program the refresh of the token
                    programTokenRefresh(it.expiresIn)
                }
            }
        }
    }

    fun addLink(uri: Uri) {
        externalLinkLiveData.postValue(Event(uri))
    }

    fun setDownload(downloadID: Long, filePath: String) {
        savedStateHandle.set(KEY_TORRENT_DOWNLOAD_ID, downloadID)
        savedStateHandle.set(KEY_TORRENT_PATH, filePath)
    }

    fun checkDownload(downloadID: Long) {
        val id = savedStateHandle.get<Long>(KEY_TORRENT_DOWNLOAD_ID)
        if (id == downloadID) {
            val fileName = savedStateHandle.get<String>(KEY_TORRENT_PATH)
            if (fileName != null)
                downloadedTorrentLiveData.postValue(Event(fileName))
        }
    }

    //todo: move this stuff to a shared navigationViewModel
    fun setListState(state: ListsTabFragment.ListState) {
        listStateLiveData.postValue(Event(state))
    }

    fun getLastBackPress(): Long {
        return savedStateHandle.get<Long>(KEY_LAST_BACK_PRESS) ?: 0
    }

    fun setLastBackPress(time: Long) {
        savedStateHandle.set(KEY_LAST_BACK_PRESS, time)
    }

    fun programTokenRefresh(secondsDelay: Int) {
        // todo: add job that is cancelled everytime this function is called
        viewModelScope.launch {
            // secondsDelay*950L -> expiration time - 5%
            delay(secondsDelay * 950L)
            refreshToken()
        }
    }

    companion object {
        const val KEY_TORRENT_DOWNLOAD_ID = "torrent_download_id_key"
        const val KEY_TORRENT_PATH = "torrent_path_key"
        const val KEY_LAST_BACK_PRESS = "last_back_press_key"
    }

}