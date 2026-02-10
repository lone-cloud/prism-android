package org.unifiedpush.distributor.sunup

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import org.unifiedpush.android.distributor.Database
import org.unifiedpush.distributor.sunup.services.MainRegistrationCounter

object DatabaseFactory {
    class MainDatabase(context: Context) : Database(context) {
        override val counter = MainRegistrationCounter
    }

    private val db: AtomicReference<Database?> = AtomicReference(null)

    fun getDb(context: Context): Database {
        return db.get() ?: run {
            val db = MainDatabase(context.applicationContext)
            this.db.set(db)
            return db
        }
    }
}
