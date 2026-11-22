package com.aistra.hail

import android.app.Application
import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.UserHandle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.DirtyDataUpdater
import com.aistra.hail.app.HailData
import com.aistra.hail.app.migrateSettingsFromSharedPreferences
import com.aistra.hail.services.AutoFreezeService
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
        AppMetaCache.warmUp()
        HailData.warmUp()
        // Registered for the whole process lifetime so cache invalidation happens regardless of
        // which tab is visible (a data-only uninstall must disappear from Home even from Apps).
        (getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps)
            ?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
        DirtyDataUpdater.update(app)
        applicationScope.launch { migrateSettingsFromSharedPreferences() }

        // Observe working_mode changes from Proto DataStore and sync root shell.
        applicationScope.launch {
            withContext(Dispatchers.Main) {
                var lastWorkingMode = HailData.workingMode
                HailData.settingsData.collect { settings ->
                    if (settings.workingMode != lastWorkingMode) {
                        lastWorkingMode = settings.workingMode
                        syncRootShell()
                    }
                    if (settings.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
                }
            }
        }

        if (!HTarget.S) setAppTheme(HailData.appTheme)
        if (HailData.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncRootShell() {
        if (HailData.workingMode.startsWith(HailData.SU)) HShell.start() else HShell.stop()
    }

    fun setAutoFreezeService(
        autoFreezeAfterLock: Boolean = HailData.autoFreezeAfterLock,
        context: Context = app,
    ) {
        val start =
            autoFreezeAfterLock &&
                HailData.checkedList.any {
                    it.packageName != packageName && it.isInstalled && !AppManager.isAppFrozen(it.packageName) && !it.whitelisted
                }
        val intent = Intent(app, AutoFreezeService::class.java)
        if (start) {
            setAutoFreezeServiceEnabled(true)
            ContextCompat.startForegroundService(context, intent)
        } else {
            stopService(intent)
            setAutoFreezeServiceEnabled(false)
        }
    }

    private val launcherAppsCallback =
        object : LauncherApps.Callback() {
            override fun onPackageAdded(
                packageName: String,
                user: UserHandle,
            ) {
                AppMetaCache.invalidatePackage(packageName)
            }

            override fun onPackageRemoved(
                packageName: String,
                user: UserHandle,
            ) {
                AppMetaCache.invalidatePackage(packageName)
            }

            override fun onPackageChanged(
                packageName: String,
                user: UserHandle,
            ) {
                AppMetaCache.invalidatePackage(packageName)
            }

            override fun onPackagesAvailable(
                packageNames: Array<String>,
                user: UserHandle,
                available: Boolean,
            ) {
                packageNames.forEach { AppMetaCache.invalidatePackage(it) }
            }

            override fun onPackagesUnavailable(
                packageNames: Array<String>,
                user: UserHandle,
                available: Boolean,
            ) {
                packageNames.forEach { AppMetaCache.invalidatePackage(it) }
            }

            override fun onPackagesSuspended(
                packageNames: Array<String>,
                user: UserHandle,
            ) {
                packageNames.forEach { AppMetaCache.invalidatePackage(it) }
            }

            override fun onPackagesUnsuspended(
                packageNames: Array<String>,
                user: UserHandle,
            ) {
                packageNames.forEach { AppMetaCache.invalidatePackage(it) }
            }
        }

    fun setAutoFreezeServiceEnabled(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            ComponentName(app, AutoFreezeService::class.java),
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    fun setAppTheme(theme: String) {
        if (HTarget.S) {
            getSystemService<UiModeManager>()!!.setApplicationNightMode(
                when (theme) {
                    HailData.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
                    HailData.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
                    else -> UiModeManager.MODE_NIGHT_AUTO
                },
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(
                when (theme) {
                    HailData.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    HailData.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                },
            )
        }
    }

    companion object {
        lateinit var app: HailApp private set

        fun setAppForTest(testApp: HailApp) {
            app = testApp
        }
    }
}
