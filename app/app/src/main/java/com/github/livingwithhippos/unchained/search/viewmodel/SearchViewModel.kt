package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val preferences: SharedPreferences,
    private val pluginRepository: PluginRepository,
    private val parser: Parser
) : ViewModel() {

    val resultLiveData = MutableLiveData<List<LinkData>>()
    val pluginLiveData = MutableLiveData<List<Plugin>>()

    fun search(query: String, pluginName: String, category: String? = null) {
        viewModelScope.launch {
            if (pluginLiveData.value.isNullOrEmpty())
                fetchPlugins()
            else {
                val plugin = pluginLiveData.value?.firstOrNull { it.name == pluginName }
                if (plugin != null) {
                    val result: ParserResult = parser.search(
                        plugin = plugin,
                        query = query,
                        category = category
                    )
                    // todo: add errors
                    when (result) {
                        is ParserResult.Result -> {
                            resultLiveData.postValue(result.values)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun fetchPlugins() {
        viewModelScope.launch {
            val plugins = pluginRepository.getPlugins()
            pluginLiveData.postValue(plugins)
        }
    }
}