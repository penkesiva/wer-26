package com.golfcues.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SessionDetailViewModelFactory(
    private val application: android.app.Application,
    private val sessionId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionDetailViewModel::class.java)) {
            return SessionDetailViewModel(application, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
