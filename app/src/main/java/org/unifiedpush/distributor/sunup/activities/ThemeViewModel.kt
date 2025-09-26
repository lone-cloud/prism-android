package org.unifiedpush.distributor.sunup.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.unifiedpush.distributor.sunup.AppStore

class ThemeViewModel(val application: Application? = null) : ViewModel() {
    var dynamicColors by mutableStateOf(
        application?.let { AppStore(it).dynamicColors } ?: false
    )

    fun toggleDynamicColors() {
        viewModelScope.launch {
            dynamicColors = !dynamicColors
            application?.run {
                AppStore(this).dynamicColors = dynamicColors
            }
        }
    }
}
