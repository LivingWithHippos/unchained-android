package com.github.livingwithhippos.unchained.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val hostsRepository: HostsRepository,
    private val pluginRepository: PluginRepository
) : ViewModel() {

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
            hostsRepository.updateFoldersRegex()
        }
    }

    fun removeExternalPlugins(context: Context): Int =
        pluginRepository.removeExternalPlugins(context)

}