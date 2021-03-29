package nl.ngti.thrust.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

import java.time.Instant
import java.time.format.DateTimeFormatter

enum class Notification_Direction {
    auto, ltr, rtl
}

data class NotificationAction(
    val title: String,
    val action: String? = null,
    val icon: String? = null)

data class Notification(
    val title: String,
    val body: String,
    var dir: Notification_Direction? = null,
    val data: Any? = null,
    val actions: List<NotificationAction>? = null,
    val badge: String? = null,
    val icon: String? = null,
    var image: String? = null,
    var lang: String? = null,
    var renotify: Boolean? = null,
    var requireInteraction: Boolean? = null,
    var silent: Boolean? = null,
    var tag: String? = null,
    var vibrate: List<Int>? = null
) {
    @JsonProperty("@timestamp", defaultValue = "\${DateTimeFormatter.ISO_INSTANT.format(Instant.now()}")
    @SerializedName("@timestamp")
    var timestamp: String? = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
//    var timestamp // millis since 1970-01-01
//            : Long? = null
}