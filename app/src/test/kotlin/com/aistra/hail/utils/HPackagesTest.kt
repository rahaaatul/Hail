package com.aistra.hail.utils

import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HPackagesTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.filesDir } returns temporaryFolder.newFolder("files")
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        // A name that is refused is reported through HLog.w, and
        // android.util.Log is not on a unit test classpath, so the object has
        // to be stubbed rather than reached through.
        mockkObject(HLog)
        every { HLog.w(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `a package name is a package name`() {
        assertTrue(HPackages.isValidPackageName("com.example.app"))
        assertTrue(HPackages.isValidPackageName("com.example.app_2"))
        assertTrue(HPackages.isValidPackageName("com.Example.App"))
    }

    @Test
    fun `a name that carries a command is not a package name`() {
        assertFalse(HPackages.isValidPackageName("com.x; id > /data/data/com.x"))
        assertFalse(HPackages.isValidPackageName("com.x && id"))
        assertFalse(HPackages.isValidPackageName("com.x`id`"))
        assertFalse(HPackages.isValidPackageName("com.x\$(id)"))
        assertFalse(HPackages.isValidPackageName("com.x id"))
        assertFalse(HPackages.isValidPackageName("com.x\nid"))
        assertFalse(HPackages.isValidPackageName("com"))
        assertFalse(HPackages.isValidPackageName(""))
    }

    @Test
    fun `a package name is one word for the shell`() {
        assertEquals("'com.example.app'", HPackages.shellArg("com.example.app"))
        assertNull(HPackages.shellArg("com.x; id > /data/data/com.x"))
    }

    @Test
    fun `an app that is added keeps a name that is a package name`() {
        HailData.addCheckedApp("com.example.app", 0, false)
        assertTrue(HailData.checkedList.any { it.packageName == "com.example.app" })
        HailData.removeCheckedApp("com.example.app", false)
    }

    @Test
    fun `an app that is added is refused a name that is not a package name`() {
        HailData.addCheckedApp("com.x; id > /data/data/com.x", 0, false)
        assertFalse(HailData.checkedList.any { it.packageName.contains("id >") })
    }
}
