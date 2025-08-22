package xbr.network

import org.junit.Assert.assertEquals
import org.junit.Test
import xbr.network.Util.toXBR
import java.math.BigInteger

class ToXBRTest {

    private val TEN18: BigInteger = BigInteger.TEN.pow(18)

    private fun expectXbr(x: Int): BigInteger =
            BigInteger.valueOf(x.toLong()).multiply(TEN18)

    /**
     * Test cases for xbr.network.Util#toXBR(int)
     */

    @Test
    fun zero_returnsZero() {
        val actual = toXBR(0)
        assertEquals(BigInteger.ZERO, actual)
        // divisible check
        assertEquals(BigInteger.ZERO, actual.mod(TEN18))
    }

    @Test
    fun one_and_minusOne() {
        val one = toXBR(1)
        val minusOne = toXBR(-1)

        assertEquals(BigInteger("1000000000000000000"), one)
        assertEquals(BigInteger("-1000000000000000000"), minusOne)

        // reversibility: divide by 10^18 gives original int
        assertEquals(BigInteger.ONE, one.divide(TEN18))
        assertEquals(BigInteger.valueOf(-1), minusOne.divide(TEN18))
    }

    @Test
    fun small_positive_and_negative_values() {
        val pos = 123
        val neg = -456

        val posActual = toXBR(pos)
        val negActual = toXBR(neg)

        assertEquals(expectXbr(pos), posActual)
        assertEquals(expectXbr(neg), negActual)

        // exact multiples (no remainder)
        assertEquals(BigInteger.ZERO, posActual.mod(TEN18))
        assertEquals(BigInteger.ZERO, negActual.mod(TEN18))

        // reversible
        assertEquals(BigInteger.valueOf(pos.toLong()), posActual.divide(TEN18))
        assertEquals(BigInteger.valueOf(neg.toLong()), negActual.divide(TEN18))
    }

    @Test
    fun int_bounds_min_and_max() {
        val max = Int.MAX_VALUE     //  2147483647
        val min = Int.MIN_VALUE     // -2147483648

        val maxActual = toXBR(max)
        val minActual = toXBR(min)

        // exact expected values via BigInteger math (no overflow)
        assertEquals(expectXbr(max), maxActual)
        assertEquals(expectXbr(min), minActual)

        // reversible and divisible by 10^18
        assertEquals(BigInteger.valueOf(max.toLong()), maxActual.divide(TEN18))
        assertEquals(BigInteger.ZERO, maxActual.mod(TEN18))

        assertEquals(BigInteger.valueOf(min.toLong()), minActual.divide(TEN18))
        assertEquals(BigInteger.ZERO, minActual.mod(TEN18))
    }

    /**
     * Test cases for xbr.network.Util#toXBR(byte[])
     */
    // --- Exceptional cases ---

    @Test(expected = NullPointerException::class)
    fun toXBR_null_throwsNullPointerException() {
        // When: null is passed
        // Then: NPE (BigInteger(byte[]) dereferences the array)
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // clarity for Java null
        val bytes: ByteArray? = null
        toXBR(bytes) // should throw
    }

    @Test(expected = NumberFormatException::class)
    fun toXBR_emptyArray_throwsNumberFormatException() {
        // BigInteger(byte[]) requires at least one byte
        toXBR(byteArrayOf())
    }

    // --- Zero representations ---

    @Test
    fun toXBR_singleZero_isZero() {
        val result = toXBR(byteArrayOf(0x00.toByte()))
        assertEquals(BigInteger.ZERO, result)
    }

    @Test
    fun toXBR_multipleLeadingZeros_isZero() {
        val result = toXBR(byteArrayOf(0x00, 0x00).map { it.toByte() }.toByteArray())
        assertEquals(BigInteger.ZERO, result)
    }

    // --- Positive numbers (no sign bit) ---

    @Test
    fun toXBR_smallPositive_noSignExtension() {
        // 0x7F = 127
        val result = toXBR(byteArrayOf(0x7F.toByte()))
        assertEquals(BigInteger.valueOf(127), result)
    }

    @Test
    fun toXBR_multibytePositive_noHighBitSet() {
        // 0x01 0x00 = 256
        val result = toXBR(byteArrayOf(0x01.toByte(), 0x00.toByte()))
        assertEquals(BigInteger.valueOf(256), result)
    }

    @Test
    fun toXBR_largePositiveBeyondLong() {
        // 0x01 0x00 0x00 0x00 0x00 0x00  = 2^40
        val bytes = byteArrayOf(
            0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte()
        )
        val result = toXBR(bytes)
        assertEquals(BigInteger.ONE.shiftLeft(40), result)
    }

    // --- Positive numbers that would be negative without a leading 0x00 ---

    @Test
    fun toXBR_positiveWithExplicitSignByte() {
        // 0x00 0x80 = +128  (without the 0x00, 0x80 would be -128)
        val result = toXBR(byteArrayOf(0x00.toByte(), 0x80.toByte()))
        assertEquals(BigInteger.valueOf(128), result)
    }

    @Test
    fun toXBR_positiveEdgeMaxBeforeSignFlip() {
        // 0x7F 0xFF = 32767
        val result = toXBR(byteArrayOf(0x7F.toByte(), 0xFF.toByte()))
        assertEquals(BigInteger.valueOf(32767), result)
    }

    // --- Negative numbers (two's complement) ---

    @Test
    fun toXBR_negativeSingleByteFF_isMinusOne() {
        val result = toXBR(byteArrayOf(0xFF.toByte()))
        assertEquals(BigInteger.valueOf(-1), result)
    }

    @Test
    fun toXBR_negativeSingleByte80_isMinus128() {
        val result = toXBR(byteArrayOf(0x80.toByte()))
        assertEquals(BigInteger.valueOf(-128), result)
    }

    @Test
    fun toXBR_negativeMultibyteFF00_isMinus256() {
        // 0xFF 0x00 in two's complement = -256
        val result = toXBR(byteArrayOf(0xFF.toByte(), 0x00.toByte()))
        assertEquals(BigInteger.valueOf(-256), result)
    }

    @Test
    fun toXBR_negativeWithSignExtension_isStillMinusOne() {
        // 0xFF 0xFF = -1 (proper sign extension)
        val result = toXBR(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
        assertEquals(BigInteger.valueOf(-1), result)
    }

    @Test
    fun toXBR_negativeNearEdge() {
        // 0xFF 0x7F = -129
        val result = toXBR(byteArrayOf(0xFF.toByte(), 0x7F.toByte()))
        assertEquals(BigInteger.valueOf(-129), result)
    }

    // --- Mixed leading zeros (should be ignored except for sign forcing) ---

    @Test
    fun toXBR_positiveWithExtraLeadingZeros_isSameValue() {
        // 0x00 0x00 0x01 0x02 = 258
        val bytes = byteArrayOf(0x00, 0x00, 0x01, 0x02).map { it.toByte() }.toByteArray()
        val result = toXBR(bytes)
        assertEquals(BigInteger.valueOf(258), result)
    }

    /**
     * Test cases for xbr.network.Util.toXBR(java.lang.Object)
     */
    // -------- Type-related & nullability behaviors (Object param) --------

    @Test(expected = NullPointerException::class)
    fun toXBR_nullObject_throwsNullPointerException() {
        // Java cast of null to byte[] yields null; BigInteger(null) -> NPE
        val obj: Any? = null
        toXBR(obj)
    }

    @Test(expected = ClassCastException::class)
    fun toXBR_stringObject_throwsClassCastException() {
        // Not a byte[] -> (byte[]) cast fails
        val obj: Any = "not bytes"
        toXBR(obj)
    }

    @Test(expected = ClassCastException::class)
    fun toXBR_boxedByteArray_throwsClassCastException() {
        // Array<Byte> (boxed) is Byte[] at runtime, not byte[] -> cast fails
        val boxed: Array<Byte> = arrayOf(0x01.toByte(), 0x02.toByte())
        val obj: Any = boxed
        toXBR(obj)
    }

    // -------- Empty array behavior --------

    @Test(expected = NumberFormatException::class)
    fun toXBRFromObject_emptyPrimitiveByteArray_throwsNumberFormatException() {
        // BigInteger(byte[]) requires at least one byte
        val obj: Any = byteArrayOf()
        toXBR(obj)
    }

    // -------- Zero encodings --------

    @Test
    fun toXBRFromObject_singleZero_isZero() {
        val obj: Any = byteArrayOf(0x00)
        val result = toXBR(obj)
        assertEquals(BigInteger.ZERO, result)
    }

    @Test
    fun toXBRFromObject_multipleLeadingZeros_isZero() {
        val obj: Any = byteArrayOf(0x00, 0x00)
        val result = toXBR(obj)
        assertEquals(BigInteger.ZERO, result)
    }

    // -------- Positive numbers (no sign bit set) --------

    @Test
    fun toXBRFromObject_smallPositive_noSignExtension() {
        val obj: Any = byteArrayOf(0x7F) // 127
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(127), result)
    }

    @Test
    fun toXBRFromObject_multibytePositive_noHighBitSet() {
        val obj: Any = byteArrayOf(0x01, 0x00) // 256
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(256), result)
    }

    @Test
    fun toXBRFromObject_largePositiveBeyondLong() {
        // 0x01 00 00 00 00 00 = 2^40
        val obj: Any = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00)
        val result = toXBR(obj)
        assertEquals(BigInteger.ONE.shiftLeft(40), result)
    }

    // -------- Positive values that need a leading 0x00 to avoid negative --------

    @Test
    fun toXBRFromObject_positiveWithExplicitSignByte() {
        // 0x00 0x80 = +128 (without 0x00, 0x80 would be -128)
        val obj: Any = byteArrayOf(0x00, 0x80.toByte())
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(128), result)
    }

    @Test
    fun toXBRFromObject_positiveEdgeMaxBeforeSignFlip() {
        // 0x7F 0xFF = 32767
        val obj: Any = byteArrayOf(0x7F, 0xFF.toByte())
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(32767), result)
    }

    // -------- Negative numbers (two's complement) --------

    @Test
    fun toXBRFromObject_negativeSingleByteFF_isMinusOne() {
        val obj: Any = byteArrayOf(0xFF.toByte())
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(-1), result)
    }

    @Test
    fun toXBRFromObject_negativeSingleByte80_isMinus128() {
        val obj: Any = byteArrayOf(0x80.toByte())
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(-128), result)
    }

    @Test
    fun toXBRFromObject_negativeMultibyteFF00_isMinus256() {
        // 0xFF 0x00 = -256
        val obj: Any = byteArrayOf(0xFF.toByte(), 0x00)
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(-256), result)
    }

    @Test
    fun toXBRFromObject_negativeWithSignExtension_isStillMinusOne() {
        // 0xFF 0xFF = -1
        val obj: Any = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(-1), result)
    }

    @Test
    fun toXBRFromObject_negativeNearEdge() {
        // 0xFF 0x7F = -129
        val obj: Any = byteArrayOf(0xFF.toByte(), 0x7F)
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(-129), result)
    }

    // -------- Leading zeros beyond sign byte --------

    @Test
    fun toXBRFromObject_positiveWithExtraLeadingZeros_isSameValue() {
        // 0x00 0x00 0x01 0x02 = 258
        val obj: Any = byteArrayOf(0x00, 0x00, 0x01, 0x02)
        val result = toXBR(obj)
        assertEquals(BigInteger.valueOf(258), result)
    }
}