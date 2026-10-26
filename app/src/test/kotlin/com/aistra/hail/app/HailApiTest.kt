package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class HailApiTest {
    @Test
    fun `action constants are non-empty and prefixed with applicationId`() {
        assertEquals("${BuildConfig.APPLICATION_ID}.action.LAUNCH", HailApi.ACTION_LAUNCH)
        assertEquals("${BuildConfig.APPLICATION_ID}.action.FREEZE", HailApi.ACTION_FREEZE)
        assertEquals("${BuildConfig.APPLICATION_ID}.action.UNFREEZE", HailApi.ACTION_UNFREEZE)
        assertEquals("${BuildConfig.APPLICATION_ID}.action.FREEZE_ALL", HailApi.ACTION_FREEZE_ALL)
        assertEquals("${BuildConfig.APPLICATION_ID}.action.UNFREEZE_ALL", HailApi.ACTION_UNFREEZE_ALL)
        assertEquals("${BuildConfig.APPLICATION_ID}.action.LOCK", HailApi.ACTION_LOCK)
    }

    @Test
    fun `getIntentForTag creates intent with tag`() {
        val intent = mockk<Intent>(relaxed = true)
        intent.addTag("mytag")
        verify { intent.putExtra(HailData.KEY_TAG, "mytag") }
    }
}
