package com.aistra.hail.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import com.aistra.hail.HailApp
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies the reinstall icon fix: the fallback/default icon returned when an APK is missing must
 * never be cached, so a later load against a real APK re-reads it. It also pins the reload paths:
 * [AppIconCache.invalidate] only drops the memory entry, so a reload needs a changed disk key
 * (sourceDir/APK modification time), while the disk key already carries that signature.
 *
 * [AppIconCache.loadBitmap] is the internal seam swapped for a counting fake, so the caching
 * contract is exercised without a real AppIconLoader or PackageManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class AppIconCacheTest {
    private lateinit var context: Context
    private var fakeLoader: FakeIconLoader? = null

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as HailApp
        HailApp.setAppForTest(app)
        context = app
        AppIconCache.clear()
        fakeLoader = FakeIconLoader()
        mockkObject(AppIconCache)
        every { AppIconCache.loadBitmap(any(), any(), any()) } answers { fakeLoader!!.load() }
    }

    @After
    fun tearDown() {
        AppIconCache.clear()
        unmockkAll()
    }

    private fun apkFile(): File =
        File(context.filesDir, "apk-${System.nanoTime()}/base.apk").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
            check(exists())
        }

    private fun info(
        packageName: String,
        sourceDir: String?,
    ): ApplicationInfo =
        ApplicationInfo().apply {
            this.packageName = packageName
            this.sourceDir = sourceDir
            flags = ApplicationInfo.FLAG_INSTALLED
            enabled = true
        }

    private class FakeIconLoader {
        var loadCount = 0
            private set

        fun load(): Bitmap {
            loadCount++
            return Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(loadCount) }
        }
    }

    @Test
    fun `missing APK source fallback is not cached`() =
        runBlocking {
            // Robolectric's ApplicationInfo shadow fills sourceDir with a real package path, so the
            // missing-APK case is expressed as a path that does not exist (the production case for
            // a data-only record or an APK removed while the app was stopped).
            val loader = fakeLoader!!
            val info =
                info(
                    "com.example.pkg",
                    sourceDir = File(context.filesDir, "gone/base.apk").absolutePath,
                )

            AppIconCache.getOrLoadBitmap(context, info, 0, 8)
            AppIconCache.getOrLoadBitmap(context, info, 0, 8)

            // Both calls reached the loader: the fallback was never cached in memory.
            assertEquals(2, loader.loadCount)
        }

    @Test
    fun `later load with a valid sourceDir reloads`() =
        runBlocking {
            val loader = fakeLoader!!
            val stale = info("com.example.pkg", sourceDir = "/missing/base.apk")
            val fresh = info("com.example.pkg", sourceDir = apkFile().absolutePath)

            val first = AppIconCache.getOrLoadBitmap(context, stale, 0, 8)
            val second = AppIconCache.getOrLoadBitmap(context, fresh, 0, 8)

            // The fallback from the stale load was not cached, so the real source reloads.
            assertNotEquals(first, second)
            assertEquals(2, loader.loadCount)
        }

    @Test
    fun `invalidate forces a reload when the source signature changes`() =
        runBlocking {
            val loader = fakeLoader!!
            val apk = apkFile()
            val info = info("com.example.pkg", sourceDir = apk.absolutePath)

            val first = AppIconCache.getOrLoadBitmap(context, info, 0, 8)
            val cached = AppIconCache.getOrLoadBitmap(context, info, 0, 8)
            // The second call is served from the memory cache.
            assertEquals(1, loader.loadCount)
            assertSame(first, cached)

            // invalidate() only drops the memory entry; the disk key (sourceDir:APK mtime) still
            // matches, so the same on-disk bitmap would be served. A reinstall changes the APK
            // modification time, which changes the disk key and forces the loader to run.
            AppIconCache.invalidate("com.example.pkg")

            check(apk.setLastModified(apk.lastModified() + 10_000))
            val reloaded = AppIconCache.getOrLoadBitmap(context, info, 0, 8)
            assertEquals(2, loader.loadCount)
            assertNotSame(first, reloaded)
        }
}
