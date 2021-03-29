package nl.ngti.thrust.config

import nl.ngti.thrust.model.EncodedECPublicKeys
import nl.ngti.thrust.utils.CryptoUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.*
import javax.annotation.PostConstruct

@Component
class ServerKeys (val cryptoUtils: CryptoUtils) {

    @Value("\${thrust.webpush.pubKeyX}")
    private val pubKeyXBase64: String? = null

    @Value("\${thrust.webpush.pubKeyY}")
    private val pubKeyYBase64: String? = null

    @Value("\${thrust.webpush.privKey}")
    private val privKeyBase64: String? = null

    var publicKey: ECPublicKey? = null
    var privateKey: ECPrivateKey? = null
    var publicKeyUncompressed: ByteArray? = null
    var publicKeyBase64: String? = null

    @PostConstruct
    fun init() {
        pubKeyXBase64?.let {
            val encodedEcPubX = it
            pubKeyYBase64?.let {
                val encodedEcPubY = it
                publicKey = cryptoUtils.decodeECPublicKey(EncodedECPublicKeys(encodedEcPubX, encodedEcPubY))
                publicKey?.let {
                    publicKeyUncompressed = cryptoUtils.toUncompressedECPublicKey(it)
                }
                publicKeyBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKeyUncompressed)
            }
        }
        privKeyBase64?.let {
            privateKey = cryptoUtils.decodeECPrivateKey(it)
        }
    }
}