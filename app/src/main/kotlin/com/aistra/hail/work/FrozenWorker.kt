package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HLog

class FrozenWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val packageName = inputData.getString(HailData.KEY_PACKAGE)
        if (packageName == null) {
            HLog.e("Missing package name in FrozenWorker input")
            return Result.failure()
        }
        val shouldFreeze = inputData.getBoolean(HailData.KEY_FROZEN, true)
        val workingMode = inputData.getString(HailData.WORKING_MODE) ?: HailData.workingMode
        return if (AppManager.setAppFrozen(packageName, shouldFreeze)) {
            Result.success()
        } else {
            HLog.e("Failed to ${if (shouldFreeze) "freeze" else "unfreeze"} $packageName")
            // setAppFrozen returns a plain Boolean without distinguishing permanent
            // from transient failures. Classify the failure:
            // - No backend service configured (MODE_DEFAULT): setAppFrozen always
            //   returns false in this mode, so retrying is futile.
            // - Package not installed: won't resolve on retry.
            // Otherwise: the failure could be transient (e.g. Shizuku/Island
            // binder temporarily unavailable), so retry. WorkManager's exponential
            // backoff will eventually stop retrying.
            val isPackageInstalled = HPackages.getApplicationInfoOrNull(packageName) != null
            if (!isPackageInstalled || workingMode == HailData.MODE_DEFAULT) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }
}