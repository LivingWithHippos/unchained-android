package com.github.livingwithhippos.unchained.folderlist.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.UnrestrictRepository
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val unrestrictRepository: UnrestrictRepository,
    private val protoStore: ProtoStore,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val folderLiveData = MutableLiveData<Event<List<DownloadItem>>>()
    val deletedDownloadLiveData = MutableLiveData<Event<DownloadItem>>()
    val errorsLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val progressLiveData = MutableLiveData<Int>()

    // used to simulate a debounce effect while typing on the search bar
    private var queryJob: Job? = null

    // stores the last query value
    val queryLiveData = MutableLiveData<String>()

    fun shouldShowFilters(): Boolean {
        return preferences.getBoolean(KEY_SHOW_FOLDER_FILTERS, false)
    }

    fun retrieveFolderFileList(folderLink: String) {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken

            val filesList: EitherResult<UnchainedNetworkException, List<String>> =
                unrestrictRepository.getEitherFolderLinks(token, folderLink)

            when (filesList) {
                is EitherResult.Failure -> errorsLiveData.postEvent(filesList.failure)
                is EitherResult.Success -> retrieveFiles(filesList.success)
            }
        }
    }

    fun retrieveFiles(links: List<String>) {
        viewModelScope.launch {

            val token = protoStore.getCredentials().accessToken
            // either first time or there were some errors, re-download
            if (links.size != getRetrievedLinks()) {

                val hitList = mutableListOf<DownloadItem>()

                links.forEachIndexed { index, link ->
                    when (
                        val file =
                            unrestrictRepository.getEitherUnrestrictedLink(token, link)
                    ) {
                        is EitherResult.Failure -> {
                            errorsLiveData.postEvent(file.failure)
                            progressLiveData.postValue((index + 1) * 100 / links.size)
                        }
                        is EitherResult.Success -> {
                            hitList.add(file.success)
                            folderLiveData.postEvent(hitList)
                            setRetrievedLinks(hitList.size)
                            progressLiveData.postValue((index + 1) * 100 / links.size)
                        }
                    }
                }
            } else {
                // I already downloaded all the files, repost the last value
                folderLiveData.value?.let {
                    folderLiveData.postEvent(it.peekContent())
                }
            }
        }
    }

    private fun setRetrievedLinks(links: Int) {
        savedStateHandle.set(KEY_RETRIEVED_LINKS, links)
    }

    private fun getRetrievedLinks(): Int {
        return savedStateHandle.get(KEY_RETRIEVED_LINKS) ?: -1
    }

    fun deleteDownloadList(downloads: List<DownloadItem>) {
        viewModelScope.launch {
            val token = protoStore.getCredentials().accessToken
            downloads.forEach {
                val deleted = downloadRepository.deleteDownload(token, it.id)
                if (deleted != null)
                    deletedDownloadLiveData.postEvent(it)
            }
        }
    }

    fun filterList(query: String?) {
        // simulate debounce
        queryJob?.cancel()

        queryJob = viewModelScope.launch {
            delay(500)
            if (isActive)
                queryLiveData.postValue(query?.trim() ?: "")
        }
    }

    fun getMinFileSize(): Long {
        val minMBString = preferences.getString("filter_size_mb", "10")
        val minMB: Long = minMBString?.toLongOrNull() ?: 10
        return minMB * 1024 * 1024
    }

    fun setFilterSizePreference(enabled: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_LIST_FILTER_SIZE, enabled)
            apply()
        }
    }

    fun getFilterSizePreference(): Boolean {
        return preferences.getBoolean(KEY_LIST_FILTER_SIZE, false)
    }

    fun setFilterTypePreference(enabled: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_LIST_FILTER_TYPE, enabled)
            apply()
        }
    }

    fun getFilterTypePreference(): Boolean {
        return preferences.getBoolean(KEY_LIST_FILTER_TYPE, false)
    }

    fun setListSortPreference(tag: String) {
        with(preferences.edit()) {
            putString(KEY_LIST_SORTING, tag)
            apply()
        }
    }

    fun getListSortPreference(): String {
        return preferences.getString(KEY_LIST_SORTING, FolderListFragment.TAG_DEFAULT_SORT)
            ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    fun setScrollingAllowed(allow: Boolean) {
        with(preferences.edit()) {
            putBoolean(KEY_ALLOW_SCROLLING, allow)
            apply()
        }
    }

    fun getScrollingAllowed(): Boolean {
        return preferences.getBoolean(KEY_ALLOW_SCROLLING, true)
    }

    companion object {
        const val KEY_ALLOW_SCROLLING = "allow_scrolling"
        const val KEY_RETRIEVED_LINKS = "retrieve_links"
        const val KEY_LIST_FILTER_SIZE = "filter_list_size"
        const val KEY_LIST_FILTER_TYPE = "filter_list_type"
        const val KEY_LIST_SORTING = "sort_list_type"
        const val KEY_SHOW_FOLDER_FILTERS = "show_folders_filters"
    }
}
