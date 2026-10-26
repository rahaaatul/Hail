package com.aistra.hail.utils

import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FuzzySearchTest {
    private lateinit var mockApp: HailApp

    @Before
    fun setUp() {
        mockApp = mockk(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)
        mockkObject(HailData)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `search returns true when query is null or empty`() {
        assertTrue(FuzzySearch.search("anything", null))
        assertTrue(FuzzySearch.search("anything", ""))
    }

    @Test
    fun `search returns false when raw is null or empty`() {
        assertFalse(FuzzySearch.search(null, "query"))
        assertFalse(FuzzySearch.search("", "query"))
    }

    @Test
    fun `search returns true when raw contains query case-insensitive`() {
        every { HailData.fuzzySearch } returns false
        assertTrue(FuzzySearch.search("Hello World", "world"))
        assertTrue(FuzzySearch.search("Hello World", "hello"))
    }

    @Test
    fun `search returns false when fuzzySearch disabled and no direct match`() {
        every { HailData.fuzzySearch } returns false
        assertFalse(FuzzySearch.search("abcdef", "xyz"))
    }

    @Test
    fun `search uses levenshtein when fuzzySearch enabled`() {
        every { HailData.fuzzySearch } returns true
        assertTrue(FuzzySearch.search("abcdef", "ace"))
        assertFalse(FuzzySearch.search("aaaaaaaaaa", "bbbbbbbbbb"))
    }

    @Test
    fun `search requires containsInOrder when fuzzy enabled`() {
        every { HailData.fuzzySearch } returns true
        assertFalse(FuzzySearch.search("abc", "cba"))
    }
}
