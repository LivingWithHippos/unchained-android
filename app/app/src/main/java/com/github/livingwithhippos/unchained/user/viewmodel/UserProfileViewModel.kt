package com.github.livingwithhippos.unchained.user.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.network.ApiFactory
import com.github.livingwithhippos.unchained.user.model.User
import com.github.livingwithhippos.unchained.user.model.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserProfileViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {


    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    private val repository: UserRepository =
        UserRepository(
            ApiFactory.userApi
        )

    val userLiveData = MutableLiveData<User>()

    fun fetchUserInfo() {
        scope.launch {
            val user = repository.getUserInfo()
            userLiveData.postValue(user)
        }
    }

    fun cancelRequests() = job.cancel()

}