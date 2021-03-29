package nl.ngti.thrust.model


data class SubscriptionEndpoint(val endpoint: String)

data class SubscriptionKeys(val p256dh: String, val auth: String)

data class Subscription(val endpoint: String,
                        val keys: SubscriptionKeys,
                        val expirationTime: String?)