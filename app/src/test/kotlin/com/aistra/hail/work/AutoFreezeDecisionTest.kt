package com.aistra.hail.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the auto-freeze skip policy: a positive delay is a "leave me alone" window that cancels a
 * lock-triggered run while the screen is on, and the run is skipped entirely when the feature or
 * the user's charging preference says so.
 */
class AutoFreezeDecisionTest {
    @Test
    fun `zero delay with interactive screen still freezes`() {
        assertFalse(
            AutoFreezeDecision.shouldSkip(
                actionLock = true,
                autoFreezeAfterLock = true,
                delayMinutes = 0f,
                isInteractive = true,
                skipWhileCharging = false,
            ),
        )
    }

    @Test
    fun `positive delay with interactive screen skips`() {
        assertTrue(
            AutoFreezeDecision.shouldSkip(
                actionLock = true,
                autoFreezeAfterLock = true,
                delayMinutes = 5f,
                isInteractive = true,
                skipWhileCharging = false,
            ),
        )
    }

    @Test
    fun `positive delay with screen off freezes`() {
        assertFalse(
            AutoFreezeDecision.shouldSkip(
                actionLock = true,
                autoFreezeAfterLock = true,
                delayMinutes = 5f,
                isInteractive = false,
                skipWhileCharging = false,
            ),
        )
    }

    @Test
    fun `auto freeze after lock disabled skips`() {
        assertTrue(
            AutoFreezeDecision.shouldSkip(
                actionLock = true,
                autoFreezeAfterLock = false,
                delayMinutes = 0f,
                isInteractive = false,
                skipWhileCharging = false,
            ),
        )
    }

    @Test
    fun `screen on manual trigger freezes`() {
        assertFalse(
            AutoFreezeDecision.shouldSkip(
                actionLock = false,
                autoFreezeAfterLock = true,
                delayMinutes = 5f,
                isInteractive = true,
                skipWhileCharging = false,
            ),
        )
    }

    @Test
    fun `skip while charging skips`() {
        assertTrue(
            AutoFreezeDecision.shouldSkip(
                actionLock = false,
                autoFreezeAfterLock = true,
                delayMinutes = 0f,
                isInteractive = false,
                skipWhileCharging = true,
            ),
        )
    }
}
