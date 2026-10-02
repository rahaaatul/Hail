package com.aistra.hail.ui.main

import android.content.Context
import android.os.Looper
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.aistra.hail.R
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class MainActivityFabTest {

    @Test
    @Config(qualifiers = "port")
    fun `portrait home keeps the fab collapsed to its icon`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val fab = activity.fab

                activity.selectDestination(R.id.nav_home)

                assertFalse(fab.isExtended)
            }
        }
    }

    @Test
    @Config(qualifiers = "port")
    fun `portrait actions keeps the fab collapsed to its icon`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val fab = activity.fab

                activity.selectDestination(R.id.nav_actions)

                assertFalse(fab.isExtended)
            }
        }
    }

    @Test
    @Config(qualifiers = "land")
    fun `landscape home extends the fab and labels it add apps`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val fab = activity.fab

                activity.selectDestination(R.id.nav_home)

                assertEquals(activity.getString(R.string.action_add_apps), fab.text.toString())
                assertTrue(fab.isExtended)
            }
        }
    }

    @Test
    @Config(qualifiers = "land")
    fun `landscape actions extends the fab and labels it add action`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val fab = activity.fab

                activity.selectDestination(R.id.nav_actions)

                assertEquals(activity.getString(R.string.action_add_action), fab.text.toString())
                assertTrue(fab.isExtended)
            }
        }
    }

    @Test
    fun `settings tab hides the fab`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val fab = activity.fab

                activity.selectDestination(R.id.nav_settings)

                shadowOf(Looper.getMainLooper()).idle()

                assertFalse(fab.isVisible)
            }
        }
    }

    @Test
    @Config(qualifiers = "port")
    fun `the portrait layout leaves the fab icon to MainActivity`() {
        assertFalse(layoutDeclaresFabIcon())
    }

    @Test
    @Config(qualifiers = "land")
    fun `the landscape layout leaves the fab icon to MainActivity`() {
        assertFalse(layoutDeclaresFabIcon())
    }

    private fun layoutDeclaresFabIcon(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.layout.app_bar_main)
        var declared = false
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (!parser.name.endsWith("FloatingActionButton")) continue
            for (index in 0 until parser.attributeCount) {
                if (parser.getAttributeName(index) == "icon") declared = true
            }
        }
        return declared
    }

    private fun MainActivity.selectDestination(destinationId: Int) {
        val navigation: NavigationBarView = findViewById(R.id.bottom_nav)
            ?: findViewById(R.id.nav_rail)
        navigation.selectedItemId = destinationId
    }
}