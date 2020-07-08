package com.github.livingwithhippos.unchained.start.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.user.model.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

//todo: evaluate if this ViewModel could be used as a shared one with the fragments
class MainActivityViewModel @ViewModelInject constructor(
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    val workingCredentialsLiveData = MutableLiveData<Credentials?>()

    fun fetchFirstWorkingCredentials() {
        scope.launch {
            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId!= null && it.clientSecret!= null && it.deviceCode.isNotBlank() && it.refreshToken!= null }
            var workingCredentials: Credentials? = null
            if (completeCredentials.isNotEmpty()) {
                for (cred in completeCredentials) {
                    if(checkCredentials(cred)) {
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
        var user: User? = null
        if (credentials.accessToken != null)
            user = userRepository.getUserInfo(credentials.accessToken)
        else
            throw IllegalArgumentException("Credentials parameter has null access token")
        // todo: needs to check if it's a network error or if token has expired etc.
        return user != null
    }
}