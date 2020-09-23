package com.github.livingwithhippos.unchained.user.viewmodel


import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UserRepository
import com.github.livingwithhippos.unchained.user.model.User
import kotlinx.coroutines.launch

class UserProfileViewModel @ViewModelInject constructor(
    private val userRepository: UserRepository,
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {

        viewModelScope.launch {
            //todo: try and move the token to the okHttp interceptor
            val token = credentialsRepository.getToken()

            val user = userRepository.getUserInfo(token)
            userLiveData.postValue(user)
        }
    }

}