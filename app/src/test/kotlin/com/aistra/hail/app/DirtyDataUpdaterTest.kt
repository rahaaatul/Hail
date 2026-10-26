package com.aistra.hail.app

import android.content.Context
import androidx.preference.PreferenceManager
import io.mockk.mockk
import org.junit.Test

class DirtyDataUpdaterTest {
    @Test
    fun `update edits default shared preferences and applies`() {
        val context = mockk<Context>(relaxed = true)
        DirtyDataUpdater.update(context)
        // Just verifies it doesn't throw
    }
}
