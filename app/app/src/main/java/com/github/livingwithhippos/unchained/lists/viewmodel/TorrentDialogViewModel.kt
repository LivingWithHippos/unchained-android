package com.github.livingwithhippos.unchained.lists.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TorrentDialogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
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