package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.KEY_USE_DOH
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

    val pluginLiveData = MutableLiveData<Pair<List<Plugin>, Int>>()
    val parsingLiveData = MutableLiveData<ParserResult>()

    fun completeSearch(
        query: String,
        pluginName: String,
        category: String? = null,
        page: Int = 1
    ): LiveData<ParserResult> {

        val plugin = pluginLiveData.value?.first?.firstOrNull { it.name == pluginName }
        if (plugin != null) {
            val results = mutableListOf<ScrapedItem>()
            // empty saved results on new searches
            saveSearchResults(results)
            job?.cancelIfActive()
            job = parser.completeSearch(plugin, query, category, page)
                .onEach {
                    when (it) {
                        is ParserResult.SingleResult -> {
                            results.add(it.value)
                            parsingLiveData.value = ParserResult.Results(results)
                            saveSearchResults(results)
                        }
                        is ParserResult.Results -> {
                            parsingLiveData.value = it
                            saveSearchResults(it.values)
                        }
                        else -> parsingLiveData.value = it
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
            val pluginsResult: Pair<List<Plugin>, Int> = pluginRepository.getPlugins(context)
            // todo: what
            pluginLiveData.postValue(pluginsResult)
            setPlugins(pluginsResult.first)
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

    fun isPluginDialogNeeded(): Boolean {
        return preferences.getBoolean(KEY_PLUGIN_DIALOG_NEEDED, true)
    }

    fun setPluginDialogNeeded(needed: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_PLUGIN_DIALOG_NEEDED, needed)
            apply()
        }
    }

    fun isDOHDialogNeeded(): Boolean {
        return preferences.getBoolean(KEY_DOH_DIALOG_NEEDED, true)
    }

    fun setDOHDialogNeeded(needed: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_DOH_DIALOG_NEEDED, needed)
            apply()
        }
    }

    fun enableDOH(enable: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_USE_DOH, enable)
            apply()
        }
    }

    fun setListSortPreference(tag: String) {
        with(preferences.edit()) {
            putString(FolderListViewModel.KEY_LIST_SORTING, tag)
            apply()
        }
    }

    fun getListSortPreference(): String {
        return preferences.getString(
            FolderListViewModel.KEY_LIST_SORTING,
            FolderListFragment.TAG_DEFAULT_SORT
        ) ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    companion object {
        const val KEY_RESULTS = "results_key"
        const val KEY_PLUGINS = "plugins_key"
        const val KEY_LAST_SELECTED_PLUGIN = "plugin_last_selected_key"
        const val KEY_PLUGIN_DIALOG_NEEDED = "plugin_dialog_needed_key"
        const val KEY_DOH_DIALOG_NEEDED = "doh_dialog_needed_key"
    }
}
