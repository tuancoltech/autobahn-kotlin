package io.crossbar.autobahn.wamp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.ClassCastException

class ShortcutsTest {

    // --- Happy paths ---

    @Test
    fun getOrDefault_keyPresent_returnsMappedValue() {
        val m = hashMapOf<Any?, Any?>("name" to "alice")
        val out: String = Shortcuts.getOrDefault(m, "name", "default")
        assertEquals("alice", out)
    }

    @Test
    fun getOrDefault_keyAbsent_returnsDefault() {
        val m = hashMapOf<Any?, Any?>("name" to "alice")
        val out: String = Shortcuts.getOrDefault(m, "missing", "default")
        assertEquals("default", out)
    }

    // --- Null handling ---

    @Test
    fun getOrDefault_valueIsNull_returnsNullEvenIfDefaultNonNull() {
        val m = hashMapOf<Any?, Any?>("k" to null)
        val out: String? = Shortcuts.getOrDefault(m, "k", "fallback")
        assertNull(out)
    }

    @Test
    fun getOrDefault_defaultIsNull_returnsNullWhenAbsent() {
        val m = hashMapOf<Any?, Any?>("k" to 1)
        val out: String? = Shortcuts.getOrDefault(m, "missing", null)
        assertNull(out)
    }

    @Test
    fun getOrDefault_nullKey_presentInMap_returnsValue() {
        val m = hashMapOf<Any?, Any?>(null to 123)
        val out: Int = Shortcuts.getOrDefault(m, null, -1)
        assertEquals(123, out)
    }

    @Test
    fun getOrDefault_nullKey_notInMap_returnsDefault() {
        val m = hashMapOf<Any?, Any?>("x" to 1)
        val out: Int = Shortcuts.getOrDefault(m, null, 42)
        assertEquals(42, out)
    }

    // --- Type safety / cast behavior (unchecked cast inside the method) ---

    @Test(expected = ClassCastException::class)
    fun getOrDefault_typeMismatch_throwsClassCastException() {
        val m = hashMapOf<Any?, Any?>("k" to 123) // value is Int
        // Ask for String -> internal cast (T) obj.get(key) should throw
        val n = Shortcuts.getOrDefault(m, "k", "default")
    }

    // --- Null map (caller error) ---

    @Test(expected = NullPointerException::class)
    fun getOrDefault_nullMap_throwsNPE() {
        val m: Map<*, *>? = null
        // containsKey on null -> NPE
        Shortcuts.getOrDefault(m, "k", "default")
    }

    // --- Multiple value types still work (erasure) ---

    @Test
    fun getOrDefault_differentValueType_stillReturnsDefaultOfT() {
        val m = hashMapOf<Any?, Any?>("k" to 1.23) // Double present under 'k'
        // Ask for Int for a missing key -> should return default Int, not cast the Double
        val out: Int = Shortcuts.getOrDefault(m, "missing", 77)
        assertEquals(77, out)
    }

}