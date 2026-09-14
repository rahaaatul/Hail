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
            // A freeze that fails is almost always a permanent condition (wrong
            // working mode, package not found, or disabled app), so retrying would
            // loop forever. Unfreeze failures are more likely transient (e.g. a
            // temporarily unavailable Shizuku/Island service), so allow a retry.
            if (shouldFreeze) Result.failure() else Result.retry()
        }
    }
}