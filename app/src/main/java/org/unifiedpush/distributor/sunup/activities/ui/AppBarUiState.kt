package org.unifiedpush.distributor.sunup.activities.ui

data class AppBarUiState(
    val currentApiUrl: String,
    val menuExpanded: Boolean = false,
    val showChangeServerDialog: Boolean = false
)
