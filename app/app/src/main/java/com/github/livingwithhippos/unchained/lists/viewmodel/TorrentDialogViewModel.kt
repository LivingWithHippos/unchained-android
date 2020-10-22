package com.github.livingwithhippos.unchained.lists.viewmodel

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.data.model.TorrentItem

class TorrentDialogViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    fun setItem(item: TorrentItem?) {
        item.let {
            savedStateHandle.set(KEY_ITEM, it)
        }
    }

    fun getItem(): TorrentItem? {
        return savedStateHandle.get(KEY_ITEM)
    }

    companion object {
        private const val KEY_ITEM = "item_key"
    }
}