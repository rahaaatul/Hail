package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchActionTest {
    @Test
    fun `data class copies and equals correctly`() {
        val original = LaunchAction("id1", "com.example.launch", listOf("dep1", "dep2"))
        assertEquals("id1", original.id)
        assertEquals("com.example.launch", original.launchPackage)
        assertEquals(listOf("dep1", "dep2"), original.unfreezePackages)
        assertEquals(original, LaunchAction("id1", "com.example.launch", listOf("dep1", "dep2")))
    }

    @Test
    fun `copy creates modified instance`() {
        val original = LaunchAction("id1", "com.example.launch", emptyList())
        val copied = original.copy(launchPackage = "com.example.other")
        assertEquals("com.example.other", copied.launchPackage)
        assertEquals("id1", copied.id)
    }
}
