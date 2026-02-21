package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.usecase.cases.GetAllCasesUseCase
import com.vakildiary.app.domain.usecase.cases.SearchCasesUseCase
import com.vakildiary.app.presentation.viewmodels.state.CaseListUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CaseListViewModel @Inject constructor(
    private val getAllCasesUseCase: GetAllCasesUseCase,
    private val searchCasesUseCase: SearchCasesUseCase
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<CaseListUiState> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                getAllCasesUseCase()
            } else {
                searchCasesUseCase(query)
            }
        }
        .map { result ->
            when (result) {
                is Result.Success -> CaseListUiState.Success(result.data)
                is Result.Error -> CaseListUiState.Error(result.message)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaseListUiState.Loading)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
