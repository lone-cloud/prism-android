package app.lonecloud.prism

import android.content.Context
import app.lonecloud.prism.services.MainRegistrationCounter
import java.util.concurrent.atomic.AtomicReference
import org.unifiedpush.distributor.Database as Database

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
