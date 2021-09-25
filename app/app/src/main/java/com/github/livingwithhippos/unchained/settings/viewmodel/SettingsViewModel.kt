package com.github.livingwithhippos.unchained.settings.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
    private val kodiRepository: KodiRepository
) : ViewModel() {

    val kodiLiveData = MutableLiveData<Event<Boolean>>()

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
            hostsRepository.updateFoldersRegex()
        }
    }

    fun removeExternalPlugins(context: Context): Int =
        pluginRepository.removeExternalPlugins(context)

    fun testKodi(ip: String, port: Int, username: String?, password: String?) {
        viewModelScope.launch {
            val response = kodiRepository.getVolume(ip, port, username, password)
            kodiLiveData.postEvent(response != null)
        }
    }
}
