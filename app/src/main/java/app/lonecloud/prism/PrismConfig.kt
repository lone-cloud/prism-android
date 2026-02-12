package app.lonecloud.prism

import org.unifiedpush.android.distributor.ui.AppConfig
import org.unifiedpush.android.distributor.ui.InAppNotifsConfig
import org.unifiedpush.android.distributor.ui.MigrationConfig
import org.unifiedpush.android.distributor.ui.NoLoginConfig
import org.unifiedpush.android.distributor.ui.PrivacyPolicy

object PrismConfig : AppConfig {
    override val appName = R.string.app_name
    override val restartableService = true
    override val privacyPolicy: PrivacyPolicy? = null
    override val loginConfig = NoLoginConfig(canChangeUrl = true)
    override val migrationConfig = object : MigrationConfig {
        override val supportTempFallback = true
    }
    override val inAppNotifsConfig: InAppNotifsConfig? = null
}
