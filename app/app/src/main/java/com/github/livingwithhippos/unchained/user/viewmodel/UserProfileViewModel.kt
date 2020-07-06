package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.user.model.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

//todo: add loading of saved state
class UserProfileViewModel @ViewModelInject constructor(
    private val userRepository: UserRepository
) : ViewModel() {


    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)


    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {
        scope.launch {
            val user = userRepository.getUserInfo()
            userLiveData.postValue(user)
        }
    }

    fun cancelRequests() = job.cancel()

}