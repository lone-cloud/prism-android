package org.unifiedpush.distributor.sunup

import org.unifiedpush.android.distributor.ui.AppConfig
import org.unifiedpush.android.distributor.ui.InAppNotifsConfig
import org.unifiedpush.android.distributor.ui.LoginConfig
import org.unifiedpush.android.distributor.ui.MigrationConfig
import org.unifiedpush.android.distributor.ui.NoLoginConfig
import org.unifiedpush.android.distributor.ui.PrivacyPolicy

class SunupAppConfig : AppConfig {
    override val appName: Int
        get() = R.string.app_name
    override val restartableService = true
    override val privacyPolicy: PrivacyPolicy
        get() = object : PrivacyPolicy {
            override val description: Int
                get() = R.string.sunup_privacy_policy
            override val linkText: String
                get() = "https://www.mozilla.org/en-US/privacy/firefox/"
            override val linkTarget: String
                get() = "https://www.mozilla.org/en-US/privacy/firefox/#types-of-data-defined"
        }
    override val loginConfig: LoginConfig
        get() = NoLoginConfig(canChangeUrl = true)
    override val migrationConfig: MigrationConfig?
        get() = if (BuildConfig.SUPPORT_MIGRATIONS) {
            object : MigrationConfig {
                override val supportTempFallback = true
            }
        } else {
            null
        }
    override val inAppNotifsConfig: InAppNotifsConfig? = null
}
