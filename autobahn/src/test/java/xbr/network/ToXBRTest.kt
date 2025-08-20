package xbr.network

import org.junit.Assert.assertEquals
import org.junit.Test
import xbr.network.Util.toXBR
import java.math.BigInteger

class ToXBRTest {

    private val TEN18: BigInteger = BigInteger.TEN.pow(18)

    private fun expectXbr(x: Int): BigInteger =
            BigInteger.valueOf(x.toLong()).multiply(TEN18)

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
}