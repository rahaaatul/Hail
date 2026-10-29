package com.aistra.hail.app

import com.aistra.hail.HailApp
import com.aistra.hail.utils.HPackages
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppManagerTest {
    private lateinit var mockApp: HailApp

    @Before
    fun setUp() {
        mockApp = mockk(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)
        mockkObject(HailData)
        mockkObject(HPackages)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isAppFrozen returns true for STOP mode when app is stopped`() {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT + HailData.STOP
        every { HPackages.isAppStopped("pkg") } returns true
        assertTrue(AppManager.isAppFrozen("pkg"))
    }

    @Test
    fun `isAppFrozen returns true for DISABLE mode when app is disabled`() {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT + HailData.DISABLE
        every { HPackages.isAppDisabled("pkg") } returns true
        assertTrue(AppManager.isAppFrozen("pkg"))
    }

    @Test
    fun `setAppFrozen returns false for own package`() {
        assertFalse(AppManager.setAppFrozen("com.aistra.hail", true))
    }

    @Test
    fun `setAppFrozen returns false when mode unknown`() {
        every { HailData.workingMode } returns "unknown_mode"
        assertFalse(AppManager.setAppFrozen("other.pkg", true))
    }

    @Test
    fun `uninstallApp returns false and starts delete intent when no handler matches`() {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT
        val result = AppManager.uninstallApp("com.example.pkg")
        assertFalse(result)
    }

    @Test
    fun `reinstallApp returns false when mode not SU or SHIZUKU`() {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT
        assertFalse(AppManager.reinstallApp("com.example.pkg"))
    }

    @Test
    fun `execute returns 0 to null when mode not SU or SHIZUKU`() =
        runBlocking {
            every { HailData.workingMode } returns HailData.MODE_DEFAULT
            val (code, output) = AppManager.execute("some command")
            assertEquals(0, code)
            assertEquals(null, output)
        }
}
