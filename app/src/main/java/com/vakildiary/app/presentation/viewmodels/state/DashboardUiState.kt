package com.vakildiary.app.presentation.viewmodels.state

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val hearingsToday: List<String>,
        val tasksToday: List<String>,
        val overdueCount: Int,
        val totalCases: Int,
        val pendingFees: String,
        val upcomingIn7Days: Int
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
