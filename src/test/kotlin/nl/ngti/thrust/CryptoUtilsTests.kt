package nl.ngti.thrust

import nl.ngti.thrust.utils.CryptoUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoUtilsTests {

    @Test
    fun generateServerKeysTests() {
        val cryptoUtils = CryptoUtils()

        cryptoUtils.init()
        val pair: KeyPair = cryptoUtils.keyPairGenerator.generateKeyPair()
        val publicKey = pair.public as ECPublicKey
        val privateKey = pair.private as ECPrivateKey
        val encodedEcPubKeys = cryptoUtils.encodeECPublicKey(publicKey)
        println("encodedPublicKeyX = ${encodedEcPubKeys.encodedPublicKeyX}")
        println("encodedPublicKeyY = ${encodedEcPubKeys.encodedPublicKeyY}")
        val encodedPrivateKey = cryptoUtils.encodeECPrivateKey(privateKey)
        println("encodedPrivateKey = $encodedPrivateKey")

        // Decode the Base64 encoded pub/priv keys
        val publicKeyGenerated = cryptoUtils.decodeECPublicKey(encodedEcPubKeys)
        assert(publicKeyGenerated.equals(publicKey))
        val privateKeyGenerated = cryptoUtils.decodeECPrivateKey(encodedPrivateKey)
        assert(privateKeyGenerated.equals(privateKey))
    }
}