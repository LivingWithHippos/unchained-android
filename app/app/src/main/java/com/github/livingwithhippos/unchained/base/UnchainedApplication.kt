package com.github.livingwithhippos.unchained.base

import android.app.Application
import com.github.livingwithhippos.unchained.base.model.repositories.CredentialsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

//todo: move under right package

@HiltAndroidApp
class UnchainedApplication : Application() {

    @Inject lateinit var credentialsRepository: CredentialsRepository

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            credentialsRepository.deleteIncompleteCredentials()
        }
    }
}