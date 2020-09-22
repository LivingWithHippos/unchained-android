package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.utilities.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//todo: add state saving and loading
class AuthenticationViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val authLiveData = MutableLiveData<Event<Authentication?>>()
    val secretLiveData = MutableLiveData<Event<Secrets?>>()
    val tokenLiveData = MutableLiveData<Event<Token?>>()
    val userLiveData = MutableLiveData<Event<User?>>()

    //todo: here we should check if we already have credentials and if they work, and pass those
    //todo: rename this first part of the auth flow as verificationInfo etc.?
    fun fetchAuthenticationInfo() {
        viewModelScope.launch {
            val authData = authRepository.getVerificationCode()
            if (authData?.deviceCode != null)
                credentialRepository.insert(
                    Credentials(
                        authData.deviceCode,
                        null,
                        null,
                        null,
                        null
                    )
                )
            authLiveData.postValue(Event(authData))
        }
    }

    fun fetchSecrets(deviceCode: String) {
        val waitTime = 5000L
        var calls = 0
        viewModelScope.launch {
            var secretData = authRepository.getSecrets(deviceCode)
            secretLiveData.postValue(Event(secretData))
            while (secretData?.clientId == null && calls < 60 && getAuthState() != MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                //todo: stop calling if authenticated via private token
                delay(waitTime)
                secretData = authRepository.getSecrets(deviceCode)
                calls++
            }
            if (secretData?.clientId != null) {
                secretLiveData.postValue(Event(secretData))
                credentialRepository.updateCredentials(
                    Credentials(
                        deviceCode = deviceCode,
                        clientId = secretData.clientId,
                        clientSecret = secretData.clientSecret,
                        accessToken = null,
                        refreshToken = null
                    )
                )
            } else {
                //todo: manage calls reaching limit time in the ui or ignore if authenticated through the private token
            }
        }

    }

    fun fetchToken(clientId: String, deviceCode: String, clientSecret: String) {
        //todo: should we blank unnecessary secrets when we have a working token?
        viewModelScope.launch {
            val tokenData = authRepository.getToken(clientId, clientSecret, deviceCode)
            tokenLiveData.postValue(Event(tokenData))
            if (tokenData?.accessToken != null) {
                // i need only a set of credentials in my application
                //todo: check this when adding private api token
                credentialRepository.deleteAllOpenSourceCredentials()
                credentialRepository.insert(
                    Credentials(
                        deviceCode = deviceCode,
                        clientId = clientId,
                        clientSecret = clientSecret,
                        accessToken = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken

                    )
                )
            }
        }
    }

    fun checkAndSaveToken(token: String) {
        viewModelScope.launch {
            // try to get personal info
            val userData = userRepository.getUserInfo(token)
            // save the token if it's working
            if (userData != null)
                credentialRepository.insertPrivateToken(token)
            // alert the observing fragment of the result
            userLiveData.postValue(Event(userData))
        }
    }

    fun setAuthState(state: MainActivityViewModel.AuthenticationState) {
        savedStateHandle.set(AUTH_STATE, state)
    }

    private fun getAuthState(): MainActivityViewModel.AuthenticationState? {
        // this value is only checked against AUTHENTICATED
        return savedStateHandle.get(AUTH_STATE)
    }

    companion object {
        const val AUTH_STATE = "auth_state"
    }
}