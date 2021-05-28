package com.github.livingwithhippos.unchained.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository.Companion.TYPE_UNCHAINED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewmodel @Inject constructor(
    private val hostsRepository: HostsRepository
) : ViewModel() {

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
            hostsRepository.updateFoldersRegex()
        }
    }

    fun removeExternalPlugins(context: Context): Int {
        return try {
            val plugins = context.filesDir.listFiles { _, name ->
                name.endsWith(TYPE_UNCHAINED)
            }

            plugins?.forEach {
                it.delete()
            }

            plugins?.size ?: -1

        } catch (e: SecurityException) {
            Timber.d("Security exception deleting plugins files: ${e.message}")
            -1
        }
    }
}