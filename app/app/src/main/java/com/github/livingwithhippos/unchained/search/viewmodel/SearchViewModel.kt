package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val pluginRepository: PluginRepository,
    private val databasePluginsRepository: DatabasePluginRepository,
    private val parser: Parser,
) : ViewModel() {

    // used to simulate a debounce effect while typing on the search bar
    private var job: Job? = null

    val pluginLiveData = MutableLiveData<Pair<List<Plugin>, Int>>()
    private val parsingLiveData = MutableLiveData<ParserResult>()

    fun completeSearch(
        query: String,
        pluginName: String,
        category: String? = null,
        page: Int = 1,
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
        preferences.edit { putString(KEY_LAST_SELECTED_PLUGIN, name) }
    }

    fun isPluginDialogNeeded(): Boolean {
        return preferences.getBoolean(KEY_PLUGIN_DIALOG_NEEDED, true)
    }

    fun setPluginDialogNeeded(needed: Boolean) {
        preferences.edit { putBoolean(KEY_PLUGIN_DIALOG_NEEDED, needed) }
    }

    fun isDOHDialogNeeded(): Boolean {
        return preferences.getBoolean(KEY_DOH_DIALOG_NEEDED, true)
    }

    fun setDOHDialogNeeded(needed: Boolean) {
        preferences.edit { putBoolean(KEY_DOH_DIALOG_NEEDED, needed) }
    }

    fun enableDOH(enable: Boolean) {
        preferences.edit { putBoolean(KEY_USE_DOH, enable) }
    }

    fun setListSortPreference(tag: String) {
        preferences.edit { putString(FolderListViewModel.KEY_LIST_SORTING, tag) }
    }

    fun getListSortPreference(): String {
        return preferences.getString(
            FolderListViewModel.KEY_LIST_SORTING,
            FolderListFragment.TAG_DEFAULT_SORT,
        ) ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    fun saveSearchCategory(category: String) {
        preferences.edit { putString(KEY_CATEGORY, category) }
    }

    fun getSearchCategory(): String {
        return preferences.getString(KEY_CATEGORY, "all") ?: "all"
    }

    fun pluginSearchWithSettings(
        query: String,
        category: String? = null,
        page: Int = 1,
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

                // todo: use something better to recognize them, hash repo + plugin name/url?
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
                delay(100)

                supervisorScope {
                    val searches =
                        enabledPlugins.map { plugin ->
                            launch {
                                Timber.d("Starting search for plugin ${plugin.name}")
                                parser.completeSearch(plugin, query, category, page).collect {
                                    when (it) {
                                        is ParserResult.SingleResult -> {
                                            parsingLiveData.postValue(
                                                ParserResult.SingleResult(it.value)
                                            )
                                        }
                                        is ParserResult.Results -> {
                                            // here I have all the results at once
                                            parsingLiveData.postValue(ParserResult.Results(it.values))
                                            Timber.w("Passingg ${it.values.size} results")
                                        }
                                        is ParserResult.SearchStarted -> {
                                            // not needed when run in parallel
                                        }
                                        is ParserResult.SearchFinished -> {
                                            // emitted once after all plugin searches complete
                                        }
                                        else -> {
                                            // forward plugin-specific errors while keeping aggregate flow alive
                                            parsingLiveData.postValue(it)
                                        }
                                    }
                                }
                            }
                        }
                    searches.joinAll()
                }

                parsingLiveData.postValue(ParserResult.SearchFinished)
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
        const val KEY_PLUGINS = "plugins_key"
        const val KEY_CATEGORY = "category_key"
        const val KEY_LAST_SELECTED_PLUGIN = "plugin_last_selected_key"
        const val KEY_PLUGIN_DIALOG_NEEDED = "plugin_dialog_needed_key"
        const val KEY_DOH_DIALOG_NEEDED = "doh_dialog_needed_key"
    }
}
