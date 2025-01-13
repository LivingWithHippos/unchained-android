package com.github.livingwithhippos.unchained.settings.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel


class HtmlDialogViewModel(private val savedStateHandle: SavedStateHandle) :
    ViewModel() {

    fun setTitleResource(titleRes: Int?) {
        titleRes?.let { savedStateHandle[KEY_TITLE] = it }
    }

    fun setMessageResource(messageRes: Int?) {
        messageRes?.let { savedStateHandle[KEY_MESSAGE] = it }
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
