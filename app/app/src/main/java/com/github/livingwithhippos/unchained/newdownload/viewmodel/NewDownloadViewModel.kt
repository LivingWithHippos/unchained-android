package com.github.livingwithhippos.unchained.newdownload.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import com.github.livingwithhippos.unchained.base.model.repositories.UnrestrictRepository
import com.github.livingwithhippos.unchained.newdownload.model.UnrestrictedLink
import com.github.livingwithhippos.unchained.utilities.KEY_TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NewDownloadViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val credentialsRepository: CredentialsRepository,
    private val unrestrictRepository: UnrestrictRepository
) : ViewModel() {

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.Default + job)

    val linkLiveData = MutableLiveData<UnrestrictedLink?>()

    fun fetchUnrestrictedLink(link: String, password: String?, remote: Int? = null) {
        scope.launch {
            //todo: add this to fragment's argument if possible
            var token = savedStateHandle.get<String>(KEY_TOKEN)
            if (token.isNullOrEmpty())
                token = credentialsRepository.getCompleteCredentials().first().accessToken
            if (token.isNullOrEmpty())
                throw IllegalArgumentException("Loaded token was null or empty: $token")

            savedStateHandle.set(KEY_TOKEN, token)
            val unrestrictedData =
                unrestrictRepository.getUnrestrictedLink(token, link, password, remote)
            linkLiveData.postValue(unrestrictedData)
        }
    }
}