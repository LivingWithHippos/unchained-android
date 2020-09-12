package com.github.livingwithhippos.unchained.start.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.entities.Credentials
import com.github.livingwithhippos.unchained.base.model.repositories.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.utilities.PRIVATE_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//todo: evaluate if this ViewModel could be used as a shared one between the fragments
class MainActivityViewModel @ViewModelInject constructor(
    private val authRepository: AuthenticationRepository,
    private val credentialRepository: CredentialsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, BAD_TOKEN, ACCOUNT_LOCKED
    }

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    val workingCredentialsLiveData = MutableLiveData<Credentials?>()

    val authenticationState = MutableLiveData<AuthenticationState> ()

    fun fetchFirstWorkingCredentials() {
        scope.launch {
            val completeCredentials = credentialRepository
                .getAllCredentials()
                .filter { it.accessToken != null && it.clientId != null && it.clientSecret != null && it.deviceCode.isNotBlank() && it.refreshToken != null }
            var workingCredentials: Credentials? = null
            if (completeCredentials.isNotEmpty()) {
                val privateCredentials =
                    if (completeCredentials.any { it.deviceCode == PRIVATE_TOKEN })
                        completeCredentials.first { it.deviceCode == PRIVATE_TOKEN }
                    else null

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

}