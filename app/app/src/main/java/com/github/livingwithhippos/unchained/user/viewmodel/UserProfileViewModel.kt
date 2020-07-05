package com.github.livingwithhippos.unchained.user.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

//todo: add loading of saved state
class UserProfileViewModel(application: Application) : AndroidViewModel(application) {


    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    private lateinit var repository: UserRepository

    fun initRepository(token: String) {
        repository =
            UserRepository(
                ApiFactory(token).userApi
            )
    }

    val userLiveData = MutableLiveData<User?>()

    fun fetchUserInfo() {
        scope.launch {
            val user = repository.getUserInfo()
            userLiveData.postValue(user)
        }
    }

    fun cancelRequests() = job.cancel()

}