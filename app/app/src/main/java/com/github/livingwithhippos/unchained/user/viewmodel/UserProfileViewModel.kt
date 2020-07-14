package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserProfileViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)


    fun saveToken(token: String) {
        savedStateHandle.set(KEY_TOKEN, token)
    }

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {
        val token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")
        scope.launch {
            //todo: try and move the token to the okhttp interceptor
            val user = userRepository.getUserInfo(token)
            userLiveData.postValue(user)
        }
    }

    fun cancelRequests() = job.cancel()

}