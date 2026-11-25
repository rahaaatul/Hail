package com.aistra.hail.app

import androidx.test.core.app.ApplicationProvider
import com.aistra.hail.HailApp
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HShizuku
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Robolectric 4.18.1 defaults to targetSdk 36 whose android-all jar is cached in CI.
@Config(application = HailApp::class)
class AppFrozenDispatchTest {
    private val packageName = "com.example.frozen"

    @Before
    fun setUp() {
        HailApp.setAppForTest(ApplicationProvider.getApplicationContext<HailApp>())
        mockkObject(HailSettings)
        mockkObject(HShell)
        mockkObject(HShizuku)
        mockkObject(HPolicy)
        mockkObject(HDhizuku)
        mockkObject(HIsland)
        every { HShell.setAppSuspended(any(), any()) } returns true
        every { HShizuku.setAppSuspended(any(), any()) } returns true
        every { HPolicy.setAppSuspended(any(), any()) } returns true
        every { HDhizuku.setAppSuspended(any(), any()) } returns true
        every { HIsland.setAppSuspended(any(), any()) } returns true
        every { HShell.setAppDisabled(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `SU suspend mode suspends through HShell`() {
        every { HailSettings.workingMode } returns HailData.MODE_SU_SUSPEND

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))
        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, false))

        verify(exactly = 1) { HShell.setAppSuspended(packageName, true) }
        verify(exactly = 1) { HShell.setAppSuspended(packageName, false) }
        verifyNoDisabledOrHiddenFallback()
    }

    @Test
    fun `Shizuku suspend mode suspends through HShizuku`() {
        every { HailSettings.workingMode } returns HailData.MODE_SHIZUKU_SUSPEND

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))
        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, false))

        verify(exactly = 1) { HShizuku.setAppSuspended(packageName, true) }
        verify(exactly = 1) { HShizuku.setAppSuspended(packageName, false) }
        verifyNoDisabledOrHiddenFallback()
    }

    @Test
    fun `Owner suspend mode suspends through HPolicy`() {
        every { HailSettings.workingMode } returns HailData.MODE_OWNER_SUSPEND

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))
        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, false))

        verify(exactly = 1) { HPolicy.setAppSuspended(packageName, true) }
        verify(exactly = 1) { HPolicy.setAppSuspended(packageName, false) }
        verifyNoDisabledOrHiddenFallback()
    }

    @Test
    fun `Dhizuku suspend mode suspends through HDhizuku`() {
        every { HailSettings.workingMode } returns HailData.MODE_DHIZUKU_SUSPEND

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))
        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, false))

        verify(exactly = 1) { HDhizuku.setAppSuspended(packageName, true) }
        verify(exactly = 1) { HDhizuku.setAppSuspended(packageName, false) }
        verifyNoDisabledOrHiddenFallback()
    }

    @Test
    fun `Island suspend mode suspends through HIsland`() {
        every { HailSettings.workingMode } returns HailData.MODE_ISLAND_SUSPEND

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))
        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, false))

        verify(exactly = 1) { HIsland.setAppSuspended(packageName, true) }
        verify(exactly = 1) { HIsland.setAppSuspended(packageName, false) }
        verifyNoDisabledOrHiddenFallback()
    }

    @Test
    fun `SU disable mode disables through HShell and never suspends`() {
        every { HailSettings.workingMode } returns HailData.MODE_SU_DISABLE

        assertTrue(AppFrozenDispatch.setAppFrozen(packageName, true))

        verify(exactly = 1) { HShell.setAppDisabled(packageName, true) }
        verify(exactly = 0) {
            HShell.setAppSuspended(any(), any())
            HShizuku.setAppSuspended(any(), any())
            HPolicy.setAppSuspended(any(), any())
            HDhizuku.setAppSuspended(any(), any())
            HIsland.setAppSuspended(any(), any())
        }
    }

    private fun verifyNoDisabledOrHiddenFallback() {
        verify(exactly = 0) {
            HShell.setAppDisabled(any(), any())
            HShell.setAppHidden(any(), any())
            HShizuku.setAppDisabled(any(), any())
            HShizuku.setAppHidden(any(), any())
            HPolicy.setAppHidden(any(), any())
            HDhizuku.setAppHidden(any(), any())
            HIsland.setAppHidden(any(), any())
        }
    }
}
