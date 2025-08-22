package xbr.network

import junit.framework.TestCase.assertEquals
import org.junit.Test
import xbr.network.Util.toInt
import java.math.BigInteger

class ToIntTest {
    private val BI = BigInteger.TEN.pow(18) // 10^18

    // ---------- Null handling ----------

    @Test(expected = NullPointerException::class)
    fun toInt_null_throwsNullPointerException() {
        val v: BigInteger? = null
        // value.divide(bi) dereferences null -> NPE
        toInt(v)
    }

    // ---------- Zero & small magnitudes ----------

    @Test
    fun toInt_zero_returnsZero() {
        assertEquals(0, toInt(BigInteger.ZERO))
    }

    @Test
    fun toInt_lessThan1e18_positive_truncatesToZero() {
        val v = BI.subtract(BigInteger.ONE) // 10^18 - 1
        assertEquals(0, toInt(v))
    }

    @Test
    fun toInt_lessThan1e18_negative_truncatesDownToMinusOne() {
        // (-10^18 + 1) / 10^18 = -0.999... -> floor = -1
        val v = BI.negate().add(BigInteger.ONE)
        assertEquals(0, toInt(v))
    }

    // ---------- Exact multiples of 10^18 ----------

    @Test
    fun toInt_exactOne_returnsOne() {
        assertEquals(1, toInt(BI))
    }

    @Test
    fun toInt_exactMinusOne_returnsMinusOne() {
        assertEquals(-1, toInt(BI.negate()))
    }

    // ---------- Positive truncation (with remainder) ----------

    @Test
    fun toInt_positiveWithRemainder_truncatesTowardZero() {
        // (1 * 10^18 + 1) / 10^18 -> 1
        val v = BI.add(BigInteger.ONE)
        assertEquals(1, toInt(v))
    }

    // ---------- Negative truncation (floor behavior) ----------

    @Test
    fun toInt_negativeBeyondExactMultiple_floorsTowardMinusInfinity() {
        // (-1 * 10^18 - 1) / 10^18 -> -2 (since BigInteger division floors for positive divisor)
        val v = BI.negate().subtract(BigInteger.ONE)
        assertEquals(-1, toInt(v))
    }

    // ---------- Integer range boundaries ----------

    @Test
    fun toInt_exactIntMax_returnsIntMax() {
        val q = BigInteger.valueOf(Int.MAX_VALUE.toLong()) // 2_147_483_647
        val v = q.multiply(BI) // quotient == Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, toInt(v))
    }

    @Test
    fun toInt_exactIntMin_returnsIntMin() {
        val q = BigInteger.valueOf(Int.MIN_VALUE.toLong()) // -2_147_483_648
        val v = q.multiply(BI) // quotient == Int.MIN_VALUE
        assertEquals(Int.MIN_VALUE, toInt(v))
    }

    @Test(expected = NumberFormatException::class)
    fun toInt_overIntMax_throwsNumberFormatException() {
        // quotient = Int.MAX_VALUE + 1 -> parseInt overflow
        val q = BigInteger.valueOf(Int.MAX_VALUE.toLong()).add(BigInteger.ONE) // 2_147_483_648
        val v = q.multiply(BI)
        toInt(v)
    }

    @Test(expected = NumberFormatException::class)
    fun toInt_belowIntMin_throwsNumberFormatException() {
        // quotient = Int.MIN_VALUE - 1 -> parseInt overflow
        val q = BigInteger.valueOf(Int.MIN_VALUE.toLong()).subtract(BigInteger.ONE) // -2_147_483_649
        val v = q.multiply(BI)
        toInt(v)
    }

    // ---------- Additional sanity checks ----------

    @Test
    fun toInt_largePositiveMultipleWithinRange() {
        // e.g., 123456789 * 10^18 -> 123456789
        val q = BigInteger.valueOf(123_456_789L)
        val v = q.multiply(BI)
        assertEquals(123_456_789, toInt(v))
    }

    @Test
    fun toInt_largeNegativeMultipleWithinRange() {
        val q = BigInteger.valueOf(-987_654_321L)
        val v = q.multiply(BI)
        assertEquals(-987_654_321, toInt(v))
    }
}