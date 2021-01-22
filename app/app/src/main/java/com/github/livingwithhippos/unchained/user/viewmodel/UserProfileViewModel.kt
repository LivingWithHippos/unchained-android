package com.github.livingwithhippos.unchained.user.viewmodel


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {

        viewModelScope.launch {
            val token = credentialsRepository.getToken()

            val user = userRepository.getUserInfo(token)
            userLiveData.postValue(user)
        }
    }

}