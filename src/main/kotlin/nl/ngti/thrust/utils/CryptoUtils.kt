package nl.ngti.thrust.utils

import nl.ngti.thrust.model.EncodedECPublicKeys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.codec.ByteArrayEncoder
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import java.util.*

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

import javax.annotation.PostConstruct

@Component
class CryptoUtils {
    var logger: Logger = LoggerFactory.getLogger(CryptoUtils::class.java)

    private val SECURE_RANDOM: SecureRandom = SecureRandom()

    lateinit var keyPairGenerator: KeyPairGenerator

    private lateinit var keyFactory: KeyFactory

    @PostConstruct
    fun init() {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"), SECURE_RANDOM)
            keyFactory = KeyFactory.getInstance("EC")
        } catch (e: NoSuchAlgorithmException) {
            logger.error("init crypto", e)
        } catch (e: InvalidAlgorithmParameterException) {
            logger.error("init crypto", e)
        }
    }

    fun encodeECPublicKey(publicKey: ECPublicKey): EncodedECPublicKeys {
        val publicKeyX = publicKey.w.affineX.toByteArray()
        val publicKeyY = publicKey.w.affineY.toByteArray()
        return EncodedECPublicKeys(Base64.getEncoder().encodeToString(publicKeyX), Base64.getEncoder().encodeToString(publicKeyY))
    }

    fun decodeECPublicKey(encodedEcPublicKeys: EncodedECPublicKeys): ECPublicKey {
        val decodedPublicKeyX = Base64.getDecoder().decode(encodedEcPublicKeys.encodedPublicKeyX)
        val decodedPublicKeyY = Base64.getDecoder().decode(encodedEcPublicKeys.encodedPublicKeyY)
        val pubPoint = ECPoint(BigInteger(1, decodedPublicKeyX), BigInteger(1, decodedPublicKeyY))
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec("secp256r1"))
        val ecParameters = parameters.getParameterSpec(ECParameterSpec::class.java)
        val pubSpec = ECPublicKeySpec(pubPoint, ecParameters)
        val publicKeyGenerated = keyFactory.generatePublic(pubSpec)
        return publicKeyGenerated as ECPublicKey
    }

    fun encodeECPrivateKey(privateKey: ECPrivateKey): String {
        val privateKeyS: ByteArray = privateKey.s.toByteArray()
        return Base64.getEncoder().encodeToString(privateKeyS)
    }

    fun decodeECPrivateKey(encodedEcPrivateKey: String): ECPrivateKey {
        val decodedPrivateKeyS = Base64.getDecoder().decode(encodedEcPrivateKey)
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec("secp256r1"))
        val ecParameters = parameters.getParameterSpec(ECParameterSpec::class.java)
        val privateSpec = ECPrivateKeySpec(BigInteger(1, decodedPrivateKeyS), ecParameters)
        val privateKeyGenerated = keyFactory.generatePrivate(privateSpec)
        return privateKeyGenerated as ECPrivateKey
    }

    @Throws(InvalidKeySpecException::class)
    fun convertX509ToECPublicKey(encodedPublicKey: ByteArray?): PublicKey? {
        val pubX509 = X509EncodedKeySpec(encodedPublicKey)
        return keyFactory!!.generatePublic(pubX509)
    }

    @Throws(InvalidKeySpecException::class)
    fun convertPKCS8ToECPrivateKey(encodedPrivateKey: ByteArray?): PrivateKey? {
        val pkcs8spec = PKCS8EncodedKeySpec(encodedPrivateKey)
        return keyFactory!!.generatePrivate(pkcs8spec)
    }

    // https://stackoverflow.com/questions/30445997/loading-raw-64-byte-long-ecdsa-public-key-in-java
    // X509 head without (byte)4
    private val P256_HEAD = Base64.getDecoder()
        .decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgA")

    // String must start with (byte)4
    @Throws(InvalidKeySpecException::class)
    fun fromUncompressedECPublicKey(encodedPublicKey: String?): ECPublicKey {
        val w = Base64.getUrlDecoder().decode(encodedPublicKey)
        val encodedKey = ByteArray(P256_HEAD.size + w.size)
        System.arraycopy(P256_HEAD, 0, encodedKey, 0, P256_HEAD.size)
        System.arraycopy(w, 0, encodedKey, P256_HEAD.size, w.size)
        val ecpks = X509EncodedKeySpec(encodedKey)
        return keyFactory!!.generatePublic(ecpks) as ECPublicKey
    }

    // Result starts with (byte)4
    fun toUncompressedECPublicKey(publicKey: ECPublicKey): ByteArray {
        val result = ByteArray(65)
        val encoded = publicKey.encoded
        System.arraycopy(
            encoded, P256_HEAD.size, result, 0,
            encoded.size - P256_HEAD.size
        )
        return result
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        // Determine the length of the result array
        var totalLength = 0
        for (array in arrays) {
            totalLength += array.size
        }

        // create the result array
        val result = ByteArray(totalLength)

        // copy the source arrays into the result array
        var currentIndex = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.size)
            currentIndex += array.size
        }
        return result
    }

    // https://tools.ietf.org/html/rfc8291
    // 3.4. Encryption Summary
    @Throws(
        InvalidKeyException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class
    )
    fun encrypt(
        plainTextString: String,
        uaPublicKeyString: String?,
        authSecret: String?,
        paddingSize: Int
    ): ByteArray {
        // ecdh_secret = ECDH(as_private, ua_public)
        // auth_secret = <from user agent>
        // salt = random(16)
        val asKeyPair = keyPairGenerator.genKeyPair()
        val asPublicKey = asKeyPair.public as ECPublicKey
        val uncompressedASPublicKey = toUncompressedECPublicKey(asPublicKey)
        val uaPublicKey = fromUncompressedECPublicKey(uaPublicKeyString)
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(asKeyPair.private)
        keyAgreement.doPhase(uaPublicKey, true)
        val ecdhSecret = keyAgreement.generateSecret()
        val salt = ByteArray(16)
        SECURE_RANDOM!!.nextBytes(salt)

        // ## Use HKDF to combine the ECDH and authentication secrets
        // # HKDF-Extract(salt=auth_secret, IKM=ecdh_secret)
        // PRK_key = HMAC-SHA-256(auth_secret, ecdh_secret)
        val hmacSHA256 = Mac.getInstance("HmacSHA256")
        hmacSHA256.init(SecretKeySpec(Base64.getUrlDecoder().decode(authSecret), "HmacSHA256"))
        val prkKey = hmacSHA256.doFinal(ecdhSecret)

        // # HKDF-Expand(PRK_key, key_info, L_key=32)
        // key_info = "WebPush: info" || 0x00 || ua_public || as_public
        val keyInfo = concat(
            "WebPush: info\u0000".toByteArray(StandardCharsets.UTF_8),
            toUncompressedECPublicKey(uaPublicKey), uncompressedASPublicKey
        )
        // IKM = HMAC-SHA-256(PRK_key, key_info || 0x01)
        hmacSHA256.init(SecretKeySpec(prkKey, "HmacSHA256"))
        hmacSHA256.update(keyInfo)
        hmacSHA256.update(1.toByte())
        val ikm = hmacSHA256.doFinal()

        // ## HKDF calculations from RFC 8188
        // # HKDF-Extract(salt, IKM)
        // PRK = HMAC-SHA-256(salt, IKM)
        hmacSHA256.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = hmacSHA256.doFinal(ikm)

        // # HKDF-Expand(PRK, cek_info, L_cek=16)
        // cek_info = "Content-Encoding: aes128gcm" || 0x00
        val cekInfo = "Content-Encoding: aes128gcm\u0000".toByteArray(StandardCharsets.UTF_8)
        // CEK = HMAC-SHA-256(PRK, cek_info || 0x01)[0..15]
        hmacSHA256.init(SecretKeySpec(prk, "HmacSHA256"))
        hmacSHA256.update(cekInfo)
        hmacSHA256.update(1.toByte())
        var cek = hmacSHA256.doFinal()
        cek = Arrays.copyOfRange(cek, 0, 16)

        // # HKDF-Expand(PRK, nonce_info, L_nonce=12)
        // nonce_info = "Content-Encoding: nonce" || 0x00
        val nonceInfo = "Content-Encoding: nonce\u0000".toByteArray(StandardCharsets.UTF_8)
        // NONCE = HMAC-SHA-256(PRK, nonce_info || 0x01)[0..11]
        hmacSHA256.init(SecretKeySpec(prk, "HmacSHA256"))
        hmacSHA256.update(nonceInfo)
        hmacSHA256.update(1.toByte())
        var nonce = hmacSHA256.doFinal()
        nonce = Arrays.copyOfRange(nonce, 0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE, SecretKeySpec(cek, "AES"),
            GCMParameterSpec(128, nonce)
        )
        val inputs: MutableList<ByteArray> = ArrayList()
        val plainTextBytes = plainTextString.toByteArray(StandardCharsets.UTF_8)
        inputs.add(plainTextBytes)
        inputs.add(byteArrayOf(2)) // padding delimiter
        val padSize = Math.max(0, paddingSize - plainTextBytes.size)
        if (padSize > 0) {
            inputs.add(ByteArray(padSize))
        }
        val encrypted = cipher.doFinal(concat(*inputs.toTypedArray()))
        val encryptedArrayLength = ByteBuffer.allocate(4)
        encryptedArrayLength.putInt(encrypted.size)
        val header = concat(
            salt,
            encryptedArrayLength.array(),
            byteArrayOf(uncompressedASPublicKey.size.toByte()),
            uncompressedASPublicKey
        )
        return concat(header, encrypted)
    }

}