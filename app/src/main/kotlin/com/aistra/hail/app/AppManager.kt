package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {
    val lockScreen: Boolean
        get() =
            when {
                HailData.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
                HailData.workingMode.startsWith(HailData.DHIZUKU) -> HDhizuku.lockScreen
                HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(HailData.JAILBREAK) -> HShell.lockScreen
                HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
                else -> false
            }

    fun isAppFrozen(packageName: String): Boolean =
        when {
            HailData.workingMode.endsWith(HailData.STOP) -> {
                HPackages.isAppStopped(packageName)
            }

            HailData.workingMode.endsWith(HailData.DISABLE) -> {
                HPackages.isAppDisabled(packageName)
            }

            HailData.workingMode.endsWith(HailData.HIDE) -> {
                HPackages.isAppHidden(packageName)
            }

            HailData.workingMode.endsWith(HailData.SUSPEND) -> {
                HPackages.isAppSuspended(packageName)
            }

            else -> {
                HPackages.isAppDisabled(packageName) ||
                    HPackages.isAppHidden(packageName) ||
                    HPackages.isAppSuspended(packageName)
            }
        }

    fun setListFrozen(
        frozen: Boolean,
        vararg appInfo: AppInfo,
    ): String? {
        val excludeMe = appInfo.filter { it.packageName != BuildConfig.APPLICATION_ID }
        var i = 0
        var denied = false
        var name = String()
        when (HailData.workingMode) {
            // call setListFrozen for some batch-style working mode here
            // fallback to setAppFrozen otherwise
            else -> {
                excludeMe.forEach {
                    when {
                        setAppFrozen(it.packageName, frozen) -> {
                            i++
                            name = it.name
                        }

                        it.applicationInfo != null -> {
                            denied = true
                        }
                    }
                }
            }
        }
        return if (denied && i == 0) {
            null
        } else if (i == 1) {
            name
        } else {
            i.toString()
        }
    }

    fun setAppFrozen(
        packageName: String,
        frozen: Boolean,
    ): Boolean =
        packageName != BuildConfig.APPLICATION_ID &&
            when (HailData.workingMode) {
                HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
                HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
                HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
                HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
                HailData.MODE_SU_STOP -> !frozen || forceStopApp(packageName)
                HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
                HailData.MODE_SU_HIDE -> HShell.setAppHidden(packageName, frozen)
                HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
                HailData.MODE_SHIZUKU_STOP -> !frozen || forceStopApp(packageName)
                HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
                HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
                HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
                HailData.MODE_ISLAND_HIDE -> HIsland.setAppHidden(packageName, frozen)
                HailData.MODE_ISLAND_SUSPEND -> HIsland.setAppSuspended(packageName, frozen)
                HailData.MODE_PRIVAPP_STOP -> !frozen || forceStopApp(packageName)
                HailData.MODE_PRIVAPP_DISABLE, HailData.MODE_JAILBREAK_DISABLE -> HPackages.setAppDisabled(packageName, frozen)
                else -> false
            }.let { result ->
                if (result) AppMetaCache.invalidateState(listOf(packageName))
                result
            }

// TODO: replace the pm/cmd shell calls below with a libsu RootService (root) and a
    // ShizukuBinderWrapper (Shizuku) that call IPackageManager.deletePackageAsUser /
    // installExistingPackageAsUser and ActivityManager.getRunningAppProcesses directly, so we
    // drop the shell parsing (ps/dumpsys) and get structured results instead of exit codes + text.
    fun uninstallApp(packageName: String): Pair<Boolean, String?> {
        val workingMode = HailData.workingMode
        val result: Pair<Boolean, String?> =
            when {
                // SU/Shizuku own the uninstall and surface errors; OWNER/DHIZUKU fall back to
                // the system dialog on failure; other modes always use the system dialog.
                workingMode.startsWith(HailData.SU) || workingMode.startsWith(HailData.JAILBREAK) -> {
                    HShell.uninstallApp(packageName)
                }

                workingMode.startsWith(HailData.SHIZUKU) -> {
                    HShizuku.uninstallApp(packageName)
                }

                workingMode.startsWith(HailData.OWNER) && HPolicy.uninstallApp(packageName) -> {
                    true to null
                }

                workingMode.startsWith(HailData.DHIZUKU) && HDhizuku.uninstallApp(packageName) -> {
                    true to null
                }

                else -> {
                    HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
                    false to null
                }
            }
        return result
    }

    fun reinstallApp(packageName: String): Pair<Boolean, String?> =
        when {
            HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(HailData.JAILBREAK) -> HShell.reinstallApp(packageName)
            HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.reinstallApp(packageName)
            else -> false to null
        }

    val supportsForceStop: Boolean
        get() =
            HailData.workingMode.let {
                it.startsWith(HailData.SU) ||
                    it.startsWith(HailData.JAILBREAK) ||
                    it.startsWith(HailData.SHIZUKU) ||
                    it.startsWith(HailData.OWNER) ||
                    it.startsWith(HailData.DHIZUKU) ||
                    it.startsWith(HailData.PRIVAPP)
            }

    fun forceStopApp(packageName: String): Boolean {
        val workingMode = HailData.workingMode
        return when {
            workingMode.startsWith(HailData.SU) || workingMode.startsWith(HailData.JAILBREAK) -> {
                HShell.forceStopApp(packageName)
            }

            workingMode.startsWith(HailData.SHIZUKU) -> {
                HShizuku.forceStopApp(packageName)
            }

            workingMode.startsWith(HailData.OWNER) || workingMode.startsWith(HailData.DHIZUKU) || workingMode.startsWith(HailData.PRIVAPP) -> {
                HPackages.forceStopApp(packageName)
            }

            else -> {
                false
            }
        }
    }

    suspend fun execute(command: String): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            when {
                HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(HailData.JAILBREAK) -> HShell.execute(command, true)
                HailData.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
                else -> 0 to null
            }
        }
}
