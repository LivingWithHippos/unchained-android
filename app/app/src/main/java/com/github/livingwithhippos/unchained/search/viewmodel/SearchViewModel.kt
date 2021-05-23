package com.github.livingwithhippos.unchained.search.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import com.github.livingwithhippos.unchained.newdownload.viewmodel.Link
import com.github.livingwithhippos.unchained.plugins.LinkData
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
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
                        saveSearchResults(results)
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

    fun getSearchResults(): List<LinkData> {
        return savedStateHandle.get<List<LinkData>>(KEY_RESULTS) ?: emptyList()
    }

    fun saveSearchResults(results: List<LinkData>) {
        savedStateHandle.set(KEY_RESULTS, results)
    }

    fun stopSearch() {
        job?.cancelIfActive()
    }

    companion object {
        const val KEY_RESULTS = "results_key"
    }
}