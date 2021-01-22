package com.github.livingwithhippos.unchained.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.HostsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewmodel @Inject constructor(
    private val hostsRepository: HostsRepository
) : ViewModel() {

    fun updateRegexps() {
        viewModelScope.launch {
            hostsRepository.updateHostsRegex()
        }
    }
}