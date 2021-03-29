package nl.ngti.thrust.api

import nl.ngti.thrust.model.Notification
import nl.ngti.thrust.model.Subscription
import nl.ngti.thrust.model.SubscriptionEndpoint
import nl.ngti.thrust.service.webpush.PushNotificationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

@RestController()
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/thrust/webpush")
class WebPushEndpoint {

    var logger: Logger = LoggerFactory.getLogger(WebPushEndpoint::class.java)

    private val subscriptions: ConcurrentHashMap<String, Subscription> = ConcurrentHashMap<String, Subscription>()

    @Autowired
    lateinit var pushNotificationService: PushNotificationService

    @GetMapping(path = ["/publicSigningKey"], produces = ["application/octet-stream"])
    fun getPublicSigningKey(): ByteArray? {
        return pushNotificationService.serverKeys.publicKeyUncompressed
    }

    @GetMapping(path = ["/publicSigningKeyBase64"])
    fun publicSigningKeyBase64(): String? {
        return pushNotificationService.serverKeys.publicKey.toString()
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    fun subscribe(@RequestBody subscription: Subscription) {
        logger.info("WebPush API: subscribe, subscription=${subscription}")
        subscriptions.put(subscription.endpoint, subscription)
    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(@RequestBody subscription: SubscriptionEndpoint) {
        subscriptions.remove(subscription.endpoint)
    }

    @PostMapping("/isSubscribed")
    fun isSubscribed(@RequestBody subscription: SubscriptionEndpoint): Boolean {
        return subscriptions.containsKey(subscription.endpoint)
    }

    @PostMapping("/broadcastMessage")
    fun sendMessageToAllUsers(@RequestBody notification: Notification): Boolean {
        val failedSubscriptions: MutableSet<String> = HashSet()
        for (subscription in subscriptions.values) {
            val remove: Boolean = pushNotificationService.sendNotification(subscription, notification)
            if (remove) {
                failedSubscriptions.add(subscription.endpoint)
            }
        }
        failedSubscriptions.forEach(Consumer { key: String? ->
            subscriptions.remove(
                key
            )
        })
        return true
    }

//    @PostMapping("/{appId}/{deviceType}/{token}")
//    fun sendNotification(
//        @PathVariable("appId") appId: Long,
//        @PathVariable("deviceType") deviceType: String?,
//        @PathVariable("token") token: String?,
//        @RequestBody notification: Notification?
//    ): CompletableFuture<*>? {
//        val certificatesContainer: CertificatesContainer = directoryServiceClient.getCertificatesContainer(appId)
//        logger.debug(
//            "App {}; Device {}; Token {}; Certificate {}; Notification {}",
//            appId,
//            deviceType,
//            token,
//            certificatesContainer,
//            notification
//        )
//        return sendersFactory.get(deviceType).apply(notification, certificatesContainer, token)
//    }
}