package com.aistra.hail.ui.apps

import com.aistra.hail.app.AppInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStatusFilterTest {
    private val all =
        StatusFilter(
            enabled = true,
            disabled = true,
            suspended = true,
            hidden = true,
            stopped = true,
            uninstalled = true,
        )

    @Test
    fun `stopped but enabled app matches the enabled filter`() {
        val filter = StatusFilter(enabled = true, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.STOPPED, enabled = true, filter))
    }

    @Test
    fun `stopped but enabled app matches the stopped filter`() {
        val filter = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = true, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.STOPPED, enabled = true, filter))
    }

    @Test
    fun `disabled app matches the disabled filter`() {
        val filter = StatusFilter(enabled = false, disabled = true, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.DISABLED, enabled = false, filter))
    }

    @Test
    fun `disabled app does not match the enabled filter`() {
        val filter = StatusFilter(enabled = true, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertFalse(matchesStatusFilter(AppInfo.State.DISABLED, enabled = false, filter))
    }

    @Test
    fun `suspended and enabled app matches the enabled filter`() {
        val filter = StatusFilter(enabled = true, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.SUSPENDED, enabled = true, filter))
    }

    @Test
    fun `suspended and enabled app matches the suspended filter`() {
        val filter = StatusFilter(enabled = false, disabled = false, suspended = true, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.SUSPENDED, enabled = true, filter))
    }

    @Test
    fun `hidden and enabled app matches the enabled filter`() {
        val filter = StatusFilter(enabled = true, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.HIDDEN, enabled = true, filter))
    }

    @Test
    fun `hidden and enabled app matches the hidden filter`() {
        val filter = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = true, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.HIDDEN, enabled = true, filter))
    }

    @Test
    fun `uninstalled app matches only the uninstalled filter`() {
        val uninstalled = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = true)
        val enabled = StatusFilter(enabled = true, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)
        val disabled = StatusFilter(enabled = false, disabled = true, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.UNINSTALLED, enabled = true, uninstalled))
        assertFalse(matchesStatusFilter(AppInfo.State.UNINSTALLED, enabled = true, enabled))
        assertFalse(matchesStatusFilter(AppInfo.State.UNINSTALLED, enabled = true, disabled))
    }

    @Test
    fun `not found app matches no filter`() {
        assertFalse(matchesStatusFilter(AppInfo.State.NOT_FOUND, enabled = true, all))
    }

    @Test
    fun `null status matches any filter`() {
        val stopped = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = true, uninstalled = false)
        val none = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(null, enabled = true, stopped))
        assertTrue(matchesStatusFilter(null, enabled = true, none))
    }

    @Test
    fun `unknown status matches any filter`() {
        val stopped = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = true, uninstalled = false)
        val none = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertTrue(matchesStatusFilter(AppInfo.State.UNKNOWN, enabled = true, stopped))
        assertTrue(matchesStatusFilter(AppInfo.State.UNKNOWN, enabled = true, none))
    }

    @Test
    fun `no filter selected hides every known state`() {
        val none = StatusFilter(enabled = false, disabled = false, suspended = false, hidden = false, stopped = false, uninstalled = false)

        assertFalse(matchesStatusFilter(AppInfo.State.ENABLED, enabled = true, none))
        assertFalse(matchesStatusFilter(AppInfo.State.STOPPED, enabled = true, none))
        assertFalse(matchesStatusFilter(AppInfo.State.UNINSTALLED, enabled = false, none))
    }
}
