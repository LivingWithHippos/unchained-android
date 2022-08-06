package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.RdCache
import com.github.livingwithhippos.unchained.data.repository.PluginRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.folderlist.viewmodel.FolderListViewModel
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.plugins.model.ScrapedItem
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.KEY_USE_DOH
import com.github.livingwithhippos.unchained.utilities.BASE_URL
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.INSTANT_AVAILABILITY_ENDPOINT
import com.github.livingwithhippos.unchained.utilities.MAGNET_PATTERN
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
    private val torrentsRepository: TorrentsRepository,
    private val parser: Parser,
    private val protoStore: ProtoStore
) : ViewModel() {

    // used to simulate a debounce effect while typing on the search bar
    private var job: Job? = null

    val pluginLiveData = MutableLiveData<Pair<List<Plugin>, Int>>()
    private val parsingLiveData = MutableLiveData<ParserResult>()

    private val _cacheLiveData = MutableLiveData<Set<String>>()
    val cacheLiveData : LiveData<Set<String>> = _cacheLiveData

    private val magnetPattern = Regex(MAGNET_PATTERN, RegexOption.IGNORE_CASE)

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
            job = parser.completeSearch(plugin, query, category, page)
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
                            clearCacheResults()
                            clearSearchResults()
                            results.clear()
                            parsingLiveData.value = it
                        }
                        is ParserResult.SearchFinished -> {
                            checkRDCache(results)
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

    private fun checkRDCache(scrapedItems: List<ScrapedItem>) {
        if (scrapedItems.isNotEmpty()) {
            viewModelScope.launch {
                val currentCache = mutableSetOf<String>()
                val token = protoStore.getCredentials().accessToken
                val builder = StringBuilder(BASE_URL)
                builder.append(INSTANT_AVAILABILITY_ENDPOINT)
                scrapedItems.forEach { item ->
                    item.magnets.forEach { magnet ->
                        val btih = magnetPattern.find(magnet)?.groupValues?.get(1)
                        if (!btih.isNullOrBlank()) {
                            builder.append("/")
                            builder.append(btih)
                        }
                    }
                }
                if (builder.length > (BASE_URL.length + INSTANT_AVAILABILITY_ENDPOINT.length)) {
                    val cache = torrentsRepository.getInstantAvailability(token, builder.toString())
                    if (cache is EitherResult.Success) {
                        if (cache.success.isNotEmpty()) {
                            val cachedIDs: Set<String> = cache.success.keys.map { it.uppercase() }.toSet()
                            currentCache.addAll(cachedIDs)
                        }
                    }
                }
                _cacheLiveData.postValue(currentCache)
                setCacheResults(currentCache)
            }
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

    private fun setSearchResults(results: List<ScrapedItem>) {
        savedStateHandle[KEY_RESULTS] = results
    }

    private fun appendSearchResults(results: List<ScrapedItem>) {
        val newCache = mutableListOf<ScrapedItem>()
        newCache.addAll(getSearchResults())
        newCache.addAll(results)
        savedStateHandle[KEY_RESULTS] = newCache
    }

    private fun clearSearchResults() {
        savedStateHandle[KEY_RESULTS] = emptyList<ScrapedItem>()
    }

    private fun setCacheResults(cache: Set<String>) {
        savedStateHandle[KEY_CACHE] = cache
    }

    private fun appendCacheResults(cache: Set<String>) {
        val newCache = mutableSetOf<String>()
        newCache.addAll(getCacheResults())
        newCache.addAll(cache)
        // as Set<String> probably not necessary
        savedStateHandle[KEY_CACHE] = newCache as Set<String>
    }

    fun getCacheResults(): Set<String> {
        try {
            return savedStateHandle.get<Set<String>>(KEY_CACHE) ?: emptySet()
        } catch (e: java.lang.ClassCastException) {
            return emptySet()
        }
    }

    private fun clearCacheResults() {
        savedStateHandle[KEY_CACHE] = emptySet<String>()
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
            FolderListViewModel.KEY_LIST_SORTING,
            FolderListFragment.TAG_DEFAULT_SORT
        ) ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    companion object {
        // todo: these needs to be moved to a single object because if I reuse the same keys for two objects I'll get the wrong result
        const val KEY_RESULTS = "search_results_key"
        const val KEY_CACHE = "search_cache_key"
        const val KEY_PLUGINS = "plugins_key"
        const val KEY_LAST_SELECTED_PLUGIN = "plugin_last_selected_key"
        const val KEY_PLUGIN_DIALOG_NEEDED = "plugin_dialog_needed_key"
        const val KEY_DOH_DIALOG_NEEDED = "doh_dialog_needed_key"
    }
}
