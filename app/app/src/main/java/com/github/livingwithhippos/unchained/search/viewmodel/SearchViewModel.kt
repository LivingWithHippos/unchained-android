package com.github.livingwithhippos.unchained.search.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.plugins.Parser
import com.github.livingwithhippos.unchained.plugins.ParserResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val preferences: SharedPreferences,
    private val parser: Parser
) : ViewModel() {
    val resultLiveData = MutableLiveData<Event<ParserResult>>()

    fun search(query: String, plugin: String) {
        viewModelScope.launch {
            // resultLiveData.postEvent(response)
        }
    }
}