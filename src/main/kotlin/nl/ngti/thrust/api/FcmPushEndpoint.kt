package nl.ngti.thrust.api

import nl.ngti.thrust.model.FcmToken
import nl.ngti.thrust.model.Notification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import nl.ngti.thrust.service.fcmpush.FcmPushService
import java.util.HashSet

//import reactor.core.publisher.Mono

@RestController()
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/thrust/fcmpush")
class FcmPushEndpoint {
    var logger: Logger = LoggerFactory.getLogger(FcmPushEndpoint::class.java)

    @Autowired
    lateinit var fcmPushService: FcmPushService

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody token: FcmToken) {
        logger.info("FCM push API register, token=${token}")
        fcmPushService.fcmSubscribe(token.token)
    }

    @PostMapping("/pushMessage")
    fun sendMessageToAllUsers(@RequestBody notification: Notification): Boolean {
        fcmPushService.sendPushMessage(notification)
        return true
    }
}