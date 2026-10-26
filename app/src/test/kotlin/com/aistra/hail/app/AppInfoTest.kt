package com.aistra.hail.app

import com.aistra.hail.HailApp
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInfoTest {
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
}
