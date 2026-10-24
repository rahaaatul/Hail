package com.aistra.hail.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.HailData
import com.aistra.hail.R
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.trySendBlocking
import rikka.shizuku.Shizuku

internal fun SettingsFragment.handleOwnerMode(): Boolean {
    return if (!HPolicy.isDeviceOwnerActive) {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_owner)
            .setMessage(getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND))
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(HPolicy.ADB_COMMAND) }.show()
            .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
        false
    } else true
}

internal fun SettingsFragment.handleDhizukuMode(rememberState: MutableState<String>, mode: String): Boolean {
    return runCatching {
        Dhizuku.init(app)
        when {
            Dhizuku.isPermissionGranted() -> true

            else -> {
                lifecycleScope.launch {
                    val result = callbackFlow {
                        Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                            override fun onRequestPermission(grantResult: Int) {
                                trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                            }
                        })
                        awaitClose()
                    }.first()
                    if (result) {
                        rememberState.value = mode
                        if (HTarget.O) HDhizuku.setDelegatedScopes()
                    }
                }
                false
            }
        }
    }.getOrElse {
        HLog.e(it)
        HUI.showToast(R.string.permission_denied)
        false
    }
}

internal fun SettingsFragment.handleSuMode(): Boolean {
    return if (!HShell.checkSU) {
        HUI.showToast(R.string.permission_denied)
        false
    } else true
}

internal fun SettingsFragment.handleShizukuMode(rememberState: MutableState<String>, mode: String): Boolean {
    return runCatching {
        when {
            Shizuku.isPreV11() -> check(!Shizuku.isPreV11()) { "unsupported shizuku version" }

            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true

            Shizuku.shouldShowRequestPermissionRationale() -> {
                HUI.showToast(R.string.permission_denied)
                false
            }

            else -> {
                lifecycleScope.launch {
                    val result = callbackFlow {
                        val shizukuRequestCode = 0
                        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                            if (requestCode == shizukuRequestCode) {
                                trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                            }
                        }
                        Shizuku.addRequestPermissionResultListener(listener)
                        Shizuku.requestPermission(shizukuRequestCode)
                        awaitClose {
                            Shizuku.removeRequestPermissionResultListener(listener)
                        }
                    }.first()
                    if (result) rememberState.value = mode
                }
                false
            }
        }
    }.getOrElse {
        HLog.e(it)
        HUI.showToast(R.string.shizuku_missing)
        false
    }
}

internal fun SettingsFragment.handleIslandMode(rememberState: MutableState<String>, mode: String): Boolean {
    return runCatching {
        when {
            mode == HailData.MODE_ISLAND_HIDE && HIsland.freezePermissionGranted() -> true

            mode == HailData.MODE_ISLAND_SUSPEND && HIsland.suspendPermissionGranted() -> true

            else -> {
                lifecycleScope.launch {
                    islandPermissionRequest?.cancel(CancellationException("Superseded by new request"))
                    islandPermissionRequest = CompletableDeferred()
                    requestPermissionLauncher.launch(
                        if (mode == HailData.MODE_ISLAND_HIDE) {
                            HIsland.PERMISSION_FREEZE_PACKAGE
                        } else {
                            HIsland.PERMISSION_SUSPEND_PACKAGE
                        },
                    )
                    if (islandPermissionRequest?.await() == true) {
                        rememberState.value = mode
                    }
                }
                false
            }
        }
    }.getOrElse {
        HLog.e(it)
        HUI.showToast(R.string.permission_denied)
        false
    }.also {
        if (it) HIsland.checkOwnerApp()
    }
}

internal fun SettingsFragment.handlePrivappMode(): Boolean {
    return if (!HPackages.isPrivilegedApp(app.packageName)) {
        HUI.showToast(R.string.permission_denied)
        false
    } else true
}

internal fun resetDynamicShortcuts() {
    HShortcuts.removeAllDynamicShortcuts()
    HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
}

internal fun requestBackgroundActivity(context: Context) {
    val powerManager = context.getSystemService(PowerManager::class.java)
    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) return
    context.startActivity(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        },
    )
}

internal fun iconPackName(pack: String, context: Context, iconPackNames: Map<String, String>): String =
    if (pack == HailData.ACTION_NONE) {
        context.getString(R.string.action_none)
    } else {
        iconPackNames[pack] ?: pack
    }
