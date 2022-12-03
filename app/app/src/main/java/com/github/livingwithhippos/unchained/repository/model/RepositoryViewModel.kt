package com.github.livingwithhippos.unchained.repository.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RepositoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
}