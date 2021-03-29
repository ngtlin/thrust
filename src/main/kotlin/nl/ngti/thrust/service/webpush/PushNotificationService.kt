package nl.ngti.thrust.service.webpush

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.ngti.thrust.config.ServerKeys
import nl.ngti.thrust.model.Notification
import nl.ngti.thrust.model.Subscription
import nl.ngti.thrust.utils.CryptoUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.annotation.PostConstruct

@Service
class PushNotificationService(val serverKeys: ServerKeys) {

    val logger: Logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    @Autowired
    lateinit var cryptoUtils: CryptoUtils

    private val jsonMapper = ObjectMapper().registerModule(KotlinModule())

    private val jwtAlgorithm = Algorithm.ECDSA256(serverKeys.publicKey, serverKeys.privateKey)

    lateinit var restTemplate: RestTemplate

    @PostConstruct
    fun init() {
        restTemplate = RestTemplate()
    }

   fun sendNotification(subscription: Subscription, notification: Notification): Boolean {
       val message = notification?.body
       message?.let {
           val result: ByteArray = cryptoUtils.encrypt(jsonMapper.writeValueAsString(message), subscription.keys.p256dh, subscription.keys.auth, 0)
           return sendMessage(subscription, result)
       }
       return false;
   }

    private fun sendMessage(subscription: Subscription, body: ByteArray): Boolean {
        var origin: String? = null
        origin = try {
            val url = URL(subscription.endpoint)
            url.protocol + "://" + url.host
        } catch (e: MalformedURLException) {
            logger.error("create origin", e)
            return true
        }

        val today = Date()
        val expires = Date(today.time + 12 * 60 * 60 * 1000)

        val token = JWT.create().withAudience(origin).withExpiresAt(expires)
            .withSubject("mailto:info@ngti.nl").sign(jwtAlgorithm)

        val h = HttpHeaders()
        h.contentType = MediaType.APPLICATION_OCTET_STREAM
        h.add("Content-Encoding", "aes128gcm")
        h.add("Authorization", "vapid t=" + token + ", k=" + serverKeys.publicKeyBase64)
        h.add("TTL", "180")

        val resEntity = HttpEntity<ByteArray>(body, h)
        try {
            val response = restTemplate.exchange(subscription.endpoint, HttpMethod.POST, resEntity, String::class.java)
            when (response.statusCode.value()) {
                201 -> logger.info("Push message successfully sent: {}", subscription.endpoint)
                404, 410 -> {
                    logger.warn("Subscription not found or gone: {}", subscription.endpoint)
                    // remove subscription from our collection of subscriptions
                    return true
                }
                429 -> logger.error("Too many requests: {}", resEntity)
                400 -> logger.error("Invalid request: {}", resEntity)
                413 -> logger.error("Payload size too large: {}", resEntity)
                else -> logger.error("Unhandled status code: {} / {}", response.statusCode, resEntity)
            }
        } catch (e: IOException) {
            logger.error("send push message", e)
        } catch (e: InterruptedException) {
            logger.error("send push message", e)
        }

        return false
    }
}