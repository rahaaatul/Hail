package com.aistra.hail.extensions

import android.view.View
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewExtensionsTest {
    @Test
    fun `isRtl is true when layout direction is RTL`() {
        val view = mockk<View>(relaxed = true)
        every { view.layoutDirection } returns View.LAYOUT_DIRECTION_RTL
        assertTrue(view.isRtl)
    }

    @Test
    fun `isRtl is false when layout direction is LTR`() {
        val view = mockk<View>(relaxed = true)
        every { view.layoutDirection } returns View.LAYOUT_DIRECTION_LTR
        assertFalse(view.isRtl)
    }
}
