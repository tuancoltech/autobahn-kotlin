package xbr.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import java.lang.reflect.Method
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class RecoverEIP712SignerTest {
    // ---------- Helpers ----------

    /** Call the same private JSON builder used by the production method to guarantee identical hashing. */
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

    /** Produce 65-byte (r||s||v) signature for the exact EIP-712 hash. */
    private fun signTypedData(
        key: ECKeyPair,
        chainId: Int,
        verifyingContract: String,
        closeAt: Int,
        marketId: String,
        channelId: String,
        channelSeq: Int,
        balance: BigInteger,
        isFinal: Boolean
    ): ByteArray {
        val json = createEip712Json(chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal)
        val hash = StructuredDataEncoder(json.toString()).hashStructuredData() // 32-byte digest
        val sig = Sign.signMessage(hash, key, false) // no prefix
        return ByteArray(65).apply {
            System.arraycopy(sig.r, 0, this, 0, 32)
            System.arraycopy(sig.s, 0, this, 32, 32)
            this[64] = sig.v[0] // 27/28
        }
    }

    private fun addrOf(key: ECKeyPair): String = "0x" + Keys.getAddress(key.publicKey)

    // Valid constants (match your EIP712 schema)
    private val chainId = 5
    private val verifyingContract = "0xAbCdEf0123456789abcdef0123456789ABCDEF01" // 20 bytes
    private val closeAt = 987_654_321
    private val marketId = "0x000102030405060708090a0b0c0d0e0f"           // bytes16 (32 hex chars after 0x)
    private val channelId = "0xfedcba98765432100123456789abcdef"           // bytes16
    private val channelSeq = 777
    private val balance = BigInteger.ONE.shiftLeft(200)                     // uint256
    private val isFinal = true

    // ---------- Tests ----------

    @Test
    fun recover_happyPath_recoversSignerAddress() {
        val key = ECKeyPair.create(BigInteger("1", 16)) // fixed private key 0x01
        val expected = addrOf(key)

        val sig = signTypedData(key, chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal)

        val got = Util.recoverEIP712Signer(
            chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal, sig
        ).get(5, TimeUnit.SECONDS)

        assertEquals(expected, got)
    }

    @Test
    fun recover_corruptedSignature_completesExceptionally() {
        // 65 bytes but r/s are zero → recovery will blow up inside BouncyCastle
        val bad = ByteArray(65) { 0 }.apply { this[64] = 27 } // set v to a valid value

        try {
            Util.recoverEIP712Signer(
                chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal, bad
            ).get(5, TimeUnit.SECONDS)

            fail("Expected exceptional completion for corrupted signature")
        } catch (e: Exception) {
            // Production method completes the future exceptionally on ANY Exception
            // Common causes here: IllegalArgumentException("Invalid point compression")
            assertNotNull(e.cause)
            // Optional: assert specific type/message if you want
            // assertTrue(e.cause is IllegalArgumentException)
            // assertTrue(e.cause?.message?.contains("Invalid point compression") == true)
        }
    }

    @Test
    fun recover_mismatchedPayload_doesNotReturnOriginalSigner() {
        val key = ECKeyPair.create(BigInteger("3", 16))
        val sig = signTypedData(key, chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal)

        // Change one field (channelSeq) so the recovered key shouldn't match the signer of the signature
        val got = Util.recoverEIP712Signer(
            chainId, verifyingContract, closeAt, marketId, channelId, channelSeq + 1, balance, isFinal, sig
        ).get(5, TimeUnit.SECONDS)

        assertNotEquals(addrOf(key), got) // likely null or a different address
    }

    @Test
    fun recover_invalidBytes16_causesExceptionalCompletion() {
        val invalidMarketId = "0xabc" // not 32 hex chars → StructuredDataEncoder will throw
        try {
            Util.recoverEIP712Signer(
                chainId, verifyingContract, closeAt, invalidMarketId, channelId, channelSeq, balance, isFinal,
                ByteArray(65) { 0 }.apply { this[64] = 27 }
            ).get(5, TimeUnit.SECONDS)
            fail("Expected exceptional completion due to invalid bytes16")
        } catch (e: Exception) {
            assertNotNull(e.cause) // typically RuntimeException from encoder
        }
    }

    @Test
    fun recover_tooShortSignature_completesExceptionally() {
        val tooShort = ByteArray(64) // missing v byte at index 64
        try {
            Util.recoverEIP712Signer(
                chainId, verifyingContract, closeAt, marketId, channelId, channelSeq, balance, isFinal, tooShort
            ).get(5, TimeUnit.SECONDS)
            fail("Expected exceptional completion due to short signature")
        } catch (e: Exception) {
            assertNotNull(e.cause) // ArrayIndexOutOfBoundsException or similar
        }
    }
}