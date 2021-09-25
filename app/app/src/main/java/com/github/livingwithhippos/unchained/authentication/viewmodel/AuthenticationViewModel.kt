package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed during the authentication process and uses the [AuthenticationRepository] to manage its process.
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    val authLiveData = MutableLiveData<Event<Authentication?>>()
    val secretLiveData = MutableLiveData<Event<Secrets>>()
    val tokenLiveData = MutableLiveData<Event<Token?>>()

    // todo: here we should check if we already have credentials and if they work, and pass those
    fun fetchAuthenticationInfo() {
        viewModelScope.launch {
            val authData = authRepository.getVerificationCode()
            authLiveData.postEvent(authData)
        }
    }

    /**
     * @param deviceCode: the device code assigned calling the authentication endpoint
     * @param expireIn: the time in seconds before the deviceCode is not valid anymore for the secrets endpoint
     */
    fun fetchSecrets(deviceCode: String, expireIn: Int) {
        // 5 seconds is the value suggested by real debrid
        val waitTime = 5000L
        // this is just an estimate, keeping track of time would be more precise. As of now this value should be 120
        var calls = (expireIn * 1000 / waitTime).toInt() - 10
        // remove 10% of the calls to account for the api calls
        calls -= calls / 10
        viewModelScope.launch {
            var secretData = authRepository.getSecrets(deviceCode)

            while (
                secretData?.clientId == null &&
                calls-- > 0 &&
                !getAuthState()
            ) {
                delay(waitTime)
                secretData = authRepository.getSecrets(deviceCode)
            }

            if (secretData?.clientId != null) {
                secretLiveData.postEvent(secretData)
            } else {
                // if the authentication link has expired before the user confirmation, request a new one
                if (calls <= 0)
                    fetchAuthenticationInfo()
            }
        }
    }

    fun fetchToken(clientId: String, deviceCode: String, clientSecret: String) {
        viewModelScope.launch {
            val tokenData = authRepository.getToken(clientId, clientSecret, deviceCode)
            tokenLiveData.postEvent(tokenData)
        }
    }

    fun setAuthState(authenticated: Boolean) {
        savedStateHandle.set(AUTH_STATE, authenticated)
    }

    private fun getAuthState(): Boolean {
        return savedStateHandle.get(AUTH_STATE) ?: false
    }

    companion object {
        const val AUTH_STATE = "auth_state"
    }
}
