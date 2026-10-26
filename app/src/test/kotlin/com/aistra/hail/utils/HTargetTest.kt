package com.aistra.hail.utils

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class HTargetTest {
    @Test
    fun `flags match SDK version comparisons`() {
        val sdk = Build.VERSION.SDK_INT
        assertEquals(sdk >= Build.VERSION_CODES.N, HTarget.N)
        assertEquals(sdk >= Build.VERSION_CODES.O, HTarget.O)
        assertEquals(sdk >= Build.VERSION_CODES.P, HTarget.P)
        assertEquals(sdk >= Build.VERSION_CODES.Q, HTarget.Q)
        assertEquals(sdk >= Build.VERSION_CODES.S, HTarget.S)
        assertEquals(sdk >= Build.VERSION_CODES.TIRAMISU, HTarget.T)
    }

    @Test
    fun `all flags are boolean`() {
        assertEquals(true, HTarget.N is Boolean)
        assertEquals(true, HTarget.O is Boolean)
        assertEquals(true, HTarget.P is Boolean)
        assertEquals(true, HTarget.Q is Boolean)
        assertEquals(true, HTarget.S is Boolean)
        assertEquals(true, HTarget.T is Boolean)
    }
}
