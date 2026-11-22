package com.aistra.hail.app

import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HShizuku

/**
 * Applies the freeze/unfreeze mode selected in [HailData.workingMode] to a single package.
 *
 * Kept out of [AppManager] so the mode dispatch stays a small, exhaustive mapping per working mode
 * family instead of one wide `when` over every mode.
 */
internal object AppFrozenDispatch {
    fun setAppFrozen(
        packageName: String,
        frozen: Boolean,
    ): Boolean {
        if (packageName == BuildConfig.APPLICATION_ID) return false
        val workingMode = HailData.workingMode
        return when {
            workingMode.startsWith(HailData.OWNER) -> setOwnerFrozen(packageName, frozen, workingMode)
            workingMode.startsWith(HailData.DHIZUKU) -> setDhizukuFrozen(packageName, frozen, workingMode)
            workingMode.startsWith(HailData.ISLAND) -> setIslandFrozen(packageName, frozen, workingMode)
            workingMode.startsWith(HailData.PRIVAPP) -> setPrivilegedFrozen(packageName, frozen, workingMode)
            workingMode.startsWith(HailData.JAILBREAK) -> HPackages.setAppDisabled(packageName, frozen)
            workingMode.startsWith(HailData.SU) -> setShellFrozen(packageName, frozen, workingMode)
            workingMode.startsWith(HailData.SHIZUKU) -> setShizukuFrozen(packageName, frozen, workingMode)
            else -> false
        }
    }

    private fun setOwnerFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_OWNER_HIDE -> HPolicy.setAppHidden(packageName, frozen)
            HailData.MODE_OWNER_SUSPEND -> HPolicy.setAppSuspended(packageName, frozen)
            else -> false
        }

    private fun setDhizukuFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
            HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
            else -> false
        }

    private fun setIslandFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_ISLAND_HIDE -> HIsland.setAppHidden(packageName, frozen)
            HailData.MODE_ISLAND_SUSPEND -> HIsland.setAppSuspended(packageName, frozen)
            else -> false
        }

    private fun setPrivilegedFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_PRIVAPP_STOP -> !frozen || AppManager.forceStopApp(packageName)
            HailData.MODE_PRIVAPP_DISABLE -> HPackages.setAppDisabled(packageName, frozen)
            else -> false
        }

    private fun setShellFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_SU_STOP -> !frozen || AppManager.forceStopApp(packageName)
            HailData.MODE_SU_DISABLE -> HShell.setAppDisabled(packageName, frozen)
            HailData.MODE_SU_HIDE -> HShell.setAppHidden(packageName, frozen)
            HailData.MODE_SU_SUSPEND -> HShell.setAppSuspended(packageName, frozen)
            else -> false
        }

    private fun setShizukuFrozen(
        packageName: String,
        frozen: Boolean,
        workingMode: String,
    ): Boolean =
        when (workingMode) {
            HailData.MODE_SHIZUKU_STOP -> !frozen || AppManager.forceStopApp(packageName)
            HailData.MODE_SHIZUKU_DISABLE -> HShizuku.setAppDisabled(packageName, frozen)
            HailData.MODE_SHIZUKU_HIDE -> HShizuku.setAppHidden(packageName, frozen)
            HailData.MODE_SHIZUKU_SUSPEND -> HShizuku.setAppSuspended(packageName, frozen)
            else -> false
        }
}
