package com.aistra.hail.app

import android.content.Context
import androidx.datastore.core.CorruptionException
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.SettingsBackup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class SettingsDataStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.application as HailApp
        HailApp.setAppForTest(app)
        context = app
        File(app.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun `SettingsSerializer round-trips a SettingsBackup`() =
        runBlocking {
            val original =
                SettingsBackup
                    .newBuilder()
                    .setSortBy(HailData.SORT_INSTALL)
                    .setWorkingMode(HailData.MODE_SHIZUKU_STOP)
                    .setAppTheme(HailData.THEME_DARK)
                    .setIconPack(HailData.ACTION_FREEZE_ALL)
                    .setTileAction(HailData.ACTION_LOCK)
                    .setDynamicShortcutAction(HailData.ACTION_NONE)
                    .setFilterUserApps(false)
                    .setFilterSystemApps(true)
                    .setFilterAllApps(false)
                    .setFilterFrozenApps(true)
                    .setFilterUnfrozenApps(false)
                    .setBiometricLogin(true)
                    .setGrayscaleIcon(false)
                    .setCompactIcon(true)
                    .setSynthesizeAdaptiveIcons(true)
                    .setFuzzySearch(true)
                    .setNineKeySearch(true)
                    .setAutoFreezeAfterLock(true)
                    .setSkipWhileCharging(true)
                    .setSkipForegroundApp(false)
                    .setSkipNotifyingApp(true)
                    .setHomeFontSize(18f)
                    .setAutoFreezeDelay(2.5f)
                    .build()

            val output = ByteArrayOutputStream()
            SettingsSerializer.writeTo(original, output)

            val input = ByteArrayInputStream(output.toByteArray())
            val restored = SettingsSerializer.readFrom(input)

            assertEquals(original, restored)
        }

    @Test
    fun `SettingsSerializer throws CorruptionException on corrupt input`() =
        runBlocking {
            val valid =
                SettingsBackup
                    .newBuilder()
                    .setSortBy("test")
                    .build()
                    .toByteArray()
            val corrupted = valid.copyOf(valid.size)
            corrupted[corrupted.size / 2] = (corrupted[corrupted.size / 2].toInt() xor 0xFF).toByte()

            val input = ByteArrayInputStream(corrupted)
            assertThrows(CorruptionException::class.java) {
                SettingsSerializer.readFrom(input)
            }
        }

    @Test
    fun `toSettingsData uses present field values and defaults for absent fields`() {
        val backup =
            SettingsBackup
                .newBuilder()
                .setSortBy(HailData.SORT_INSTALL)
                .setFilterUserApps(false)
                .setHomeFontSize(20f)
                .build()

        val data = backup.toSettingsData()

        assertEquals(HailData.SORT_INSTALL, data.sortBy)
        assertFalse(data.filterUserApps)
        assertEquals(20f, data.homeFontSize, 0.001f)
        assertEquals(HailData.MODE_DEFAULT, data.workingMode)
        assertTrue(data.filterSystemApps)
        assertEquals(HailData.FOLLOW_SYSTEM, data.appTheme)
        assertEquals(HailData.ACTION_NONE, data.iconPack)
        assertEquals(HailData.AUTO_FREEZE_AFTER_LOCK, data.tileAction)
        assertEquals(HailData.ACTION_NONE, data.dynamicShortcutAction)
    }

    @Test
    fun `toSettingsBackup sets all fields from SettingsData`() {
        val data =
            SettingsData(
                sortBy = HailData.SORT_INSTALL,
                filterUserApps = false,
                filterSystemApps = true,
                filterAllApps = false,
                filterFrozenApps = false,
                filterUnfrozenApps = false,
                workingMode = HailData.MODE_SHIZUKU_HIDE,
                biometricLogin = true,
                appTheme = HailData.THEME_LIGHT,
                iconPack = HailData.ACTION_FREEZE_ALL,
                grayscaleIcon = false,
                compactIcon = true,
                synthesizeAdaptiveIcons = true,
                homeFontSize = 22f,
                fuzzySearch = true,
                nineKeySearch = true,
                tileAction = HailData.ACTION_UNFREEZE_ALL,
                autoFreezeAfterLock = true,
                autoFreezeDelay = 3.5f,
                skipWhileCharging = true,
                skipForegroundApp = true,
                skipNotifyingApp = true,
                dynamicShortcutAction = HailData.ACTION_LOCK_FREEZE,
            )

        val backup = data.toSettingsBackup()

        assertTrue(backup.hasSortBy())
        assertEquals(HailData.SORT_INSTALL, backup.sortBy)
        assertTrue(backup.hasFilterUserApps())
        assertFalse(backup.filterUserApps)
        assertTrue(backup.hasFilterSystemApps())
        assertTrue(backup.filterSystemApps)
        assertTrue(backup.hasWorkingMode())
        assertEquals(HailData.MODE_SHIZUKU_HIDE, backup.workingMode)
        assertTrue(backup.hasBiometricLogin())
        assertTrue(backup.biometricLogin)
        assertTrue(backup.hasAppTheme())
        assertEquals(HailData.THEME_LIGHT, backup.appTheme)
        assertTrue(backup.hasIconPack())
        assertEquals(HailData.ACTION_FREEZE_ALL, backup.iconPack)
        assertTrue(backup.hasHomeFontSize())
        assertEquals(22f, backup.homeFontSize, 0.001f)
        assertTrue(backup.hasAutoFreezeDelay())
        assertEquals(3.5f, backup.autoFreezeDelay, 0.001f)
    }

    @Test
    fun `migrateSettingsFromSharedPreferences migrates string boolean float values`() =
        runBlocking {
            val sp = context.getSharedPreferences("hail_preferences", Context.MODE_PRIVATE)
            sp
                .edit()
                .putString(HailData.SORT_BY, HailData.SORT_INSTALL)
                .putBoolean(HailData.FILTER_USER_APPS, false)
                .putFloat(HailData.HOME_FONT_SIZE, 18f)
                .apply()

            migrateSettingsFromSharedPreferences()

            val settings = SettingsDataStore.readSettings()

            assertEquals(HailData.SORT_INSTALL, settings.sortBy)
            assertFalse(settings.filterUserApps)
            assertEquals(18f, settings.homeFontSize, 0.001f)
        }
}
