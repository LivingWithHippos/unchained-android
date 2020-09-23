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
            authLiveData.postValue(Event(authData))
        }
    }

    /**
     * @param deviceCode: the device code assigned calling the authentication endpoint
     * @param expireIn: the time in seconds before the deviceCode is not valid anymore for the secrets endpoint
     */
    fun fetchSecrets(deviceCode: String, expireIn: Int) {
        val waitTime = 5000L
        // this is just an estimate, keeping track of time would be more precise. As of now this value should be 120
        var calls = (expireIn*1000/waitTime).toInt()-10
        // remove 10% of the calls to account for the api calls
        calls -= calls/10
        viewModelScope.launch {
            var secretData = authRepository.getSecrets(deviceCode)
            secretLiveData.postValue(Event(secretData))
            while (secretData?.clientId == null && calls-- > 0 && getAuthState() != MainActivityViewModel.AuthenticationState.AUTHENTICATED) {
                delay(waitTime)
                secretData = authRepository.getSecrets(deviceCode)
                calls++
            }
            if (secretData?.clientId != null) {
                secretLiveData.postValue(Event(secretData))
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

            }
        }
    }

    fun checkAndSaveToken(privateKey: String? = null, token: Token?= null) {
        viewModelScope.launch {

            if (privateKey == null && token == null)
                throw IllegalArgumentException("checkAndSaveToken: passed tokens were both null")

            // try to get user info
            val user: User? = userRepository.getUserInfo(privateKey ?: token!!.accessToken)

            if (user != null) {
                if (privateKey != null)
                    credentialRepository.insertPrivateToken(privateKey)
                else {
                    val deviceCode = authLiveData.value?.peekContent()?.deviceCode
                    val clientId = secretLiveData.value?.peekContent()?.clientId
                    val clientSecret = secretLiveData.value?.peekContent()?.clientSecret
                    if (deviceCode!=null && clientId!=null && clientSecret!=null)
                        credentialRepository.insert(
                            Credentials(
                                deviceCode = deviceCode,
                                clientId = clientId,
                                clientSecret = clientSecret,
                                accessToken = token!!.accessToken,
                                refreshToken = token.refreshToken
                            )
                        )
                }
            }

            // alert the observing fragment of the result
            userLiveData.postValue(Event(user))
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