package com.example.lcb.app.news.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class MeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var offlineReadingEnabled: Boolean
        get() = savedStateHandle[KEY_OFFLINE_READING] ?: true
        set(value) {
            savedStateHandle[KEY_OFFLINE_READING] = value
        }

    private companion object {
        private const val KEY_OFFLINE_READING = "me_offline_reading"
    }
}
