package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.backup.DeltaSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class DeltaSyncViewModel @Inject constructor(
    private val deltaSyncManager: DeltaSyncManager
) : ViewModel() {
    private val _syncCount = MutableStateFlow(0)
    val syncCount: StateFlow<Int> = _syncCount.asStateFlow()

    fun syncDocuments() {
        viewModelScope.launch {
            when (val result = deltaSyncManager.syncDocuments()) {
                is Result.Success -> _syncCount.value = result.data
                is Result.Error -> _syncCount.value = 0
            }
        }
    }
}
