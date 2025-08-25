package xbr.network

import junit.framework.TestCase.assertTrue
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import java.lang.reflect.Method
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SignEIP712DataTest {

    // ----------------------
    // Helpers
    // ----------------------

    // reflect the private createEIP712Data(...) so we can reproduce the exact hash used by the method
    private fun createEip712Json(
        chainId: Int,
        verifyingContract: String,
        closeAt: Int,
        marketId: String,
        channelId: String,
        channelSeq: Int,
        balance: BigInteger,
        isFinal: Boolean
    ): JSONObject {
        val m: Method = Util::class.java.getDeclaredMethod(
            "createEIP712Data",
            Int::class.javaPrimitiveType,
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            BigInteger::class.java,
            Boolean::class.javaPrimitiveType
        )
        m.isAccessible = true
        return m.invoke(
            null, chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal
        ) as JSONObject
    }

    private fun callSign(
        keyPair: ECKeyPair,
        chainId: Int = 1,
        verifyingContract: String = "0x0000000000000000000000000000000000000001",
        closeAt: Int = 123456789,
        marketId: String = "0x000102030405060708090a0b0c0d0e0f",      // ✅ 16 bytes
        channelId: String = "0xfedcba98765432100123456789abcdef",      // ✅ 16 bytes
        channelSeq: Int = 42,
        balance: BigInteger = BigInteger("1000000000000000000"),
        isFinal: Boolean = false
    ): ByteArray {
        val fut = Util.signEIP712Data(
            keyPair, chainId, verifyingContract, closeAt,
            marketId, channelId, channelSeq, balance, isFinal
        )
        return fut.get(5, TimeUnit.SECONDS)
    }

    private fun bytesToSignatureData(sig: ByteArray): Sign.SignatureData {
        require(sig.size == 65)
        val r = sig.copyOfRange(0, 32)
        val s = sig.copyOfRange(32, 64)
        val v = byteArrayOf(sig[64])
        return Sign.SignatureData(v, r, s)
    }

    // ----------------------
    // Tests
    // ----------------------

    @Test
    fun sign_deterministic_sameInputs_sameSignature() {
        val priv = BigInteger("2".repeat(64), 16)
        val keyPair = ECKeyPair.create(priv)

        val s1 = callSign(keyPair)
        val s2 = callSign(keyPair)

        // deterministic RFC6979 should produce identical signatures
        assertArrayEquals(s1, s2)
    }

    @Test
    fun sign_vByte_is27or28() {
        val keyPair = ECKeyPair.create(BigInteger("3".repeat(64), 16))
        val sig = callSign(keyPair)
        val v = sig[64].toInt()
        assertTrue("v must be 27 or 28, was $v", v == 27 || v == 28)
    }

    @Test(expected = NullPointerException::class)
    fun sign_nullKeyPair_throwsNPE_notCaughtByMethod() {
        // The method catches only IOException | JSONException.
        // Passing null keyPair leads to NPE inside Sign.signMessage(...)
        @Suppress("NULL_FOR_NONNULL_TYPE") // explicit for clarity
        val kp: ECKeyPair? = null
        Util.signEIP712Data(
            kp, 1, "0x0", 0, "0x000102030405060708090a0b0c0d0e0f", "0xfedcba98765432100123456789abcdef", 0, BigInteger.ZERO, false
        )
    }

    @Test
    fun sign_signatureLooksValid_basicShape() {
        val keyPair = ECKeyPair.create(BigInteger("4".repeat(64), 16))
        val sig = callSign(keyPair)
        // non-zero r/s (highly likely with a valid signature)
        assertTrue(sig.copyOfRange(0, 32).any { it.toInt() != 0 })
        assertTrue(sig.copyOfRange(32, 64).any { it.toInt() != 0 })
    }
}