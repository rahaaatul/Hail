package com.aistra.hail.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ActionExecutor {
    private val mutex = Mutex()

    suspend fun prepare(action: LaunchAction): Result<Intent> = withContext(Dispatchers.IO) {
        mutex.withLock {
            action.unfreezePackages.forEach { packageName ->
                if (HPackages.getApplicationInfoOrNull(packageName) == null) {
                    return@withContext Result.failure(
                        IllegalStateException(app.getString(R.string.action_app_unavailable, packageName))
                    )
                }
                if (AppManager.isAppFrozen(packageName) &&
                    !AppManager.setAppFrozen(packageName, false)
                ) {
                    return@withContext Result.failure(
                        IllegalStateException(app.getString(R.string.action_unfreeze_failed, packageName))
                    )
                }
                if (AppManager.isAppFrozen(packageName)) {
                    return@withContext Result.failure(
                        IllegalStateException(app.getString(R.string.action_unfreeze_failed, packageName))
                    )
                }
            }
            val launchIntent = app.packageManager.getLaunchIntentForPackage(action.launchPackage)
                ?: return@withContext Result.failure(
                    ActivityNotFoundException(app.getString(R.string.activity_not_found))
                )
            app.setAutoFreezeService()
            Result.success(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}