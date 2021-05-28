package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repositoy.PluginRepository
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.ScrapedItem
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
    private val preferences: SharedPreferences,
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
            val results = mutableListOf<ScrapedItem>()
            // empty saved results on new searches
            saveSearchResults(results)
            job?.cancelIfActive()
            job = parser.completeSearch(plugin, query, category, page)
                .onEach {
                    if (it is ParserResult.SingleResult) {
                        results.add(it.value)
                        parsingLiveData.value = ParserResult.Results(results)
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

    fun fetchPlugins(context: Context) {
        viewModelScope.launch {
            val plugins = pluginRepository.getPlugins(context)
            pluginLiveData.postValue(plugins)
            setPlugins(plugins)
        }
    }

    fun getSearchResults(): List<ScrapedItem> {
        return savedStateHandle.get<List<ScrapedItem>>(KEY_RESULTS) ?: emptyList()
    }

    private fun saveSearchResults(results: List<ScrapedItem>) {
        savedStateHandle.set(KEY_RESULTS, results)
    }

    fun getPlugins(): List<Plugin> {
        return savedStateHandle.get<List<Plugin>>(KEY_PLUGINS) ?: emptyList()
    }

    private fun setPlugins(plugins: List<Plugin>) {
        savedStateHandle.set(KEY_PLUGINS, plugins)
    }

    fun stopSearch() {
        job?.cancelIfActive()
    }

    fun getLastSelectedPlugin(): String {
        return preferences.getString(KEY_LAST_SELECTED_PLUGIN, "") ?: ""
    }

    fun setLastSelectedPlugin(name: String) {
        with(preferences.edit()) {
            putString(KEY_LAST_SELECTED_PLUGIN, name)
            apply()
        }
    }

    fun isDialogNeeded(): Boolean {
        return preferences.getBoolean(KEY_PLUGIN_DIALOG_NEEDED, true)
    }

    fun setDialogNeeded(needed: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_PLUGIN_DIALOG_NEEDED, needed)
            apply()
        }
    }

    companion object {
        const val KEY_RESULTS = "results_key"
        const val KEY_PLUGINS = "plugins_key"
        const val KEY_LAST_SELECTED_PLUGIN = "plugin_last_selected_key"
        const val KEY_PLUGIN_DIALOG_NEEDED = "plugin_dialog_needed_key"
    }
}