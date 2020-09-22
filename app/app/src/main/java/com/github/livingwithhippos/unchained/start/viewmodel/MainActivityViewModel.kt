package com.github.livingwithhippos.unchained.start.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//todo: evaluate if this ViewModel could be used as a shared one between the fragments
class MainActivityViewModel @ViewModelInject constructor(
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    //todo: move out of this class
    //todo: separate AUTHENTICATED in AUTHENTICATED_PRIVATE and AUTHENTICATED_OPEN to make it easier to inform the user of unavailable app api
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, BAD_TOKEN, ACCOUNT_LOCKED
    }

    val workingCredentialsLiveData = MutableLiveData<Credentials?>()

    val authenticationState = MutableLiveData<Event<AuthenticationState>>()

    fun fetchFirstWorkingCredentials() {
        viewModelScope.launch {
            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId != null && it.clientSecret != null && it.deviceCode.isNotBlank() && it.refreshToken != null }
            var workingCredentials: Credentials? = null
            if (completeCredentials.isNotEmpty()) {
                val privateCredentials =
                    completeCredentials.firstOrNull { it.deviceCode == PRIVATE_TOKEN }

                if (privateCredentials != null) {
                    if (checkCredentials(privateCredentials))
                        workingCredentials = privateCredentials
                }
                // if the private token is not working this also gets triggered
                //todo: add network check
                if (workingCredentials == null)
                    for (cred in completeCredentials) {
                        if (checkCredentials(cred)) {
                            workingCredentials = cred
                            break
                        }
                    }
            }
            // passes null if no working credentials, otherwise pass the first working one
            workingCredentialsLiveData.postValue(workingCredentials)
        }
    }

    private suspend fun checkCredentials(credentials: Credentials): Boolean {
        if (credentials.accessToken != null) {
            val user = userRepository.getUserInfo(credentials.accessToken)
            return user != null
        } else
            throw IllegalArgumentException("Credentials parameter has null access token")
        // todo: needs to check if it's a network error or if token has expired etc.
    }

    fun setAuthenticated() {
        authenticationState.postValue(Event(AuthenticationState.AUTHENTICATED))
    }

    fun setUnauthenticated() {
        //todo: delete active credentials?
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
                }
            }
        }
    }

}