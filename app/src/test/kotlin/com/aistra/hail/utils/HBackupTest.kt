package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HBackupTest {
    @Test
    fun `isMagnitudeTooWide returns false for normal doubles`() {
        assertFalse(isMagnitudeTooWide(0.0))
        assertFalse(isMagnitudeTooWide(1.5))
        assertFalse(isMagnitudeTooWide(-1.5))
        assertFalse(isMagnitudeTooWide(100.0))
    }

    @Test
    fun `isMagnitudeTooWide returns true for infinite and NaN`() {
        assertTrue(isMagnitudeTooWide(Double.POSITIVE_INFINITY))
        assertTrue(isMagnitudeTooWide(Double.NEGATIVE_INFINITY))
        assertTrue(isMagnitudeTooWide(Double.NaN))
    }

    @Test
    fun `isMagnitudeTooWide returns true for values beyond 2^53`() {
        assertTrue(isMagnitudeTooWide(9.007199254740994E15)) // 2^53 + 2
        assertTrue(isMagnitudeTooWide(-9.007199254740994E15))
    }

    @Test
    fun `isTooWideInteger returns false for exact integers in range`() {
        assertFalse(isTooWideInteger(0.0))
        assertFalse(isTooWideInteger(1.0))
        assertFalse(isTooWideInteger(-1.0))
        assertFalse(isTooWideInteger(9.007199254740992E15)) // 2^53 exactly
    }

    @Test
    fun `isTooWideInteger returns true for non-integer doubles`() {
        assertTrue(isTooWideInteger(1.5))
        assertTrue(isTooWideInteger(-1.5))
    }

    @Test
    fun `isTooWideInteger returns true for integers beyond 2^53`() {
        assertTrue(isTooWideInteger(9.007199254740994E15))
        assertTrue(isTooWideInteger(-9.007199254740994E15))
    }

    @Test
    fun `toExactLongOrNull returns null for non-integer doubles`() {
        assertEquals(null, toExactLongOrNull(1.5))
        assertEquals(null, toExactLongOrNull(-1.5))
    }

    @Test
    fun `toExactLongOrNull returns null for out-of-range integers`() {
        assertEquals(null, toExactLongOrNull(9.007199254740994E15))
        assertEquals(null, toExactLongOrNull(-9.007199254740994E15))
    }

    @Test
    fun `toExactLongOrNull returns long for exact integers in range`() {
        assertEquals(0L, toExactLongOrNull(0.0))
        assertEquals(1L, toExactLongOrNull(1.0))
        assertEquals(-1L, toExactLongOrNull(-1.0))
        assertEquals(9007199254740992L, toExactLongOrNull(9.007199254740992E15))
    }
}
