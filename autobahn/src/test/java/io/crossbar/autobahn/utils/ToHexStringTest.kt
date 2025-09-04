package io.crossbar.autobahn.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ToHexStringTest {
    /**
     * toHexString() test cases
     **/

    // ---------- Exceptional / edge inputs ----------

    @Test(expected = NullPointerException::class)
    fun toHexString_null_throwsNPE() {
        val bytes: ByteArray? = null
        AuthUtil.toHexString(bytes)
    }

    @Test
    fun toHexString_empty_returnsEmptyString() {
        val out = AuthUtil.toHexString(byteArrayOf())
        assertEquals("", out)
    }

    // ---------- Single-byte values ----------

    @Test
    fun toHexString_singleZero_padsToTwoHexDigits() {
        val out = AuthUtil.toHexString(byteArrayOf(0x00))
        assertEquals("00", out)
    }

    @Test
    fun toHexString_singleLowNibble_padsLeft() {
        val out = AuthUtil.toHexString(byteArrayOf(0x0F))
        assertEquals("0f", out) // lower-case per "%02x"
    }

    @Test
    fun toHexString_singleHighNibble() {
        val out = AuthUtil.toHexString(byteArrayOf(0x7F))
        assertEquals("7f", out)
    }

    @Test
    fun toHexString_singleSignedByte80() {
        val out = AuthUtil.toHexString(byteArrayOf(0x80.toByte()))
        assertEquals("80", out) // signed byte prints as 0x80
    }

    @Test
    fun toHexString_singleSignedByteFF() {
        val out = AuthUtil.toHexString(byteArrayOf(0xFF.toByte()))
        assertEquals("ff", out)
    }

    // ---------- Multi-byte sequences ----------

    @Test
    fun toHexString_leadingZerosAcrossBytes() {
        val out = AuthUtil.toHexString(byteArrayOf(0x00, 0x01))
        assertEquals("0001", out)
    }

    @Test
    fun toHexString_mixedValues_lowercaseAndConcatenated() {
        // 0xDE 0xAD 0xBE 0xEF -> "deadbeef" (lowercase)
        val out = AuthUtil.toHexString(
            byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        )
        assertEquals("deadbeef", out)
    }

    @Test
    fun toHexString_variedSignedAndUnsignedBytes() {
        // [0x00, 0x10, 0x7f, 0x80, 0x9a, 0xff] -> "00107f809aff"
        val out = AuthUtil.toHexString(
            byteArrayOf(0x00, 0x10, 0x7F, 0x80.toByte(), 0x9A.toByte(), 0xFF.toByte())
        )
        assertEquals("00107f809aff", out)
    }

    @Test
    fun toHexString_longerArray_stableConcatenation() {
        val out = AuthUtil.toHexString(
            byteArrayOf(
                0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()
            )
        )
        assertEquals("0123456789abcdef", out)
    }
}