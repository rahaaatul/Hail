package com.aistra.hail.app

import android.content.Intent
import android.os.Process
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp
import com.aistra.hail.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object AppManager {
    val lockScreen: Boolean
        get() = when {
            HailData.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
            HailData.workingMode.startsWith(HailData.DHIZUKU) -> HDhizuku.lockScreen
            HailData.workingMode.startsWith(HailData.SU) -> HShell.lockScreen
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
            else -> false
        }

    fun isAppFrozen(packageName: String): Boolean = when {
        HailData.workingMode.endsWith(HailData.STOP) -> HPackages.isAppStopped(packageName)
        HailData.workingMode.endsWith(HailData.DISABLE) -> HPackages.isAppDisabled(packageName)
        HailData.workingMode.endsWith(HailData.HIDE) -> HPackages.isAppHidden(packageName)
        HailData.workingMode.endsWith(HailData.SUSPEND) -> HPackages.isAppSuspended(packageName)
        else -> HPackages.isAppDisabled(packageName)
                || HPackages.isAppHidden(packageName)
                || HPackages.isAppSuspended(packageName)
    }

    private fun getPmAction(frozen: Boolean): String? = when {
        HailData.workingMode.endsWith(HailData.DISABLE) -> if (frozen) "disable" else "enable"
        HailData.workingMode.endsWith(HailData.HIDE) -> if (frozen) "hide" else "unhide"
        HailData.workingMode.endsWith(HailData.SUSPEND) -> if (frozen) "suspend" else "unsuspend"
        HailData.workingMode.endsWith(HailData.STOP) -> if (frozen) "force-stop" else null
        else -> null
    }

    suspend fun setListFrozen(
        frozen: Boolean,
        notifier: BulkOperationNotifier? = null,
        vararg appInfo: AppInfo
    ): String? = withContext(Dispatchers.IO) {
        val excludeMe = appInfo.filter { it.packageName != BuildConfig.APPLICATION_ID }
        // Filter out uninstalled apps (no applicationInfo)
        val validApps = excludeMe.filter { it.applicationInfo != null }
        if (validApps.isEmpty()) return@withContext "0"

        // Route to batch execution for SU mode only; Shizuku/others use sequential (binder = fast)
        // STOP mode: interBatchDelay = 0 for all speeds (am force-stop is lightweight)
        val isStopMode = HailData.workingMode.endsWith(HailData.STOP)

        return@withContext when {
            HailData.workingMode.startsWith(HailData.SU) -> when (HailData.workingSpeed) {
                HailData.SPEED_AGGRESSIVE -> executeBatch(frozen, notifier, validApps, batchSize = 10, interBatchDelay = if (isStopMode) 0 else 100)
                HailData.SPEED_BALANCED -> executeBatch(frozen, notifier, validApps, batchSize = 4, interBatchDelay = if (isStopMode) 0 else 500)
                HailData.SPEED_RELAXED -> executeBatch(frozen, notifier, validApps, batchSize = 1, interBatchDelay = 0)
                else -> executeSequential(frozen, notifier, validApps)
            }
            else -> executeSequential(frozen, notifier, validApps) // Shizuku, DHIZUKU, OWNER, ISLAND, PRIVAPP
        }
    }

    private suspend fun executeBatch(
        frozen: Boolean,
        notifier: BulkOperationNotifier?,
        apps: List<AppInfo>,
        batchSize: Int,
        interBatchDelay: Long
    ): String? = withContext(Dispatchers.IO) {
        val pmAction = getPmAction(frozen) ?: return@withContext "unsupported_mode"
        val userId = HShell.getCurrentUserId()

        // For STOP mode, no unfreeze - only freeze
        if (pmAction == "force-stop" && !frozen) return@withContext "0"

        val isStopMode = pmAction == "force-stop"

        val commands = apps.map { app ->
            "pm $pmAction --user $userId ${app.packageName}"
        }

        var processed = 0
        var success = 0
        var failed = 0

        notifier?.start(HailApp.app, apps.size, frozen)

        // Use BatchUtils.chunkCommands to enforce MAX_COMMANDS_PER_SCRIPT safety cap
        BatchUtils.chunkCommands(commands, batchSize).forEach { batchCommands ->
            if (notifier?.isCancelled == true) return@withContext "cancelled"

            // Only SU mode gets batching; Shizuku uses binder (already fast)
            val results = when {
                HailData.workingMode.startsWith(HailData.SU) -> HShell.executeBatch(batchCommands)
                else -> batchCommands.map { HShell.execute(it, true) } // fallback (shouldn't reach for SU)
            }

            results.forEachIndexed { batchIndex, (exitCode, output) ->
                val appIndex = processed + batchIndex
                val app = apps[appIndex]
                val result = exitCode == 0
                if (result) success++ else failed++
                processed++

                // Load icon and update notification with error handling
                try {
                    val icon = app.applicationInfo?.let { info ->
                        AppIconCache.getOrLoadBitmap(HailApp.app, info, Process.myUserHandle().hashCode(), 128)
                    }
                    notifier?.update(processed, apps.size, app.name.toString(), icon)
                } catch (e: Exception) {
                    HLog.e("Error updating notification for ${app.packageName}: $e")
                    notifier?.update(processed, apps.size, app.name.toString(), null)
                }
            }

            // STOP mode (am force-stop) is lightweight - no inter-batch delay needed
            // Other modes respect the configured interBatchDelay
            val effectiveDelay = if (isStopMode) 0L else interBatchDelay
            if (batchCommands.size == batchSize && effectiveDelay > 0) {
                delay(effectiveDelay)
            }
        }

        notifier?.complete(success, failed, frozen)
        return@withContext when {
            notifier?.isCancelled == true -> "cancelled"
            success == 0 -> null
            success == 1 -> apps.first { AppManager.isAppFrozen(it.packageName) == frozen }.name.toString()
            else -> success.toString()
        }
    }

    private suspend fun executeSequential(
        frozen: Boolean,
        notifier: BulkOperationNotifier?,
        apps: List<AppInfo>
    ): String? = withContext(Dispatchers.IO) {
        notifier?.start(HailApp.app, apps.size, frozen)

        var processed = 0
        var success = 0
        var failed = 0

        apps.forEachIndexed { index, appInfo ->
            if (notifier?.isCancelled == true) return@withContext "cancelled"
            // Throttle based on working speed setting
            when (HailData.workingSpeed) {
                HailData.SPEED_AGGRESSIVE -> {
                    // Small delay to avoid overwhelming system with su calls
                    if (index > 0) delay(200)
                }
                HailData.SPEED_BALANCED -> {
                    // Pause after every 4 apps
                    if (index > 0 && (index + 1) % 4 == 0) delay(2000)
                }
                HailData.SPEED_RELAXED -> {
                    // Pause after each app - longer delay for heavy pm commands
                    if (index > 0) delay(3000)
                }
            }
            val result = setAppFrozen(appInfo.packageName, frozen)
            if (result) success++ else failed++
            processed++

            // Load icon and update notification with error handling
            try {
                val icon = appInfo.applicationInfo?.let { info ->
                    AppIconCache.getOrLoadBitmap(HailApp.app, info, Process.myUserHandle().hashCode(), 128)
                }
                notifier?.update(processed, apps.size, appInfo.name.toString(), icon)
            } catch (e: Exception) {
                HLog.e("Error updating notification for ${appInfo.packageName}: $e")
                notifier?.update(processed, apps.size, appInfo.name.toString(), null)
            }
        }

        notifier?.complete(success, failed, frozen)

        return@withContext when {
            notifier?.isCancelled == true -> "cancelled"
            success == 0 -> null
            success == 1 -> apps.first { AppManager.isAppFrozen(it.packageName) == frozen }.name.toString()
            else -> success.toString()
        }
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (HailData.workingMode) {
            HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
            HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
            HailData.MODE_SU_STOP -> !frozen || HShell.forceStopApp(packageName)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_HIDE -> HShell.setAppHidden(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            HailData.MODE_SHIZUKU_STOP -> !frozen || HShizuku.forceStopApp(packageName)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
            HailData.MODE_ISLAND_HIDE -> HIsland.setAppHidden(packageName, frozen)
            HailData.MODE_ISLAND_SUSPEND -> HIsland.setAppSuspended(packageName, frozen)
            HailData.MODE_PRIVAPP_STOP -> !frozen || HPackages.forceStopApp(packageName)
            HailData.MODE_PRIVAPP_DISABLE -> HPackages.setAppDisabled(packageName, frozen)
            else -> false
        }

    fun uninstallApp(packageName: String): Boolean {
        when {
            HailData.workingMode.startsWith(HailData.OWNER) ->
                if (HPolicy.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.DHIZUKU) ->
                if (HDhizuku.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.SU) ->
                if (HShell.uninstallApp(packageName)) return true

            HailData.workingMode.startsWith(HailData.SHIZUKU) ->
                if (HShizuku.uninstallApp(packageName)) return true
        }
        HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
        return false
    }

    fun reinstallApp(packageName: String): Boolean = when {
        HailData.workingMode.startsWith(HailData.SU) -> HShell.reinstallApp(packageName)
        HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.reinstallApp(packageName)
        else -> false
    }

    suspend fun execute(command: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        when {
            HailData.workingMode.startsWith(HailData.SU) -> HShell.execute(command, true)
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
            else -> 0 to null
        }
    }
}