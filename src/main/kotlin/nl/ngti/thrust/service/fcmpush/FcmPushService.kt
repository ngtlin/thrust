package nl.ngti.thrust.service.fcmpush

import nl.ngti.thrust.model.IcndbJoke
import nl.ngti.thrust.model.Notification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
//import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.HashMap

import java.util.concurrent.ExecutionException


@Service
class FcmPushService(val fcmClient: FcmClient) {
    val logger: Logger = LoggerFactory.getLogger(FcmPushService::class.java)

    private var seq = 0

    fun fcmSubscribe(token: String) {
        fcmClient.subscribe("Scc", token)
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    fun sendPushMessage(notification: Notification) {
        val data: MutableMap<String, String> = HashMap()
        data["id"] = java.lang.String.valueOf(seq)
        data["joke"] = notification.body
        data["seq"] = java.lang.String.valueOf(this.seq++)
        data["ts"] = System.currentTimeMillis().toString()
        logger.debug("Sending chuck joke...")
        fcmClient.send(data)
    }

}