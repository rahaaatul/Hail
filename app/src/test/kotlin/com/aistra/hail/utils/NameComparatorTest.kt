package com.aistra.hail.utils

import com.aistra.hail.app.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class NameComparatorTest {
    @Test
    fun `compare sorts pinned AppInfo first`() {
        val pinned = AppInfo("pkg.pinned", pinned = true)
        val unpinned = AppInfo("pkg.unpinned", pinned = false)
        assertEquals(-1, NameComparator.compare(pinned, unpinned))
        assertEquals(1, NameComparator.compare(unpinned, pinned))
    }

    @Test
    fun `compare sorts AppInfo by name when both same pinned state`() {
        val first = AppInfo("pkg.b", pinned = false)
        val second = AppInfo("pkg.a", pinned = false)
        assertEquals(1, NameComparator.compare(first, second))
        assertEquals(-1, NameComparator.compare(second, first))
        assertEquals(0, NameComparator.compare(first, first))
    }

    @Test
    fun `compare returns 0 for unrelated types`() {
        assertEquals(0, NameComparator.compare("string", 42))
    }

    @Test
    fun `compare equal AppInfo returns 0`() {
        assertEquals(0, NameComparator.compare(AppInfo("a"), AppInfo("a")))
    }
}
