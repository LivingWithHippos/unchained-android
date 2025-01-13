package com.github.livingwithhippos.unchained.lists.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.data.model.DownloadItem

class DownloadDialogViewModel(private val savedStateHandle: SavedStateHandle) :
    ViewModel() {

    fun setItem(item: DownloadItem?) {
        item.let { savedStateHandle[KEY_ITEM] = it }
    }

    fun getItem(): DownloadItem? {
        return savedStateHandle[KEY_ITEM]
    }

    companion object {
        private const val KEY_ITEM = "item_key"
    }
}
