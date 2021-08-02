package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.User
import com.github.livingwithhippos.unchained.data.repositoy.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val protoStore: ProtoStore
) : ViewModel() {

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {

        viewModelScope.launch {
            protoStore.credentialsFlow.collect {
                val user = userRepository.getUserInfo(it.accessToken)
                userLiveData.postValue(user)
            }
        }
    }
}