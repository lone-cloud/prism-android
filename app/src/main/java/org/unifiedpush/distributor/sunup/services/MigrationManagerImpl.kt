package org.unifiedpush.distributor.sunup.services

import android.content.Context
import org.unifiedpush.distributor.MigrationManager
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.Distributor

class MigrationManagerImpl : MigrationManager() {
    override val distrib = Distributor
    override fun getStore(context: Context): MigrationStore = AppStore(context)
}
