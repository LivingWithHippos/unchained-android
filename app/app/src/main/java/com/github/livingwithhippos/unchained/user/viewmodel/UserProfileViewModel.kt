package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.launch

class UserProfileViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    fun saveToken(token: String) {
        savedStateHandle.set(KEY_TOKEN, token)
    }

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {
        val token = savedStateHandle.get<String>(KEY_TOKEN)
        if (token.isNullOrEmpty())
            throw IllegalArgumentException("Loaded token was null or empty: $token")

        viewModelScope.launch {
            //todo: try and move the token to the okhttp interceptor
            val user = userRepository.getUserInfo(token)
            userLiveData.postValue(user)
        }
    }

}