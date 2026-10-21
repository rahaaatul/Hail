package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object HShell {
    init {
        Shell.enableLegacyStderrRedirection = true
        Shell.setDefaultBuilder(Shell.Builder.create())
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var warmupJob: Job? = null

    private fun getCurrentUserId(): Int = execSU("am get-current-user").second?.trim()?.toIntOrNull() ?: 0

    private val userArg: String
        get() = "--user ${getCurrentUserId()}"

    fun start() = synchronized(this) {
        if (warmupJob?.isActive == true) return@synchronized
        Shell.getCachedShell()?.let { shell ->
            if (shell.isAlive) return@synchronized
            shell.close()
        }
        warmupJob = scope.launch {
            execute(":")
            synchronized(this@HShell) {
                warmupJob = null
                if (!isActive) Shell.getCachedShell()?.close()
            }
        }
    }

    fun stop() = synchronized(this) {
        warmupJob?.cancel()
        warmupJob = null
        Shell.getCachedShell()?.close()
    }

    fun execute(command: String): Pair<Int, String?> = synchronized(this) {
        runCatching {
            Shell.getCachedShell()?.let { shell ->
                if (!shell.isAlive) shell.close()
            }
            Shell.cmd(command).exec().let { result ->
                if (Shell.getCachedShell()?.isAlive == false) Shell.getCachedShell()?.close()
                result.code to result.out.joinToString("\n").ifBlank { null }
            }
        }.getOrElse { 1 to it.stackTraceToString() }
    }

    private fun execSU(command: String) = execute(command)

    val checkSU get() = execSU("whoami").second?.trim() == "root"

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun forceStopApp(packageName: String): Boolean =
        HPackages.shellArg(packageName)?.let { execSU("am force-stop $userArg $it").first == 0 } ?: false

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        HPackages.shellArg(packageName)
            ?.let { execSU("pm ${if (disabled) "disable" else "enable"} $userArg $it").first == 0 } ?: false

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        HPackages.shellArg(packageName)
            ?.let { execSU("pm ${if (hidden) "hide" else "unhide"} $userArg $it").first == 0 } ?: false

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        HPackages.shellArg(packageName)
            ?.let { execSU("pm ${if (suspended) "suspend" else "unsuspend"} $userArg $it").first == 0 } ?: false

    fun uninstallApp(packageName: String) = HPackages.shellArg(packageName)?.let {
        execSU(
            "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall $userArg"} $it"
        ).first == 0
    } ?: false

    fun reinstallApp(packageName: String) = HPackages.shellArg(packageName)?.let {
        execSU("pm install-existing $userArg $it").first == 0
    } ?: false

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean) = HPackages.shellArg(packageName)?.let {
        execSU(
            "appops set $userArg $it RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
        ).first == 0
    } ?: false
}