package org.unifiedpush.distributor.sunup.services

import android.content.Context
import org.unifiedpush.distributor.MigrationManager as MManager
import org.unifiedpush.distributor.sunup.AppStore
import org.unifiedpush.distributor.sunup.Distributor

class MigrationManager : MManager() {
    override val distrib = Distributor
    override fun getStore(context: Context): MigrationStore = AppStore(context)
}
