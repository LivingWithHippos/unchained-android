package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Authentication
import com.github.livingwithhippos.unchained.data.model.Secrets
import com.github.livingwithhippos.unchained.data.model.Token
import com.github.livingwithhippos.unchained.data.repositoy.AuthenticationRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [ViewModel] subclass.
 * It offers LiveData to be observed during the authentication process and uses the [AuthenticationRepository] to manage its process.
 */
@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
    private val protoStore: ProtoStore,
) : ViewModel() {

    val authLiveData = MutableLiveData<Event<Authentication?>>()
    val secretLiveData = MutableLiveData<Event<SecretResult>>()
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
     */
    fun fetchSecrets(deviceCode: String) {
        // check how many calls we've made
        val calls = savedStateHandle.get<Int>(SECRET_CALLS) ?: 0
        val maxCalls = savedStateHandle.get<Int>(SECRET_CALLS_MAX) ?: 108
        if (calls >= maxCalls) {
            secretLiveData.postEvent(SecretResult.Expired)
        } else {
            viewModelScope.launch {
                val secretData = authRepository.getSecrets(deviceCode)
                if (secretData != null)
                    secretLiveData.postEvent(SecretResult.Retrieved(secretData))
                else {
                    delay(SECRET_CALLS_DELAY)
                    secretLiveData.postEvent(SecretResult.Empty)
                }
            }
        }

    }

    fun fetchToken() {
        viewModelScope.launch {
            val credentials = protoStore.credentialsFlow.single()
            val tokenData = authRepository.getToken(credentials.clientId, credentials.clientSecret, credentials.deviceCode)
            tokenLiveData.postEvent(tokenData)
        }
    }

    fun setAuthState(authenticated: Boolean) {
        savedStateHandle.set(AUTH_STATE, authenticated)
    }

    private fun getAuthState(): Boolean {
        return savedStateHandle.get(AUTH_STATE) ?: false
    }

    /**
     *
     * @param expiresIn: the time in seconds before the deviceCode is not valid anymore for the secrets endpoint
     */
    fun setupSecretLoop(expiresIn: Int) {
        // this is just an estimate, keeping track of time would be more precise. As of now this value should be 120
        var calls = (expiresIn * 1000 / SECRET_CALLS_DELAY).toInt() - 10
        // remove 10% of the calls to account for the api calls
        calls -= calls / 10
        savedStateHandle.set(SECRET_CALLS_MAX, calls)
        savedStateHandle.set(SECRET_CALLS, 0)
    }

    companion object {
        const val AUTH_STATE = "auth_state"
        const val SECRET_CALLS = "secret_calls"
        const val SECRET_CALLS_MAX = "max_secret_calls"

        // 5 seconds is the value suggested by real debrid
        const val SECRET_CALLS_DELAY = 5000L
    }
}

sealed class SecretResult {
    object Empty : SecretResult()
    object Expired : SecretResult()
    data class Retrieved(val value: Secrets) : SecretResult()
}