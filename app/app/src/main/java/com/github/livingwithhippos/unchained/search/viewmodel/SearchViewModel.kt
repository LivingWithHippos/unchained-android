package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val pluginRepository: PluginRepository,
    private val parser: Parser
) : ViewModel() {

    // used to simulate a debounce effect while typing on the search bar
    private var job: Job? = null

    val pluginLiveData = MutableLiveData<List<Plugin>>()
    private val parsingLiveData = MutableLiveData<ParserResult>()

    fun completeSearch(
        query: String,
        pluginName: String,
        category: String? = null,
        page: Int = 1
    ): LiveData<ParserResult> {

        val plugin = pluginLiveData.value?.firstOrNull { it.name == pluginName }
        if (plugin != null) {
            val results = mutableListOf<LinkData>()
            job?.cancelIfActive()
            job = parser.completeSearch(plugin, query, category, page)
                .onEach {
                    if (it is ParserResult.SingleResult) {
                        results.add(it.value)
                        parsingLiveData.value = ParserResult.Result(results)
                    } else {
                        parsingLiveData.value = it
                    }
                }
                .launchIn(viewModelScope)
            return parsingLiveData
        } else {
            parsingLiveData.value = ParserResult.MissingPlugin
            return parsingLiveData
        }
    }

    fun fetchPlugins() {
        viewModelScope.launch {
            val plugins = pluginRepository.getPlugins()
            pluginLiveData.postValue(plugins)
        }
    }

}