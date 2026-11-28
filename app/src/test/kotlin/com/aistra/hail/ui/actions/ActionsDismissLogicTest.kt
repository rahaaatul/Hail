package com.aistra.hail.ui.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the dismiss policy: create keeps the draft selection (both picker states),
 * edit clears it.
 */
class ActionsDismissLogicTest {
    @Test
    fun `create with picker closed keeps draft`() {
        assertFalse(shouldClearOnDismiss(isCreate = true, pickerOpen = false))
    }

    @Test
    fun `create with picker open keeps draft`() {
        assertFalse(shouldClearOnDismiss(isCreate = true, pickerOpen = true))
    }

    @Test
    fun `edit with picker closed clears`() {
        assertTrue(shouldClearOnDismiss(isCreate = false, pickerOpen = false))
    }

    @Test
    fun `edit with picker open clears`() {
        assertTrue(shouldClearOnDismiss(isCreate = false, pickerOpen = true))
    }

    @Test
    fun `shouldCommitDraftOnDismiss create with picker open true`() {
        assertTrue(shouldCommitDraftOnDismiss(isCreate = true, pickerOpen = true))
    }

    @Test
    fun `shouldCommitDraftOnDismiss create with picker closed false`() {
        assertFalse(shouldCommitDraftOnDismiss(isCreate = true, pickerOpen = false))
    }

    @Test
    fun `shouldCommitDraftOnDismiss edit with picker open false`() {
        assertFalse(shouldCommitDraftOnDismiss(isCreate = false, pickerOpen = true))
    }

    @Test
    fun `shouldCommitDraftOnDismiss edit with picker closed false`() {
        assertFalse(shouldCommitDraftOnDismiss(isCreate = false, pickerOpen = false))
    }

    @Test
    fun `seedDraftForPicker non-empty draft wins`() {
        assertEquals(setOf("a", "b"), seedDraftForPicker(setOf("a", "b"), listOf("c")))
    }

    @Test
    fun `seedDraftForPicker empty draft uses committed`() {
        assertEquals(setOf("c", "d"), seedDraftForPicker(emptySet(), listOf("c", "d")))
    }

    @Test
    fun `seedDraftForPicker both empty returns empty`() {
        assertEquals(emptySet<String>(), seedDraftForPicker(emptySet(), emptyList()))
    }
}
