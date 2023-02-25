package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class UserProfileViewModel
@Inject
constructor(private val userRepository: UserRepository, private val protoStore: ProtoStore) :
    ViewModel() {

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {

        viewModelScope.launch {
            val credentials = protoStore.getCredentials()
            val user = userRepository.getUserInfo(credentials.accessToken)
            userLiveData.postValue(user)
        }
    }
}
