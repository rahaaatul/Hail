package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import org.junit.Assert.assertEquals
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
        HailApp.setAppForTest(RuntimeEnvironment.application as HailApp)
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
}
