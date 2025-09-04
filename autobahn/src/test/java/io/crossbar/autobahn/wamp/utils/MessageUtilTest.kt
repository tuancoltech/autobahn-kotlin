package io.crossbar.autobahn.wamp.utils

import io.crossbar.autobahn.wamp.exceptions.ProtocolError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MessageUtilTest {

    // -------- validateMessage(wmsg, type, name, lengthMin, lengthMax) --------

    @Test
    fun validateMessage_range_ok_whenAtMin() {
        val type = 1
        val wmsg = listOf<Any>(type, "payload")
        // size == 2, min=2, max=3 -> OK
        MessageUtil.validateMessage(wmsg, type, "TestMsg", 2, 3)
    }

    @Test
    fun validateMessage_range_ok_whenAtMax() {
        val type = 7
        val wmsg = listOf<Any>(type, "a", "b", "c")
        // size == 4, min=2, max=4 -> OK
        MessageUtil.validateMessage(wmsg, type, "TestMsg", 2, 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMessage_range_empty_throwsIllegalArgument() {
        MessageUtil.validateMessage(emptyList(), 1, "TestMsg", 2, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMessage_range_firstIsNotInteger_throwsIllegalArgument() {
        val wmsg = listOf<Any>("NOT_INT", 123)
        MessageUtil.validateMessage(wmsg, 1, "TestMsg", 1, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMessage_range_wrongTypeCode_throwsIllegalArgument() {
        val wmsg = listOf<Any>(99, "body")
        MessageUtil.validateMessage(wmsg, 1, "TestMsg", 1, 3)
    }

    @Test
    fun validateMessage_range_tooShort_throwsProtocolErrorWithMessage() {
        val type = 2
        val wmsg = listOf<Any>(type) // size=1, min=2
        try {
            MessageUtil.validateMessage(wmsg, type, "TooShortMsg", 2, 4)
            fail("Expected ProtocolError")
        } catch (e: ProtocolError) {
            // Message should include actual length and verbose name
            assertTrue(e.message?.contains("Invalid message length 1") == true)
            assertTrue(e.message?.contains("TooShortMsg") == true)
        }
    }

    @Test
    fun validateMessage_range_tooLong_throwsProtocolError() {
        val type = 5
        val wmsg = listOf<Any>(type, "a", "b", "c", "d") // size=5, max=4
        try {
            MessageUtil.validateMessage(wmsg, type, "TooLongMsg", 2, 4)
            fail("Expected ProtocolError")
        } catch (e: ProtocolError) {
            assertTrue(e.message?.contains("Invalid message length 5") == true)
            assertTrue(e.message?.contains("TooLongMsg") == true)
        }
    }

    // -------- validateMessage(wmsg, type, name, length) (exact length overload) --------

    @Test
    fun validateMessage_exact_ok() {
        val type = 10
        val wmsg = listOf<Any>(type, 42L, "ok") // size 3
        MessageUtil.validateMessage(wmsg, type, "ExactMsg", 3) // exact=3 -> OK
    }

    @Test
    fun validateMessage_exact_tooShort_throwsProtocolError() {
        val type = 3
        val wmsg = listOf<Any>(type) // size 1
        try {
            MessageUtil.validateMessage(wmsg, type, "ExactShort", 2)
            fail("Expected ProtocolError")
        } catch (e: ProtocolError) {
            assertTrue(e.message?.contains("Invalid message length 1") == true)
            assertTrue(e.message?.contains("ExactShort") == true)
        }
    }

    @Test
    fun validateMessage_exact_tooLong_throwsProtocolError() {
        val type = 4
        val wmsg = listOf<Any>(type, "a", "b", "c") // size 4
        try {
            MessageUtil.validateMessage(wmsg, type, "ExactLong", 3)
            fail("Expected ProtocolError")
        } catch (e: ProtocolError) {
            assertTrue(e.message?.contains("Invalid message length 4") == true)
            assertTrue(e.message?.contains("ExactLong") == true)
        }
    }

    // -------- parseLong(Object) --------

    @Test
    fun parseLong_withInteger_returnsValue() {
        assertEquals(123L, MessageUtil.parseLong(123))
        assertEquals(-7L, MessageUtil.parseLong(-7))
    }

    @Test
    fun parseLong_withLong_returnsValue() {
        assertEquals(1234567890123L, MessageUtil.parseLong(1234567890123L))
        assertEquals(-1L, MessageUtil.parseLong(-1L))
    }

    @Test
    fun parseLong_withNull_returnsZero() {
        val obj: Any? = null
        assertEquals(0L, MessageUtil.parseLong(obj))
    }

    @Test
    fun parseLong_withOtherTypes_returnsZero() {
        assertEquals(0L, MessageUtil.parseLong("123"))
        assertEquals(0L, MessageUtil.parseLong(3.14))
        assertEquals(0L, MessageUtil.parseLong(true))
        assertEquals(0L, MessageUtil.parseLong(listOf(1, 2, 3)))
    }

}