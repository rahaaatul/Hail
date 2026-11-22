package com.aistra.hail.app

import com.aistra.hail.HailApp
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HPackages
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppInfoTest {
    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)
        mockkObject(AppMetaCache)
        mockkObject(HPackages)
        every { AppMetaCache.get(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun entry(
        installed: Boolean,
        state: AppInfo.State,
    ) = AppMetaCache.Entry(
        packageName = "pkg",
        name = "pkg",
        isSystemApp = false,
        firstInstallTime = 0L,
        lastUpdateTime = 0L,
        flags = 0,
        enabled = true,
        installed = installed,
        state = state,
        sourceSignature = "",
    )

    @Test
    fun `name falls back to packageName when not in cache`() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)
        val info = AppInfo("com.example.pkg")
        assertEquals("com.example.pkg", info.name)
    }

    @Test
    fun `equals compares by packageName`() {
        assertTrue(AppInfo("pkg") == AppInfo("pkg"))
        assertFalse(AppInfo("pkg.a") == AppInfo("pkg.b"))
    }

    @Test
    fun `hashCode is based on packageName`() {
        assertEquals(AppInfo("pkg").hashCode(), AppInfo("pkg").hashCode())
    }

    @Test
    fun `tagIdList defaults to list with single 0`() {
        assertEquals(listOf(0), AppInfo("pkg").tagIdList)
    }

    @Test
    fun `default pinned is false`() {
        assertFalse(AppInfo("pkg").pinned)
    }

    @Test
    fun `default whitelisted is false`() {
        assertFalse(AppInfo("pkg").whitelisted)
    }

    @Test
    fun `applicationInfo is null when package not installed`() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns null
        HailApp.setAppForTest(mockApp)
        val info = AppInfo("com.example.notinstalled")
        assertEquals(null, info.applicationInfo)
    }

    @Test
    fun `state is UNKNOWN when cache miss`() {
        every { AppMetaCache.get("pkg") } returns null

        assertEquals(AppInfo.State.UNKNOWN, AppInfo("pkg").state)
    }

    @Test
    fun `isInstalled trusts the live predicate over a stale cache entry`() {
        every { AppMetaCache.get("pkg") } returns entry(installed = true, state = AppInfo.State.ENABLED)
        every { HPackages.isInstalled("pkg") } returns false

        assertFalse(AppInfo("pkg").isInstalled)
    }

    @Test
    fun `isInstalled is true when the live predicate says so`() {
        every { AppMetaCache.get("pkg") } returns null
        every { HPackages.isInstalled("pkg") } returns true

        assertTrue(AppInfo("pkg").isInstalled)
    }

    @Test
    fun `isInstalled is false when the cache entry is not installed`() {
        every { AppMetaCache.get("pkg") } returns entry(installed = false, state = AppInfo.State.NOT_FOUND)
        every { HPackages.isInstalled("pkg") } returns false

        assertFalse(AppInfo("pkg").isInstalled)
    }
}
