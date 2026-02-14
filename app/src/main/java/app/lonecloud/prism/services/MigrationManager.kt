package app.lonecloud.prism.services

import android.content.Context
import app.lonecloud.prism.Distributor
import app.lonecloud.prism.PrismPreferences
import org.unifiedpush.android.distributor.MigrationManager as MManager

class MigrationManager : MManager() {
    override val distrib = Distributor
    override fun getStore(context: Context): MigrationStore = PrismPreferences(context)
}
