package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationRepository
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import kotlinx.coroutines.*

//todo: add state saving and loading
class AuthenticationViewModel @ViewModelInject constructor(
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    val authLiveData = MutableLiveData<Authentication?>()
    val secretLiveData = MutableLiveData<Secrets?>()
    val tokenLiveData = MutableLiveData<Token?>()

    //todo: here we should check if we already have credentials and if they work, and pass those
    //todo: rename this first part of the auth flow as verificationInfo etc.?
    fun fetchAuthenticationInfo() {
        scope.launch {
            val authData = authRepository.getVerificationCode()
            authLiveData.postValue(authData)
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
        }
    }

    fun fetchSecrets(deviceCode: String) {
        val waitTime = 5000L
        var calls = 0
        scope.launch {
            var secretData = authRepository.getSecrets(deviceCode)
            secretLiveData.postValue(secretData)
            while (secretData?.clientId == null && calls < 60) {
                delay(waitTime)
                secretData = authRepository.getSecrets(deviceCode)
                calls++
            }
            if (secretData?.clientId != null) {
                secretLiveData.postValue(secretData)
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
                //todo: manage calls reaching limit time in the ui
            }
        }

    }

    fun fetchToken(clientId: String, deviceCode: String, clientSecret: String) {
        //todo: should we blank unnecessary secrets when we have a working token?
        scope.launch {
            val tokenData = authRepository.getToken(clientId, clientSecret, deviceCode)
            tokenLiveData.postValue(tokenData)
            if (tokenData?.accessToken != null) {
                credentialRepository.updateCredentials(
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

    fun cancelRequests() = job.cancel()
}