package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HLog

class FrozenWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val packageName = inputData.getString(HailData.KEY_PACKAGE)
        if (packageName == null) {
            HLog.e("Missing package name in FrozenWorker input")
            return Result.failure()
        }
        val shouldFreeze = inputData.getBoolean(HailData.KEY_FROZEN, true)
        return if (AppManager.setAppFrozen(packageName, shouldFreeze)) {
            Result.success()
        } else {
            HLog.e("Failed to ${if (shouldFreeze) "freeze" else "unfreeze"} $packageName")
            // AppManager.setAppFrozen() returns a plain Boolean: false for both
            // permanent failures (package not found, permission denied) and
            // transient ones (Shizuku/Island service temporarily unavailable).
            // We cannot reliably distinguish them from the return value alone,
            // so classifying by freeze-vs-unfreeze direction is incorrect in both
            // directions. Instead, classify by working mode: MODE_DEFAULT always
            // returns false (no backend service configured), so retrying serves no
            // purpose. For all other modes the failure could be transient, so retry
            // and let WorkManager's exponential backoff eventually give up.
            if (HailData.workingMode == HailData.MODE_DEFAULT) Result.failure() else Result.retry()
        }
    }
}