package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.cache.InstantAvailability
import com.github.livingwithhippos.unchained.data.repository.DatabasePluginRepository
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
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val pluginRepository: PluginRepository,
    private val databasePluginsRepository: DatabasePluginRepository,
    private val parser: Parser
) : ViewModel() {

    // used to simulate a debounce effect while typing on the search bar
    private var job: Job? = null

    val pluginLiveData = MutableLiveData<Pair<List<Plugin>, Int>>()
    private val parsingLiveData = MutableLiveData<ParserResult>()

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
            job?.cancelIfActive()
            job =
                parser
                    .completeSearch(plugin, query, category, page)
                    .onEach {
                        when (it) {
                            is ParserResult.SingleResult -> {
                                results.add(it.value)
                                parsingLiveData.value = ParserResult.Results(results)
                                setSearchResults(results)
                            }
                            is ParserResult.Results -> {
                                // here I have all the results at once
                                parsingLiveData.value = it
                                results.addAll(it.values)
                                setSearchResults(results)
                            }
                            is ParserResult.SearchStarted -> {
                                setCacheResults(null)
                                clearSearchResults()
                                results.clear()
                                parsingLiveData.value = it
                            }
                            is ParserResult.SearchFinished -> {
                                parsingLiveData.value = it
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
            pluginLiveData.postValue(pluginsResult)
            setPlugins(pluginsResult.first)
        }
    }

    fun getSearchResults(): List<ScrapedItem> {
        return savedStateHandle.get<List<ScrapedItem>>(KEY_RESULTS) ?: emptyList()
    }

    private fun setSearchResults(results: List<ScrapedItem>) {
        savedStateHandle[KEY_RESULTS] = results
    }

    private fun clearSearchResults() {
        savedStateHandle[KEY_RESULTS] = emptyList<ScrapedItem>()
    }

    private fun setCacheResults(cache: InstantAvailability?) {
        savedStateHandle[KEY_CACHE] = cache
    }

    fun getCacheResults(): InstantAvailability? {
        return try {
            savedStateHandle.get<InstantAvailability>(KEY_CACHE)
        } catch (e: java.lang.ClassCastException) {
            null
        }
    }

    fun getPlugins(): List<Plugin> {
        return savedStateHandle.get<List<Plugin>>(KEY_PLUGINS) ?: emptyList()
    }

    private fun setPlugins(plugins: List<Plugin>) {
        savedStateHandle[KEY_PLUGINS] = plugins
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
            FolderListViewModel.KEY_LIST_SORTING, FolderListFragment.TAG_DEFAULT_SORT)
            ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    fun saveSearchCategory(category: String) {
        with(preferences.edit()) {
            putString(KEY_CATEGORY, category)
            apply()
        }
    }

    fun getSearchCategory(): String {
        return preferences.getString(KEY_CATEGORY, "all") ?: "all"
    }

    fun pluginSearchWithSettings(
        query: String,
        category: String? = null,
        page: Int = 1
    ): LiveData<ParserResult> {
        // get category if selected
        // get all selected plugins
        // retrieve plugins from disk if needed
        // search for each one and publish
        job?.cancelIfActive()

        job =
            viewModelScope.launch {
                // get category if selected
                // get all selected plugins
                // retrieve plugins from disk if needed
                // search for each one and publish

                // todo: repo + plugin name?
                val enabledPlugins: List<Plugin> =
                    databasePluginsRepository.getEnabledPlugins().values.flatten().mapNotNull {
                        repoPlugin ->
                        pluginLiveData.value?.first?.firstOrNull { it.name == repoPlugin.name }
                    }
                if (enabledPlugins.isEmpty()) {
                    parsingLiveData.postValue(ParserResult.NoEnabledPlugins)
                    return@launch
                }

                parsingLiveData.postValue(ParserResult.SearchStarted(-1))

                // todo: launch search, parallel if possible and post results on live data

                val results = mutableListOf<ScrapedItem>()

                setCacheResults(null)
                clearSearchResults()

                enabledPlugins.forEach { plugin ->
                    Timber.d("Starting search for plugin ${plugin.name}")
                    parser
                        .completeSearch(plugin, query, category, page)
                        .onEach {
                            when (it) {
                                is ParserResult.SingleResult -> {
                                    results.add(it.value)
                                    parsingLiveData.value = ParserResult.Results(results)
                                    setSearchResults(results)
                                }
                                is ParserResult.Results -> {
                                    // here I have all the results at once
                                    parsingLiveData.value = it
                                    results.addAll(it.values)
                                    setSearchResults(results)
                                }
                                is ParserResult.SearchStarted -> {
                                    // results.clear()
                                    parsingLiveData.value = it
                                }
                                is ParserResult.SearchFinished -> {
                                    parsingLiveData.value = it
                                }
                                else -> {
                                    // todo: check if we are stopping on the fragment side, with
                                    // parallel search it's ok to continue if a single one fails
                                    parsingLiveData.value = it
                                }
                            }
                        }
                        .launchIn(viewModelScope)
                }
            }

        return parsingLiveData
    }

    fun setPluginEnabled(name: String, checked: Boolean) {
        viewModelScope.launch { databasePluginsRepository.enablePlugin(name, checked) }
    }

    companion object {
        // todo: these needs to be moved to a single object because if I reuse the same keys for two
        // objects I'll get the wrong result
        const val KEY_RESULTS = "search_results_key"
        const val KEY_CACHE = "search_cache_key"
        const val KEY_PLUGINS = "plugins_key"
        const val KEY_CATEGORY = "category_key"
        const val KEY_LAST_SELECTED_PLUGIN = "plugin_last_selected_key"
        const val KEY_PLUGIN_DIALOG_NEEDED = "plugin_dialog_needed_key"
        const val KEY_DOH_DIALOG_NEEDED = "doh_dialog_needed_key"
    }
}
