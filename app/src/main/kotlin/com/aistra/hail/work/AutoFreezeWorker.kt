package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.HSystem

class AutoFreezeWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private const val MAX_TRIES = 3
    override fun doWork(): Result {
        val result = if (
            (inputData.getBoolean(HailData.ACTION_LOCK, true) && HSystem.isInteractive(applicationContext))
                || isSkipWhileCharging(applicationContext)
        ) {
            // Not stopping the AutoFreezeService here. The worker will run at
            // some point. Then we'll stop the Service
            Result.success()
        } else {
            val checkedList = HailData.checkedList.filter { !isSkipApp(applicationContext, it) }
            repeat(MAX_TRIES) {
                val tryResult = AppManager.setListFrozen(true, checkedList)
                if (tryResult != null) {
                    app.setAutoFreezeService()
                    break
                }
            }
            Result.success()
        }
        return result
    }

    private fun isSkipWhileCharging(context: Context): Boolean =
        HailData.skipWhileCharging && HSystem.isCharging(context)

    private fun isSkipApp(context: Context, appInfo: AppInfo): Boolean =
        AppManager.isAppFrozen(appInfo.packageName) || (
            HailData.skipForegroundApp && HSystem.isForegroundApp(
                context,
                appInfo.packageName,
            )
            ) ||
            (
                HailData.skipNotifyingApp &&
                    AutoFreezeService.instance?.activeNotifications?.keys?.any { it == appInfo.packageName } == true
                ) ||
            appInfo.whitelisted
}
