package com.github.livingwithhippos.unchained.repository.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.PluginVersion
import com.github.livingwithhippos.unchained.data.model.RepositoryInfo
import com.github.livingwithhippos.unchained.data.model.RepositoryPlugin
import com.github.livingwithhippos.unchained.data.repository.CustomDownloadRepository
import com.github.livingwithhippos.unchained.data.repository.DatabasePluginRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val pluginsRepository: DatabasePluginRepository,
    private val downloadRepository: CustomDownloadRepository,
) : ViewModel() {
    val pluginsRepositoryLiveData = MutableLiveData<Event<PluginRepositoryEvent>>()

    fun checkCurrentRepositories() {
        viewModelScope.launch {
            val repositories = pluginsRepository.getRepositoriesLink()
            for (repo in repositories) {
                when (val info = downloadRepository.downloadPluginRepository(repo.link)) {
                    is EitherResult.Failure -> {
                        Timber.w("Error downloading repo at ${repo.link}: ${info.failure}")
                    }
                    is EitherResult.Success -> {
                        pluginsRepository.saveRepositoryInfo(
                            repo.link,
                            info.success
                        )
                    }
                }
            }
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.Updated
            )
        }
    }

    fun retrieveDatabaseRepositories() {
        viewModelScope.launch {
            val repo = pluginsRepository.getFullRepositoriesData()
            pluginsRepositoryLiveData.postEvent(
                PluginRepositoryEvent.FullData(repo)
            )
        }
    }
}

sealed class PluginRepositoryEvent {
    object Updated: PluginRepositoryEvent()
    data class FullData(val data: Map<RepositoryInfo, Map<RepositoryPlugin, List<PluginVersion>>>) : PluginRepositoryEvent()
}