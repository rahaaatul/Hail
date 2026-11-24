package com.aistra.hail.work

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailSettings
import java.util.concurrent.TimeUnit

object HWork {
    fun cancelWork(name: String) {
        if (WorkManager.isInitialized()) {
            WorkManager.getInstance(app).cancelUniqueWork(name)
        }
    }

    fun setDeferredFrozen(
        packageName: String,
        frozen: Boolean = true,
        minutes: Long,
    ) {
        WorkManager.getInstance(app).enqueueUniqueWork(
            packageName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<FrozenWorker>()
                .setInputData(
                    workDataOf(
                        HailData.KEY_PACKAGE to packageName,
                        HailData.KEY_FROZEN to frozen,
                    ),
                ).setInitialDelay(minutes, TimeUnit.MINUTES)
                .build(),
        )
    }

    fun setAutoFreeze(screenOff: Boolean) {
        WorkManager.getInstance(app).enqueueUniqueWork(
            HailApi.ACTION_FREEZE_ALL,
            ExistingWorkPolicy.REPLACE, // in case the old task has not been executed...
            OneTimeWorkRequestBuilder<AutoFreezeWorker>().run {
                // A positive delay means "wait for the user to leave the device alone": only then
                // is the worker postponed. A zero delay (or a screen-on trigger) runs immediately.
                if (screenOff && HailSettings.autoFreezeDelay > 0f) {
                    setInitialDelay(HailSettings.autoFreezeDelay.toLong(), TimeUnit.MINUTES)
                }
                setInputData(workDataOf(HailData.ACTION_LOCK to screenOff))
                build()
            },
        )
    }
}
