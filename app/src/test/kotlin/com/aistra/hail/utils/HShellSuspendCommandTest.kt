package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HShellSuspendCommandTest {
    @Test
    fun `suspend command runs as shell user`() {
        assertEquals(
            "su shell -c 'pm suspend --user 0 com.example.app'",
            pmSuspendCommand("com.example.app", true, 0),
        )
    }

    @Test
    fun `unsuspend command runs as shell user`() {
        assertEquals(
            "su shell -c 'pm unsuspend --user 10 com.example.app'",
            pmSuspendCommand("com.example.app", false, 10),
        )
    }

    @Test
    fun `command never invokes pm directly with root`() {
        val command = pmSuspendCommand("com.example.app", true, 0)

        assertFalse(command.startsWith("pm "))
        assertFalse(command.contains("su -c"))
    }
}
