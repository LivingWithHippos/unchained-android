package com.github.livingwithhippos.unchained.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import kotlinx.coroutines.launch

class SettingsViewmodel @ViewModelInject constructor(
    private val hostsRepository: HostsRepository
) : ViewModel() {

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
        }
    }
}