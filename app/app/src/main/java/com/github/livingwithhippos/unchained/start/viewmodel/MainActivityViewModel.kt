package com.github.livingwithhippos.unchained.start.viewmodel

import android.annotation.SuppressLint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.AuthenticationState
import com.github.livingwithhippos.unchained.data.model.Credentials
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import kotlinx.coroutines.launch

/**
 * a [ViewModel] subclass.
 * Shared between the fragments to observe the authentication status and update it.
 */
class MainActivityViewModel @ViewModelInject constructor(
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authenticationState = MutableLiveData<Event<AuthenticationState>>()

    val userLiveData = MutableLiveData<User?>()

    // fixme: this is here because userLiveData.postValue(user) is throwing an unsafe error
    //  but auto-correcting it changes the value of val authenticationState = MutableLiveData<Event<AuthenticationState>>() to a nullable one
    @SuppressLint("NullSafeMutableLiveData")
    fun fetchFirstWorkingCredentials() {
        viewModelScope.launch {
            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId != null && it.clientSecret != null && it.deviceCode.isNotBlank() && it.refreshToken != null }
            var user: User? = null
            if (completeCredentials.isNotEmpty()) {
                val privateCredentials =
                    completeCredentials.firstOrNull { it.deviceCode == PRIVATE_TOKEN }

                if (privateCredentials != null) {
                    user = checkCredentials(privateCredentials)
                }
                // if the private token is not working this also gets triggered
                if (user == null)
                    for (cred in completeCredentials) {
                        user = checkCredentials(cred)
                        if (user != null) {
                            break
                        }
                    }
            }
            // passes null if no working credentials, otherwise pass the first working one
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
                }
            }
        }
    }

}