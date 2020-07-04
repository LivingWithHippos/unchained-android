package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.authentication.model.Authentication
import com.github.livingwithhippos.unchained.authentication.model.AuthenticationRepository
import com.github.livingwithhippos.unchained.base.network.ApiAuthFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AuthenticationViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    private val repository: AuthenticationRepository =
        AuthenticationRepository(
            ApiAuthFactory.authApi
        )

    val authLiveData = MutableLiveData<Authentication?>()

    //todo: here we should check if we already have credentials and if they work, nad pass those
    //todo: rename this first part of the auth flow as verificationInfo etc.?
    fun fetchAuthenticationInfo() {
        scope.launch {
            val authData = repository.getVerificationCode()
            authLiveData.postValue(authData)
        }
    }

    fun cancelRequests() = job.cancel()
}