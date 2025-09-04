package io.crossbar.autobahn.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ToBinaryTest {

    /**
     * toBinary() test cases
     */

    // ---------- Exceptional / edge inputs ----------

    @Test(expected = NullPointerException::class)
    fun toBinary_null_throwsNPE() {
        val s: String? = null
        // s!!.length would NPE in Java method (s.length())
        AuthUtil.toBinary(s)
    }

    @Test
    fun toBinary_empty_returnsEmptyArray() {
        val out = AuthUtil.toBinary("")
        assertEquals(0, out.size)
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    fun toBinary_oddLength_throwsStringIndexOutOfBounds() {
        // Access to s.charAt(i + 1) will fail on last iteration
        AuthUtil.toBinary("ABC") // length 3
    }

    // ---------- Valid hex: lowercase / uppercase / mixed ----------

    @Test
    fun toBinary_lowercase_valid() {
        val out = AuthUtil.toBinary("0a1b")
        assertArrayEquals(byteArrayOf(0x0A, 0x1B), out)
    }

    @Test
    fun toBinary_uppercase_valid() {
        val out = AuthUtil.toBinary("AF10")
        assertArrayEquals(byteArrayOf(0xAF.toByte(), 0x10), out)
    }

    @Test
    fun toBinary_mixedCase_valid() {
        val out = AuthUtil.toBinary("DeAdBeEf")
        assertArrayEquals(
            byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()),
            out
        )
    }

    @Test
    fun toBinary_withLeadingZeros() {
        val out = AuthUtil.toBinary("0001")
        assertArrayEquals(byteArrayOf(0x00, 0x01), out)
    }

    // ---------- Byte boundary values ----------

    @Test
    fun toBinary_byteBoundaries_00_7F_80_FF() {
        val out = AuthUtil.toBinary("007F80FF")
        assertArrayEquals(
            byteArrayOf(
                0x00,
                0x7F,             // 127
                0x80.toByte(),    // -128 in signed byte
                0xFF.toByte()     // -1 in signed byte
            ),
            out
        )
    }

    // ---------- Invalid hex chars: current behavior (no validation) ----------

    @Test
    fun toBinary_invalidBothNibbles_returnsEF() {
        // Character.digit('G', 16) == -1, so result = (-1<<4)+(-1) = -17 -> 0xEF
        val out = AuthUtil.toBinary("GG")
        assertArrayEquals(byteArrayOf(0xEF.toByte()), out)
    }

    @Test
    fun toBinary_invalidFirstNibble_validSecond() {
        // (-1<<4)+9 = -7 -> 0xF9
        val out = AuthUtil.toBinary("G9")
        assertArrayEquals(byteArrayOf(0xF9.toByte()), out)
    }

    @Test
    fun toBinary_validFirstNibble_invalidSecond() {
        // (0<<4)+(-1) = -1 -> 0xFF
        val out = AuthUtil.toBinary("0G")
        assertArrayEquals(byteArrayOf(0xFF.toByte()), out)
    }

    // ---------- Multi-byte round-up sanity ----------

    @Test
    fun toBinary_multiByteSequence() {
        val out = AuthUtil.toBinary("7F00ff12")
        assertArrayEquals(
            byteArrayOf(0x7F, 0x00, 0xFF.toByte(), 0x12),
            out
        )
    }
}