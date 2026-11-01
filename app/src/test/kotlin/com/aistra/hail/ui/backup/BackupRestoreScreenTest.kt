package com.aistra.hail.ui.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistra.hail.HailApp
import com.aistra.hail.backup.BackupCategory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class BackupRestoreScreenTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        // Use HailApp as the application context to have access to resources
        context = ApplicationProvider.getApplicationContext<HailApp>()
    }

    @Test
    fun `APPS maps to backup_category_apps string resource`() {
        val resId = BackupCategory.APPS.stringResId
        val expected = com.aistra.hail.R.string.backup_category_apps
        assertEquals(expected, resId)
    }

    @Test
    fun `TAGS maps to backup_category_tags string resource`() {
        val resId = BackupCategory.TAGS.stringResId
        val expected = com.aistra.hail.R.string.backup_category_tags
        assertEquals(expected, resId)
    }

    @Test
    fun `ACTIONS maps to backup_category_actions string resource`() {
        val resId = BackupCategory.ACTIONS.stringResId
        val expected = com.aistra.hail.R.string.backup_category_actions
        assertEquals(expected, resId)
    }

    @Test
    fun `SETTINGS maps to backup_category_settings string resource`() {
        val resId = BackupCategory.SETTINGS.stringResId
        val expected = com.aistra.hail.R.string.backup_category_settings
        assertEquals(expected, resId)
    }

    @Test
    fun `WHITELIST maps to backup_category_whitelist string resource`() {
        val resId = BackupCategory.WHITELIST.stringResId
        val expected = com.aistra.hail.R.string.backup_category_whitelist
        assertEquals(expected, resId)
    }
}
