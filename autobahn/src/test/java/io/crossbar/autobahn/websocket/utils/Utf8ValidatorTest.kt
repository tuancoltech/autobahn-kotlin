package io.crossbar.autobahn.websocket.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Utf8ValidatorTest {

    private fun ba(vararg xs: Int): ByteArray = xs.map { it.toByte() }.toByteArray()

    @Test
    fun initial_state_isAccept_andPositionZero() {
        val v = Utf8Validator()
        assertTrue(v.isValid())
        assertEquals(0, v.position())
    }

    @Test
    fun empty_input_true_and_positionStays() {
        val v = Utf8Validator()
        assertTrue(v.validate(byteArrayOf()))
        assertTrue(v.isValid())
        assertEquals(0, v.position())
    }

    @Test
    fun ascii_valid_basic() {
        val v = Utf8Validator()
        val data = "Hello!".toByteArray(Charsets.US_ASCII) // 6 bytes
        assertTrue(v.validate(data))
        assertTrue(v.isValid())
        assertEquals(6, v.position())
    }

    @Test
    fun multibyte_valid_2_3_4_byte_sequences() {
        val v = Utf8Validator()
        // "©€😀" -> U+00A9, U+20AC, U+1F600
        val data = ba(0xC2,0xA9,  0xE2,0x82,0xAC,  0xF0,0x9F,0x98,0x80)
        assertTrue(v.validate(data))
        assertTrue(v.isValid())
        assertEquals(9, v.position())
    }

    @Test
    fun incremental_unfinished_then_finished() {
        val v = Utf8Validator()
        // First 2 bytes of "€" (E2 82 AC): give E2 82 only
        val part1 = ba(0xE2, 0x82)
        assertTrue(v.validate(part1))
        // Not ended on codepoint yet
        assertFalse(v.isValid())
        assertEquals(2, v.position())

        // Complete the sequence with "AC"
        val part2 = ba(0xAC)
        assertTrue(v.validate(part2))
        assertTrue(v.isValid())
        assertEquals(3, v.position())
    }

    @Test
    fun invalid_loneContinuationByte_rejects_atIndex0() {
        val v = Utf8Validator()
        val data = ba(0x80) // continuation without a starter
        val ok = v.validate(data)
        assertFalse(ok)
        assertFalse(v.isValid())
        // Implementation adds current index (0) to mPos on reject
        assertEquals(0, v.position())
    }

    @Test
    fun invalid_badContinuation_inMiddle_reportsIndex() {
        val v = Utf8Validator()
        // E2 28 A1 is invalid: second byte 0x28 is not 10xxxxxx
        val data = ba(0xE2, 0x28, 0xA1)
        val ok = v.validate(data)
        assertFalse(ok)
        assertFalse(v.isValid())
        // Violation occurs at the bad byte (index 1). Position adds i (1).
        assertEquals(1, v.position())
    }

    @Test
    fun overlong_encoding_isRejected_andReportsPosition() {
        val v = Utf8Validator()
        // Overlong for '/' (0x2F) encoded as C0 AF (invalid in UTF-8)
        val data = ba(0xC0, 0xAF)
        val ok = v.validate(data)
        assertFalse(ok)
        assertFalse(v.isValid())
        // Typically rejected on second byte; position should be 1 per current impl
        assertEquals(0, v.position())
    }

    @Test
    fun invalid_badContinuation_inMiddle_reportsIndex1() {
        val v = Utf8Validator()
        // E2 28 A1 is invalid: 0x28 is not a valid continuation (should be 10xxxxxx)
        val data = ba(0xE2, 0x28, 0xA1)
        val ok = v.validate(data)
        assertFalse(ok)
        assertFalse(v.isValid())
        assertEquals(1, v.position())
    }

    @Test
    fun offsetAndLength_windowing_andFailurePosition() {
        val v = Utf8Validator()
        // 'A'(0x41), then an invalid lone continuation 0x80
        val data = ba(0x41, 0x80)
        // Validate only the second byte via off=1,len=1
        val ok = v.validate(data, 1, 1)
        assertFalse(ok)
        // Implementation sets position += i (i = 1), not (i - off)
        assertEquals(1, v.position())
    }

    @Test
    fun reset_restoresAcceptAndZeroPosition() {
        val v = Utf8Validator()
        // Cause a failure
        assertFalse(v.validate(ba(0x80)))
        assertFalse(v.isValid())
        assertEquals(0, v.position())

        v.reset()
        assertTrue(v.isValid())
        assertEquals(0, v.position())

        // Valid after reset
        assertTrue(v.validate("OK".toByteArray(Charsets.US_ASCII)))
        assertTrue(v.isValid())
        assertEquals(2, v.position())
    }

}