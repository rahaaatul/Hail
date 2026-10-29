package com.aistra.hail.app

import android.content.Context
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailDataPreferenceDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class HailDataPreferenceDataStoreTest {
    private lateinit var context: Context
    private lateinit var dataStore: HailDataPreferenceDataStore

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.application as HailApp
        HailApp.setAppForTest(app)
        context = app
        dataStore = HailDataPreferenceDataStore()
    }

    // ── Boolean ──
    @Test
    fun `getBoolean returns DataStore value for known key`() {
        assertEquals(HailData.filterUserApps, dataStore.getBoolean(HailData.FILTER_USER_APPS, false))
    }

    @Test
    fun `getBoolean returns default for unknown key`() {
        assertEquals(true, dataStore.getBoolean("unknown_boolean_key", true))
        assertEquals(false, dataStore.getBoolean("unknown_boolean_key", false))
    }

    @Test
    fun `putBoolean does not crash for known key`() {
        dataStore.putBoolean(HailData.FILTER_USER_APPS, false)
    }

    @Test
    fun `putBoolean does not crash for unknown key`() {
        dataStore.putBoolean("unknown_boolean_key", true)
    }

    // ── String ──
    @Test
    fun `getString returns DataStore value for known key`() {
        assertEquals(HailData.sortBy, dataStore.getString(HailData.SORT_BY, "default"))
    }

    @Test
    fun `getString returns default for unknown key`() {
        assertEquals("default", dataStore.getString("unknown_string_key", "default"))
    }

    @Test
    fun `putString does not crash for known key`() {
        dataStore.putString(HailData.SORT_BY, HailData.SORT_INSTALL)
    }

    @Test
    fun `putString does not crash for unknown key`() {
        dataStore.putString("unknown_string_key", "value")
    }

    // ── Float ──
    @Test
    fun `getFloat returns DataStore value for known key`() {
        assertEquals(HailData.homeFontSize, dataStore.getFloat(HailData.HOME_FONT_SIZE, 0f), 0.001f)
    }

    @Test
    fun `getFloat returns default for unknown key`() {
        assertEquals(99f, dataStore.getFloat("unknown_float_key", 99f), 0.001f)
    }

    @Test
    fun `putFloat does not crash for known key`() {
        dataStore.putFloat(HailData.HOME_FONT_SIZE, 16f)
    }

    @Test
    fun `putFloat does not crash for unknown key`() {
        dataStore.putFloat("unknown_float_key", 1.5f)
    }

    // ── Int ──
    @Test
    fun `getInt returns default`() {
        assertEquals(7, dataStore.getInt("any_int_key", 7))
    }

    @Test
    fun `putInt is a no-op`() {
        dataStore.putInt("any_int_key", 42)
    }

    // ── Long ──
    @Test
    fun `getLong returns default`() {
        assertEquals(7L, dataStore.getLong("any_long_key", 7L))
    }

    @Test
    fun `putLong is a no-op`() {
        dataStore.putLong("any_long_key", 42L)
    }

    // ── StringSet ──
    @Test
    fun `getStringSet returns default`() {
        assertEquals(setOf("a"), dataStore.getStringSet("any_string_set_key", setOf("a")))
    }

    @Test
    fun `putStringSet is a no-op`() {
        dataStore.putStringSet("any_string_set_key", setOf("a", "b"))
    }
}
