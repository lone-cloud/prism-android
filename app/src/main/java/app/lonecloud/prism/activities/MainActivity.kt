package app.lonecloud.prism.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lonecloud.prism.activities.ui.App
import app.lonecloud.prism.activities.ui.theme.AppTheme
import org.unifiedpush.android.distributor.ipc.InternalMessenger

class MainActivity : ComponentActivity() {
    private lateinit var messenger: InternalMessenger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messenger = InternalMessenger(this)

        enableEdgeToEdge()

        setContent {
            val factory = ViewModelFactory(this.application, messenger)
            val themeViewModel = viewModel<ThemeViewModel>(factory = factory)
            AppTheme(
                dynamicColor = themeViewModel.dynamicColors
            ) {
                App(factory, themeViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
