package com.aistra.hail.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinyinSearchTest {
    @Test
    fun `searchPinyinAll returns true when query is null or empty`() {
        assertTrue(PinyinSearch.searchPinyinAll("anything", null))
        assertTrue(PinyinSearch.searchPinyinAll("anything", ""))
    }

    @Test
    fun `searchPinyinAll returns false when raw is null or empty`() {
        assertFalse(PinyinSearch.searchPinyinAll(null, "query"))
        assertFalse(PinyinSearch.searchPinyinAll("", "query"))
    }

    @Test
    fun `searchPinyinAll returns false when locale is not Chinese`() {
        // When not Chinese locale, searchCap/searchAllSpell short-circuit to false
        assertFalse(PinyinSearch.searchPinyinAll("计算器", "jsq"))
    }

    @Test
    fun `searchCap rejects query longer than 8 chars`() {
        assertFalse(PinyinSearch.searchPinyinAll("abcdefghij", "abcdefghi"))
    }

    @Test
    fun `searchAllSpell rejects query longer than 48 chars`() {
        assertFalse(PinyinSearch.searchPinyinAll("a".repeat(49), "a".repeat(49)))
    }
}
