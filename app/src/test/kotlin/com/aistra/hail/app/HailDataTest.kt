package com.aistra.hail.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HailDataTest {
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
        assertEquals("filter_all_apps", HailData.FILTER_ALL_APPS)
        assertEquals("filter_frozen_apps", HailData.FILTER_FROZEN_APPS)
        assertEquals("filter_unfrozen_apps", HailData.FILTER_UNFROZEN_APPS)
    }

    @Test
    fun `KEY constants are correct`() {
        assertEquals("package", HailData.KEY_PACKAGE)
        assertEquals("tag", HailData.KEY_TAG)
        assertEquals("frozen", HailData.KEY_FROZEN)
    }
}
