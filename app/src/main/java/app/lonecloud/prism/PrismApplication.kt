package app.lonecloud.prism

import android.app.Application
import androidx.work.Configuration

class PrismApplication :
    Application(),
    Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
