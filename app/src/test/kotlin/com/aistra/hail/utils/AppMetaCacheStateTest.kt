package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.utils.AppMetaCache.withFreshState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class AppMetaCacheStateTest {
    @Before
    fun setUp() {
        HailApp.setAppForTest(RuntimeEnvironment.getApplication() as HailApp)
    }

    // ApplicationInfo exposes flags/enabled as public Java fields, which MockK cannot stub, so the
    // real type is instantiated and its fields are set directly.
    private fun info(
        flags: Int,
        enabled: Boolean = true,
    ): ApplicationInfo =
        ApplicationInfo().apply {
            this.flags = flags
            this.enabled = enabled
        }

    @Test
    fun `stateFrom returns NOT_FOUND for null info`() {
        assertEquals(AppInfo.State.NOT_FOUND, AppMetaCache.stateFrom(null))
    }

    @Test
    fun `stateFrom returns UNINSTALLED when FLAG_INSTALLED is clear`() {
        assertEquals(AppInfo.State.UNINSTALLED, AppMetaCache.stateFrom(info(flags = 0)))
    }

    @Test
    fun `stateFrom returns ENABLED for an installed app`() {
        assertEquals(AppInfo.State.ENABLED, AppMetaCache.stateFrom(info(ApplicationInfo.FLAG_INSTALLED)))
    }

    @Test
    fun `stateFrom returns STOPPED for a stopped app`() {
        val flags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_STOPPED
        assertEquals(AppInfo.State.STOPPED, AppMetaCache.stateFrom(info(flags)))
    }

    @Test
    fun `stateFrom returns DISABLED when the app is disabled`() {
        assertEquals(AppInfo.State.DISABLED, AppMetaCache.stateFrom(info(ApplicationInfo.FLAG_INSTALLED, enabled = false)))
    }

    @Test
    fun `installedFrom is false for null info`() {
        assertEquals(false, AppMetaCache.installedFrom(null))
    }

    @Test
    fun `installedFrom reads FLAG_INSTALLED`() {
        assertEquals(true, AppMetaCache.installedFrom(info(ApplicationInfo.FLAG_INSTALLED)))
        assertEquals(false, AppMetaCache.installedFrom(info(0)))
    }

    @Test
    fun `stateFrom returns UNINSTALLED for a data-only user app`() {
        val info =
            ApplicationInfo().apply {
                flags = 0
                enabled = true
            }

        assertEquals(AppInfo.State.UNINSTALLED, AppMetaCache.stateFrom(info))
        assertEquals(false, AppMetaCache.installedFrom(info))
    }

    private fun seedEntry() =
        AppMetaCache.Entry(
            packageName = "pkg",
            name = "pkg",
            isSystemApp = false,
            firstInstallTime = 0L,
            lastUpdateTime = 0L,
            flags = ApplicationInfo.FLAG_INSTALLED,
            enabled = true,
            installed = true,
            state = AppInfo.State.ENABLED,
            sourceSignature = "",
        )

    @Test
    fun `withFreshState picks up a disabled app`() {
        val info =
            ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_INSTALLED
                enabled = false
            }

        val updated = seedEntry().withFreshState(info)

        assertEquals(AppInfo.State.DISABLED, updated.state)
        assertEquals(false, updated.enabled)
        assertEquals(true, updated.installed)
    }

    @Test
    fun `withFreshState marks a data-only app uninstalled`() {
        val info =
            ApplicationInfo().apply {
                flags = 0
                enabled = true
            }

        val updated = seedEntry().withFreshState(info)

        assertEquals(AppInfo.State.UNINSTALLED, updated.state)
        assertEquals(false, updated.installed)
        assertEquals(true, updated.enabled)
    }

    @Test
    fun `withFreshState is idempotent`() {
        val info =
            ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_INSTALLED
                enabled = false
            }

        val once = seedEntry().withFreshState(info)
        val twice = once.withFreshState(info)

        assertEquals(once, twice)
    }

    @Test
    fun `withFreshState is a no-op for an unchanged enabled app`() {
        val info =
            ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_INSTALLED
                enabled = true
            }

        assertEquals(seedEntry(), seedEntry().withFreshState(info))
    }

    @Test
    fun `installedFrom is false when the APK is missing`() {
        val info =
            ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_INSTALLED
                sourceDir = "/data/app/does-not-exist/base.apk"
                enabled = true
            }

        assertFalse(AppMetaCache.installedFrom(info))
        assertEquals(AppInfo.State.UNINSTALLED, AppMetaCache.stateFrom(info))
    }

    @Test
    fun `installedFrom is true for a code-less package with no sourceDir`() {
        val info =
            ApplicationInfo().apply {
                flags = ApplicationInfo.FLAG_INSTALLED
                sourceDir = null
                enabled = true
            }

        assertTrue(AppMetaCache.installedFrom(info))
    }

    @Test
    fun `displayName falls back to the package name when no entry is cached`() {
        val packageName = "com.example.pkg"

        assertEquals(packageName, AppMetaCache.displayName(packageName))
    }
}
