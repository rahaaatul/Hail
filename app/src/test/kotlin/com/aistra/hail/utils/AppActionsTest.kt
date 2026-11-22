package com.aistra.hail.utils

import android.content.Intent
import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.BatchResult
import com.aistra.hail.app.HailData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppActionsTest {
    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        mockkObject(HailData)
        mockkObject(AppManager)
        mockkObject(HPackages)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `ensureUnfrozen returns failure when package not found`() =
        runTest {
            every { AppManager.isAppFrozen("missing") } returns false
            every { HPackages.getApplicationInfoOrNull("missing") } returns null

            val result = AppActions.ensureUnfrozen("missing")

            assertTrue(result.isFailure)
        }

    @Test
    fun `ensureUnfrozen returns failure when unfreeze fails`() =
        runTest {
            val pkg = "com.example.test"
            every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
            every { AppManager.isAppFrozen(pkg) } returns true
            coEvery { AppManager.setAppFrozen(pkg, false) } returns false

            val result = AppActions.ensureUnfrozen(pkg)

            assertTrue(result.isFailure)
        }

    @Test
    fun `ensureUnfrozen returns success when already unfrozen`() =
        runTest {
            val pkg = "com.example.test"
            every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
            every { AppManager.isAppFrozen(pkg) } returns false

            val result = AppActions.ensureUnfrozen(pkg)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `freezePackages returns the batch result of setListFrozen`() =
        runTest {
            val pkg = "com.example.test"
            val batch = BatchResult(total = 1, succeeded = 1, failed = emptyList())
            every { AppManager.setListFrozen(true, listOf(pkg)) } returns batch

            val result = AppActions.freezePackages(true, listOf(pkg))

            assertEquals(batch, result.getOrThrow())
        }

    @Test
    fun `freezePackages reports partial failure`() =
        runTest {
            val packages = listOf("a", "b")
            every { AppManager.setListFrozen(false, packages) } returns
                BatchResult(total = 2, succeeded = 1, failed = listOf("b"))

            val result = AppActions.freezePackages(false, packages).getOrThrow()

            assertEquals(1, result.succeeded)
            assertEquals(listOf("b"), result.failed)
            assertEquals(2, result.total)
        }

    @Test
    fun `freezePackages returns failure when setListFrozen throws`() =
        runTest {
            every { AppManager.setListFrozen(true, listOf("a")) } throws IllegalStateException("denied")

            val result = AppActions.freezePackages(true, listOf("a"))

            assertTrue(result.isFailure)
        }

    @Test
    fun `getLaunchIntent returns failure when no launch intent found`() =
        runTest {
            every { HailData.workingMode } returns HailData.MODE_DEFAULT
            every { HailApp.app.packageManager.getLaunchIntentForPackage("missing") } returns null

            val result = AppActions.getLaunchIntent("missing")

            assertTrue(result.isFailure)
        }

    @Test
    fun `getLaunchIntent returns intent when available`() =
        runTest {
            every { HailData.workingMode } returns HailData.MODE_DEFAULT
            val intent = mockk<Intent>(relaxed = true)
            every { HailApp.app.packageManager.getLaunchIntentForPackage("com.example.test") } returns intent
            every { HailApp.app.setAutoFreezeService() } returns Unit
            every { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } returns intent

            val result = AppActions.getLaunchIntent("com.example.test")

            assertTrue(result.isSuccess)
            assertEquals(intent, result.getOrThrow())
        }
}
