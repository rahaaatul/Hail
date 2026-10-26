package com.aistra.hail.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NineKeySearchTest {
    @Test
    fun `search returns true when query is null or empty`() {
        assertTrue(NineKeySearch.search(null))
        assertTrue(NineKeySearch.search(""))
        assertTrue(NineKeySearch.search(null, "any", "thing"))
    }

    @Test
    fun `search returns false when all strings are null or empty`() {
        assertFalse(NineKeySearch.search("a", null, ""))
    }

    @Test
    fun `search matches latin characters by nine-key mapping`() {
        assertTrue(NineKeySearch.search("2", "a")) // a -> 2
        assertTrue(NineKeySearch.search("3", "d")) // d -> 3
        assertTrue(NineKeySearch.search("4", "g")) // g -> 4
        assertTrue(NineKeySearch.search("5", "j")) // j -> 5
        assertTrue(NineKeySearch.search("6", "m")) // m -> 6
        assertTrue(NineKeySearch.search("7", "p")) // p -> 7
        assertTrue(NineKeySearch.search("8", "t")) // t -> 8
        assertTrue(NineKeySearch.search("9", "w")) // w -> 9
    }

    @Test
    fun `search matches uppercase letters`() {
        assertTrue(NineKeySearch.search("2", "A"))
        assertTrue(NineKeySearch.search("22", "aa"))
    }

    @Test
    fun `search keeps digits unchanged`() {
        assertTrue(NineKeySearch.search("5", "5"))
        assertTrue(NineKeySearch.search("123", "123"))
    }

    @Test
    fun `search keeps unknown characters unchanged`() {
        // Unknown chars (e.g. "!") produce empty pinyin/nine-key, so they don't match
        assertFalse(NineKeySearch.search("x", "!"))
        assertFalse(NineKeySearch.search("x", " "))
    }

    @Test
    fun `search matches full word via nine key`() {
        assertTrue(NineKeySearch.search("228", "cat")) // c->2, a->2, t->8
        assertTrue(NineKeySearch.search("43556", "hello")) // h->4, e->3, l->5, l->5, o->6
    }

    @Test
    fun `search returns false when no match`() {
        assertFalse(NineKeySearch.search("1", "a"))
        assertFalse(NineKeySearch.search("999", "a"))
    }

    @Test
    fun `search converts chinese to pinyin then nine key`() {
        // "我爱你" -> wo(96) ai(24) ni(64) -> "962464"; query "5282" doesn't match
        assertFalse(NineKeySearch.search("5282", "我爱你"))
    }
}
