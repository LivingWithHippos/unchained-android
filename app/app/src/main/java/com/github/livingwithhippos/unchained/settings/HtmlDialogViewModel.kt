package com.github.livingwithhippos.unchained.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HtmlDialogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun setTitleResource(titleRes: Int?) {
        titleRes?.let {
            savedStateHandle.set(KEY_TITLE, it)
        }
    }

    fun setMessageResource(messageRes: Int?) {
        messageRes?.let {
            savedStateHandle.set(KEY_MESSAGE, it)
        }
    }

    fun getTitleResource(): Int {
        return savedStateHandle.get<Int>(KEY_TITLE) ?: -1
    }

    fun getMessageResource(): Int {
        return savedStateHandle.get<Int>(KEY_MESSAGE) ?: -1
    }

    companion object {
        const val KEY_TITLE = "title_key"
        const val KEY_MESSAGE = "message_key"
    }

}

