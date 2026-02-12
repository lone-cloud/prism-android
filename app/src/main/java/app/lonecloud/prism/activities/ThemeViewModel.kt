package app.lonecloud.prism.activities

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lonecloud.prism.AppStore
import kotlinx.coroutines.launch
import org.unifiedpush.android.distributor.ipc.InternalMessenger
import org.unifiedpush.android.distributor.ipc.InternalOpcode

class ThemeViewModel(val messenger: InternalMessenger?, val application: Application?) : ViewModel() {
    var dynamicColors by mutableStateOf(application?.let { AppStore(it).dynamicColors } ?: false)

    fun toggleDynamicColors() {
        viewModelScope.launch {
            dynamicColors = !dynamicColors
            application?.let { AppStore(it).dynamicColors = dynamicColors }
            messenger?.sendIMessage(InternalOpcode.THEME_DYN_SET, if (dynamicColors) 1 else 0)
        }
    }
}
