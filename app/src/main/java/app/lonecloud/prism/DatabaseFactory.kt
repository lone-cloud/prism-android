/*
 * Copyright (C) 2024 p1gp1g
 * Modified by lone-cloud under AGPL v3.0
 *
 * Original work: https://codeberg.org/Sunup/android
 * Licensed under Apache License 2.0
 */

package app.lonecloud.prism

import android.content.Context
import app.lonecloud.prism.services.MainRegistrationCounter
import java.util.concurrent.atomic.AtomicReference
import org.unifiedpush.android.distributor.Database as Database

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
