package com.aistra.hail.app

import androidx.room3.Room
import com.aistra.hail.HailApp
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.AppMetadataDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class HailDataTest {
    private lateinit var db: AppMetadataDatabase

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as HailApp
        HailApp.setAppForTest(app)
        db =
            Room
                .inMemoryDatabaseBuilder(app, AppMetadataDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        AppMetaCache.useDatabaseForTest(db)
        HailDataStore.resetForTest()
        SettingsDataStore.resetForTest()
        HailSettings.resetSettingsForTest()
    }

    @After
    fun tearDown() {
        AppMetaCache.useDatabaseForTest(null)
        db.close()
    }

    @Test
    fun `working mode default is correct`() {
        assertEquals("default", HailData.MODE_DEFAULT)
    }

    @Test
    fun `working mode shizuku hide is correct`() {
        assertEquals("shizuku_hide", HailData.MODE_SHIZUKU_HIDE)
    }

    @Test
    fun `working mode island hide is correct`() {
        assertEquals("island_hide", HailData.MODE_ISLAND_HIDE)
    }

    @Test
    fun `MODE constants combine prefix and suffix correctly`() {
        assertEquals("shizuku_stop", HailData.MODE_SHIZUKU_STOP)
        assertEquals("su_disable", HailData.MODE_SU_DISABLE)
        assertEquals("owner_suspend", HailData.MODE_OWNER_SUSPEND)
        assertEquals("privapp_stop", HailData.MODE_PRIVAPP_STOP)
    }

    @Test
    fun `WORKING_MODE_VALUES contains all expected modes`() {
        assertTrue(HailData.WORKING_MODE_VALUES.contains(HailData.MODE_DEFAULT))
        assertTrue(HailData.WORKING_MODE_VALUES.contains(HailData.MODE_SHIZUKU_STOP))
        assertTrue(HailData.WORKING_MODE_VALUES.contains(HailData.MODE_OWNER_HIDE))
    }

    @Test
    fun `SORT constants are correct`() {
        assertEquals("name", HailData.SORT_NAME)
        assertEquals("install", HailData.SORT_INSTALL)
        assertEquals("update", HailData.SORT_UPDATE)
    }

    @Test
    fun `FILTER constants are correct`() {
        assertEquals("filter_user_apps", HailData.FILTER_USER_APPS)
        assertEquals("filter_system_apps", HailData.FILTER_SYSTEM_APPS)
        assertEquals("filter_enabled_apps", HailData.FILTER_ENABLED_APPS)
        assertEquals("filter_disabled_apps", HailData.FILTER_DISABLED_APPS)
        assertEquals("filter_suspended_apps", HailData.FILTER_SUSPENDED_APPS)
        assertEquals("filter_hidden_apps", HailData.FILTER_HIDDEN_APPS)
        assertEquals("filter_stopped_apps", HailData.FILTER_STOPPED_APPS)
        assertEquals("filter_uninstalled_apps", HailData.FILTER_UNINSTALLED_APPS)
    }

    @Test
    fun `KEY constants are correct`() {
        assertEquals("package", HailData.KEY_PACKAGE)
        assertEquals("tag", HailData.KEY_TAG)
        assertEquals("frozen", HailData.KEY_FROZEN)
    }

    @Test
    fun `add and remove checked apps go through room`() =
        runBlocking {
            assertFalse(HailData.isChecked("com.example.pkg"))

            HailData.addCheckedApp("com.example.pkg", tagId = 2)
            HailData.flush()

            assertTrue(HailData.isChecked("com.example.pkg"))
            assertEquals(1, db.checkedAppDao().loadAll().size)
            assertEquals(
                "2",
                db
                    .checkedAppDao()
                    .loadAll()
                    .single()
                    .tagIds,
            )

            HailData.removeCheckedApp("com.example.pkg")
            HailData.flush()

            assertFalse(HailData.isChecked("com.example.pkg"))
            assertTrue(db.checkedAppDao().loadAll().isEmpty())
        }

    @Test
    fun `hails own package is never checked`() =
        runBlocking {
            val own = com.aistra.hail.BuildConfig.APPLICATION_ID
            HailData.addCheckedApp(own)
            HailData.flush()

            assertFalse(HailData.isChecked(own))
            assertTrue(db.checkedAppDao().loadAll().isEmpty())
        }

    @Test
    fun `tags are persisted to room`() =
        runBlocking {
            HailData.tags.clear()
            HailData.tags.add("Work" to 1)
            HailData.saveTags()
            HailData.flush()

            assertEquals(listOf(1), db.tagDao().loadAll().map { it.id })
            assertEquals(listOf("Work"), db.tagDao().loadAll().map { it.name })
        }

    @Test
    fun `optimistic boolean setters update the cache synchronously`() {
        HailSettings.autoFreezeAfterLock = true
        assertTrue(HailSettings.autoFreezeAfterLock)
        assertTrue(HailSettings.settingsData.value.autoFreezeAfterLock)

        HailSettings.grayscaleIcon = false
        assertFalse(HailSettings.grayscaleIcon)
        assertFalse(HailSettings.settingsData.value.grayscaleIcon)
    }

    @Test
    fun `optimistic string setters update the cache synchronously`() {
        HailSettings.appTheme = HailData.THEME_DARK
        assertEquals(HailData.THEME_DARK, HailSettings.appTheme)
        assertEquals(HailData.THEME_DARK, HailSettings.settingsData.value.appTheme)

        HailSettings.tileAction = HailData.ACTION_LOCK
        assertEquals(HailData.ACTION_LOCK, HailSettings.tileAction)
        assertEquals(HailData.ACTION_LOCK, HailSettings.settingsData.value.tileAction)
    }

    @Test
    fun `optimistic float setters update the cache synchronously`() {
        HailSettings.homeFontSize = 20f
        assertEquals(20f, HailSettings.homeFontSize, 0.001f)
        assertEquals(20f, HailSettings.settingsData.value.homeFontSize, 0.001f)

        HailSettings.autoFreezeDelay = 1.5f
        assertEquals(1.5f, HailSettings.autoFreezeDelay, 0.001f)
        assertEquals(1.5f, HailSettings.settingsData.value.autoFreezeDelay, 0.001f)
    }

    @Test
    fun `seedSettings replaces the synchronous cache`() {
        HailSettings.seedSettings(
            SettingsData(
                sortBy = HailData.SORT_SIZE,
                workingMode = HailData.MODE_ISLAND_HIDE,
                homeFontSize = 22f,
                settingsMigratedFromSp = true,
            ),
        )

        assertEquals(HailData.SORT_SIZE, HailSettings.sortBy)
        assertEquals(HailData.MODE_ISLAND_HIDE, HailSettings.workingMode)
        assertEquals(22f, HailSettings.homeFontSize, 0.001f)
        assertTrue(HailSettings.settingsData.value.settingsMigratedFromSp)
    }

    @Test
    fun `snapshot lists are copy on write and stay mutable`() =
        runBlocking {
            HailData.addCheckedApp("com.example.pkg", tagId = 1)
            HailData.tags.clear()
            HailData.tags.add("Work" to 1)

            assertTrue(HailData.checkedList is CopyOnWriteArrayList<*>)
            assertTrue(HailData.tags is CopyOnWriteArrayList<*>)

            // The UI keeps mutating the live lists; add/remove must still work and persist.
            HailData.checkedList.add(AppInfo("com.example.extra"))
            HailData.removeCheckedApp("com.example.pkg")
            HailData.flush()

            assertEquals(listOf("com.example.extra"), HailData.checkedList.map { it.packageName })
            assertEquals(listOf("Work" to 1), HailData.tags)
            assertEquals(listOf("com.example.extra"), db.checkedAppDao().loadAll().map { it.packageName })
        }
}
