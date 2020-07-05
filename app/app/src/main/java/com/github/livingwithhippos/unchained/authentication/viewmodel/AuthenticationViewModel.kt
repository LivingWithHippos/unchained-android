package com.github.livingwithhippos.unchained.authentication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationRepository
import com.github.livingwithhippos.unchained.authentication.model.Secrets
import com.github.livingwithhippos.unchained.authentication.model.Token
import com.github.livingwithhippos.unchained.base.model.database.UnchaindeDB
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.network.ApiAuthFactory
import kotlinx.coroutines.*

//todo: add state saving and loading
class AuthenticationViewModel(application: Application) : AndroidViewModel(application) {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    private val repository: AuthenticationRepository =
        AuthenticationRepository(
            ApiAuthFactory.authApi
        )

    private val credentialRepository : CredentialsRepository

    init {
        val credentialsDao = UnchaindeDB.getDatabase(application).credentialsDao()
        credentialRepository = CredentialsRepository(credentialsDao)
    }

    val authLiveData = MutableLiveData<Authentication?>()
    val secretLiveData = MutableLiveData<Secrets?>()
    val tokenLiveData = MutableLiveData<Token?>()

    //todo: here we should check if we already have credentials and if they work, and pass those
    //todo: rename this first part of the auth flow as verificationInfo etc.?
    fun fetchAuthenticationInfo() {
        scope.launch {
            val authData = repository.getVerificationCode()
            authLiveData.postValue(authData)
            if (authData?.deviceCode != null)
                credentialRepository.insert(Credentials(authData.deviceCode,null,null, null,null))
        }
    }

    fun fetchSecrets(deviceCode: String) {
        val waitTime = 5000L
        var calls = 0
        scope.launch {
            var secretData = repository.getSecrets(deviceCode)
            secretLiveData.postValue(secretData)
            while (secretData?.clientId == null && calls < 60) {
                delay(waitTime)
                secretData = repository.getSecrets(deviceCode)
                calls++
            }
            if (secretData != null) {
                secretLiveData.postValue(secretData)
                credentialRepository.updateSecrets(deviceCode,secretData.clientId,secretData.clientSecret)
            } else {
                //todo: manage calls reaching limit time in the ui
            }
        }

    }

    fun fetchToken(clientId: String, deviceCode: String, clientSecret: String){
        //todo: should we blank unnecessary secrets when we have a working token?
        scope.launch {
            val tokenData = repository.getToken(clientId, clientSecret, deviceCode)
            tokenLiveData.postValue(tokenData)
            if (tokenData?.accessToken != null)
                credentialRepository.updateToken(deviceCode,tokenData.accessToken,tokenData.refreshToken)
        }
    }

    fun cancelRequests() = job.cancel()
}