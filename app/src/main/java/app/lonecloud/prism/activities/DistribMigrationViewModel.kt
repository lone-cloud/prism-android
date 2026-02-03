package app.lonecloud.prism.activities

import android.app.Application
import android.content.Context
import app.lonecloud.prism.AppStore
import org.unifiedpush.android.distributor.ui.compose.DistribMigrationViewModel as UPDistribMigrationViewModel
import org.unifiedpush.android.distributor.ui.compose.state.DistribMigrationState
import org.unifiedpush.distributor.utils.listOtherDistributors

class DistribMigrationViewModel(state: DistribMigrationState, val application: Application? = null) : UPDistribMigrationViewModel(state) {
    constructor(application: Application) : this(
        stateFrom(application),
        application
    )

    override fun onFallbackDistribSelected(distributor: String?) {
        publishAction(
            AppAction(AppAction.Action.FallbackDistribSelected(distributor))
        )
    }

    override fun onMigrationDistributorSelected(distributor: String) {
        publishAction(
            AppAction(AppAction.Action.MigrateToDistrib(distributor))
        )
    }

    override fun onFallbackIntroShown() {
        publishAction(
            AppAction(AppAction.Action.FallbackIntroShown)
        )
    }

    override fun onServiceReactivated() {
        publishAction(
            AppAction(AppAction.Action.ReactivateUnifiedPush)
        )
    }

    override fun refreshDistributors() {
        application?.let { context ->
            refreshDistributors {
                val store = AppStore(context)
                val fallbackDistrib = store.fallbackService
                return@refreshDistributors context.listOtherDistributors()
                    .map { packageName ->
                        context.applicationRowState(packageName).copy(
                            selected = fallbackDistrib == packageName
                        )
                    }.toSet()
            }
        }
    }

    companion object {
        fun stateFrom(context: Context): DistribMigrationState {
            val store = AppStore(context)
            val fallbackDistrib = store.fallbackService
            val distributors = context.listOtherDistributors().map { packageName ->
                context.applicationRowState(packageName).copy(
                    selected = fallbackDistrib == packageName
                )
            }.toSet()
            return DistribMigrationState(
                distributors,
                store.fallbackIntroShown,
                migrated = store.migrated,
                featureEnabled = false
            )
        }
    }
}
