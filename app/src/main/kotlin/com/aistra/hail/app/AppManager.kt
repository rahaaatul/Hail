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

    suspend fun setListFrozen(
        frozen: Boolean,
        notifier: BulkOperationNotifier? = null,
        vararg appInfo: AppInfo
    ): String? = withContext(Dispatchers.IO) {
        val excludeMe = appInfo.filter { it.packageName != BuildConfig.APPLICATION_ID }
        // Filter out uninstalled apps (no applicationInfo)
        val validApps = excludeMe.filter { it.applicationInfo != null }
        if (validApps.isEmpty()) return@withContext "0"

        notifier?.start(HailApp.app, validApps.size, frozen)

        var processed = 0
        var success = 0
        var failed = 0

        validApps.forEachIndexed { index, appInfo ->
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

            // Load icon on IO dispatcher (already on IO)
            val icon = appInfo.applicationInfo?.let { info ->
                AppIconCache.getOrLoadBitmap(HailApp.app, info, Process.myUserHandle().hashCode(), 128)
            }
            notifier?.update(processed, validApps.size, appInfo.name.toString(), icon)
        }

        notifier?.complete(success, failed, frozen)

        return@withContext when {
            notifier?.isCancelled == true -> "cancelled"
            success == 0 -> null
            success == 1 -> validApps.first { AppManager.isAppFrozen(it.packageName) == frozen }.name.toString()
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