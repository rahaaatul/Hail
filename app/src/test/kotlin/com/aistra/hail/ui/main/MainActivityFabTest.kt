package com.aistra.hail.ui.main

import android.os.Looper
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
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

                assertEquals("Add apps", fab.text.toString())
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

                assertEquals("Add action", fab.text.toString())
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

    private fun MainActivity.selectDestination(destinationId: Int) {
        val navigation: NavigationBarView = findViewById(R.id.bottom_nav)
            ?: findViewById(R.id.nav_rail)
        navigation.selectedItemId = destinationId
    }
}