package com.aistra.hail.extensions

import android.content.Context
import android.content.res.Configuration
import android.view.View
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextExtensionsTest {
    @Test
    fun `isLandscape is true when orientation is landscape`() {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<android.content.res.Resources>(relaxed = true)
        val config = Configuration()
        config.orientation = Configuration.ORIENTATION_LANDSCAPE
        every { resources.configuration } returns config
        every { context.resources } returns resources
        assertTrue(context.isLandscape)
    }

    @Test
    fun `isLandscape is false when orientation is portrait`() {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<android.content.res.Resources>(relaxed = true)
        val config = Configuration()
        config.orientation = Configuration.ORIENTATION_PORTRAIT
        every { resources.configuration } returns config
        every { context.resources } returns resources
        assertFalse(context.isLandscape)
    }

    @Test
    fun `isRtl is false when layout direction is LTR (default)`() {
        val context = mockk<Context>(relaxed = true)
        val resources = mockk<android.content.res.Resources>(relaxed = true)
        val config = mockk<Configuration>(relaxed = true)
        every { config.layoutDirection } returns android.view.View.LAYOUT_DIRECTION_LTR
        every { resources.configuration } returns config
        every { context.resources } returns resources
        assertFalse(context.isRtl)
    }
}
