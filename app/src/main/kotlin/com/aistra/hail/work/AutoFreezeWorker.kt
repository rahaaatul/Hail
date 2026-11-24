package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailSettings
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HSystem

class AutoFreezeWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        val actionLock = inputData.getBoolean(HailData.ACTION_LOCK, true)
        if (
            AutoFreezeDecision.shouldSkip(
                actionLock = actionLock,
                autoFreezeAfterLock = HailSettings.autoFreezeAfterLock,
                delayMinutes = HailSettings.autoFreezeDelay,
                isInteractive = HSystem.isInteractive(applicationContext),
                skipWhileCharging = isSkipWhileCharging(applicationContext),
            )
        ) {
            return Result.success() // Not stopping the AutoFreezeService here.
        }
        // The worker will run at some point. Then we'll stop the Service
        val checkedList = HailData.checkedList.filter { !isSkipApp(applicationContext, it) }
        repeat(3) {
            val result = AppManager.setListFrozen(true, checkedList.map { it.packageName })
            if (result.succeeded > 0) {
                app.setAutoFreezeService()
                return Result.success()
            }
        }
        // Nothing was frozen: let the service re-evaluate and stop itself when no eligible apps remain.
        app.setAutoFreezeService()
        return Result.success() // 3 tries failed -> stop WorkManager retries
    }

    private fun isSkipWhileCharging(context: Context): Boolean = HailSettings.skipWhileCharging && HSystem.isCharging(context)

    private fun isSkipApp(
        context: Context,
        appInfo: AppInfo,
    ): Boolean =
        AppManager.isAppFrozen(appInfo.packageName) || (
            HailSettings.skipForegroundApp &&
                HSystem.isForegroundApp(
                    context,
                    appInfo.packageName,
                )
        ) || (
            HailSettings.skipNotifyingApp &&
                AutoFreezeService.instance?.activeNotifications?.any { it.packageName == appInfo.packageName } == true
        ) || appInfo.whitelisted
}

/**
 * Pure skip/freeze policy for the auto-freeze lifecycle. Kept free of Android and [HailData]
 * dependencies so the timing rules can be unit-tested without a device or WorkManager.
 */
internal object AutoFreezeDecision {
    /**
     * Returns `true` when the worker must not freeze apps this run.
     *
     * - A lock-triggered run is ignored when the user disabled auto-freeze after lock.
     * - A lock-triggered run with a positive delay is ignored while the device is interactive:
     *   the delay is a "leave me alone" window, so an active screen cancels this run.
     * - Any run is ignored while charging when the user asked to skip charging.
     */
    fun shouldSkip(
        actionLock: Boolean,
        autoFreezeAfterLock: Boolean,
        delayMinutes: Float,
        isInteractive: Boolean,
        skipWhileCharging: Boolean,
    ): Boolean {
        val lockSkipped = actionLock && (!autoFreezeAfterLock || (delayMinutes > 0f && isInteractive))
        return lockSkipped || skipWhileCharging
    }
}
