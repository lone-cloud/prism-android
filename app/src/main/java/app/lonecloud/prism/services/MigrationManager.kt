package app.lonecloud.prism.services

import android.content.Context
import app.lonecloud.prism.AppStore
import app.lonecloud.prism.Distributor
import org.unifiedpush.distributor.MigrationManager as MManager

class MigrationManager : MManager() {
    override val distrib = Distributor
    override fun getStore(context: Context): MigrationStore = AppStore(context)
}
