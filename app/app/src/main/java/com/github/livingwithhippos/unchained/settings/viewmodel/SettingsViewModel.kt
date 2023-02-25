package com.github.livingwithhippos.unchained.settings.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.repository.HostsRepository
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel.Companion.KEY_DOWNLOAD_FOLDER
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository,
    private val kodiRepository: KodiRepository,
    private val protoStore: ProtoStore,
    private val preferences: SharedPreferences
) : ViewModel() {

    val kodiLiveData = MutableLiveData<Event<Boolean>>()

    val eventLiveData = MutableLiveData<Event<SettingEvent>>()

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
            hostsRepository.updateFoldersRegex()
        }
    }

    fun removeAllPlugins(context: Context): Int = pluginRepository.removeInstalledPlugins(context)

    fun testKodi(ip: String, port: Int, username: String?, password: String?) {
        viewModelScope.launch {
            val response = kodiRepository.getVolume(ip, port, username, password)
            kodiLiveData.postEvent(response != null)
        }
    }

    fun setDownloadFolder(uri: Uri) {
        uri.describeContents()
        with(preferences.edit()) {
            putString(KEY_DOWNLOAD_FOLDER, uri.toString())
            apply()
        }
    }

    fun userLogout() {
        viewModelScope.launch {
            protoStore.deleteCredentials()
            eventLiveData.postEvent(SettingEvent.Logout)
        }
    }
}

sealed class SettingEvent {
    object Logout : SettingEvent()
}
