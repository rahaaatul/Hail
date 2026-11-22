package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HDhizuku
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
                HailSettings.workingMode.startsWith(HailData.OWNER) -> HPolicy.lockScreen
                HailSettings.workingMode.startsWith(HailData.DHIZUKU) -> HDhizuku.lockScreen
                HailSettings.workingMode.startsWith(HailData.SU) || HailSettings.workingMode.startsWith(HailData.JAILBREAK) -> HShell.lockScreen
                HailSettings.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.lockScreen
                else -> false
            }

    fun isAppFrozen(packageName: String): Boolean =
        when {
            HailSettings.workingMode.endsWith(HailData.STOP) -> {
                HPackages.isAppStopped(packageName)
            }

            HailSettings.workingMode.endsWith(HailData.DISABLE) -> {
                HPackages.isAppDisabled(packageName)
            }

            HailSettings.workingMode.endsWith(HailData.HIDE) -> {
                HPackages.isAppHidden(packageName)
            }

            HailSettings.workingMode.endsWith(HailData.SUSPEND) -> {
                HPackages.isAppSuspended(packageName)
            }

            else -> {
                HPackages.isAppDisabled(packageName) ||
                    HPackages.isAppHidden(packageName) ||
                    HPackages.isAppSuspended(packageName)
            }
        }

    internal fun batchFreeze(
        frozen: Boolean,
        packages: Collection<String>,
        freeze: (String, Boolean) -> Boolean,
    ): BatchResult {
        val failed = packages.filterNot { freeze(it, frozen) }
        return BatchResult(
            total = packages.size,
            succeeded = packages.size - failed.size,
            failed = failed,
        )
    }

    fun setListFrozen(
        frozen: Boolean,
        packages: Collection<String>,
    ): BatchResult {
        val targets = packages.filter { it != BuildConfig.APPLICATION_ID }
        val result = batchFreeze(frozen, targets, ::setAppFrozenInternal)
        if (result.succeeded > 0) AppMetaCache.invalidateState(targets)
        return result
    }

    fun setAppFrozen(
        packageName: String,
        frozen: Boolean,
    ): Boolean =
        setAppFrozenInternal(packageName, frozen).also {
            if (it) AppMetaCache.invalidateState(listOf(packageName))
        }

    private fun setAppFrozenInternal(
        packageName: String,
        frozen: Boolean,
    ): Boolean = AppFrozenDispatch.setAppFrozen(packageName, frozen)

// TODO: replace the pm/cmd shell calls below with a libsu RootService (root) and a
    // ShizukuBinderWrapper (Shizuku) that call IPackageManager.deletePackageAsUser /
    // installExistingPackageAsUser and ActivityManager.getRunningAppProcesses directly, so we
    // drop the shell parsing (ps/dumpsys) and get structured results instead of exit codes + text.
    fun uninstallApp(packageName: String): Pair<Boolean, String?> {
        val workingMode = HailSettings.workingMode
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
            HailSettings.workingMode.startsWith(HailData.SU) || HailSettings.workingMode.startsWith(HailData.JAILBREAK) -> HShell.reinstallApp(packageName)
            HailSettings.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.reinstallApp(packageName)
            else -> false to null
        }

    val supportsForceStop: Boolean
        get() =
            HailSettings.workingMode.let {
                it.startsWith(HailData.SU) ||
                    it.startsWith(HailData.JAILBREAK) ||
                    it.startsWith(HailData.SHIZUKU) ||
                    it.startsWith(HailData.OWNER) ||
                    it.startsWith(HailData.DHIZUKU) ||
                    it.startsWith(HailData.PRIVAPP)
            }

    fun forceStopApp(packageName: String): Boolean {
        val workingMode = HailSettings.workingMode
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
                HailSettings.workingMode.startsWith(HailData.SU) || HailSettings.workingMode.startsWith(HailData.JAILBREAK) -> HShell.execute(command, true)
                HailSettings.workingMode.startsWith(HailData.SHIZUKU) -> HShizuku.execute(command)
                else -> 0 to null
            }
        }
}
