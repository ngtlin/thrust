package nl.ngti.thrust.service.fcmpush

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.json.Json
import org.springframework.stereotype.Service

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutionException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.TopicManagementResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import io.grpc.netty.shaded.io.netty.handler.codec.base64.Base64Encoder
import org.apache.tomcat.util.json.JSONParser
import org.apache.tomcat.util.json.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import javax.annotation.PostConstruct
import java.io.FileNotFoundException

import java.io.FileReader
import java.util.*
import java.io.FileWriter

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedReader





@Service
class FcmClient {

    val logger: Logger = LoggerFactory.getLogger(FcmClient::class.java)

    @Value("\${thrust.fcm.service-account-file}")
    private val serviceAccountFile: String? = null

    @Throws(InterruptedException::class, ExecutionException::class)
    fun send(data: Map<String, String>) {
        val message: Message = Message.builder().putAllData(data).setTopic("Scc")
            .setWebpushConfig(
                WebpushConfig.builder().putHeader("ttl", "300")
                    .setNotification(
                        WebpushNotification(
                            "Background Title (server)",
                            "Background Body (server)", "mail2.png"
                        )
                    )
                    .build()
            )
            .build()
        val response: String = FirebaseMessaging.getInstance().sendAsync(message).get()
        println("Sent message: $response")
    }

    fun subscribe(topic: String, clientToken: String) {
        try {
            val response: TopicManagementResponse = FirebaseMessaging.getInstance()
                .subscribeToTopicAsync(Collections.singletonList(clientToken), topic).get()
            println(response.getSuccessCount().toString() + " tokens were subscribed successfully")
        } catch (e: InterruptedException) {
            logger.error("subscribe", e)
        } catch (e: ExecutionException) {
            logger.error("subscribe", e)
        }
    }

    @PostConstruct
    fun init() {
        val serviceAccount ="{\"type\":\"service_account\",\"project_id\":\"climatechallenge-ae8c6\",\"private_key_id\":\"abd42aded42daf27544f6b9e4f76ba12fd8d2c06\",\"private_key\":\"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8JwB1aC15BsUQ\\n0EWQ77xWRx8lWcGvb/clwOzyaE5sYRqAp4tfY1pVbza8sgCbBN+0VtGkVu0Gaf2+\\nx6U+k2hA0Y/bltVglzGzVEFleKXuNwlhnCREW98g0svzQBs/TjER4rL94/YXOq5y\\nkRqLp2ikLbXn4kT8Vlccyb16wiSpoajRI9+jdejWh6vgLhYW2BndUAaAhMDc4xTI\\n3vPfCe7l8C4+FI1KN8YuMtQ2JXbDuQpFpoxmlAcCYBjnaW3rgWtnxP9cGbN9HMBS\\nMqySOpGThNyLkiQaj+TW8ON2h+jQIQOYP0UU0BSp6c++TrMfL7xyP+/mP7cgjWD8\\nkAty3xO/AgMBAAECggEAUkuY1sGHSi1c2prOIrPJI8+d3t9gaRTG+/tjaC+ZRaw/\\nRFCEFGfHZiDrxyLwQt2h3yYuMfxXVfvcIar/Ktyct+UxT/GsSMA42+AZboGTyhrG\\nfqwl7MRmVtv8mtvKTykrK1CFy6j+xXBERxYSj0zc5xqbOt5sIBa/qXbP6raqBItR\\neUaguZ4GBClFVHbXSFT/KClRpaFu75K515k6Am/Ao286F4WLI26iXi/KdeylN8uV\\nvg7vhTamgTpkagaBsRMfybo3t9q/HIiztr3cfiRxUu31OyV50fATVgWGxV4GtPUs\\n7jv0FPMXzzzclpzZVnYd8TRX6ZEPFD38jtrZD6lEEQKBgQDqqAQsLwQAGI4BHaoa\\nnNSkhlm6LA/lg2QnTrcREptA1dxF67rrTVND4TpHDTPE4GU4/eUYbADp98Itd4Ir\\nsIBkglbyRMPqTyAGIKYP+8jzzW/PO/LlgHNq1wy/llGY5iMmMcbl/w5pLnpWz4Y0\\n/ENX+5pMHEx2s83MGS32KKLyVwKBgQDNRCNxZp0MRqGNRdmDr9h4ZIF8UwACwIPa\\njM13L56Q4mXuh+JiKz7IZlUzwbi3JO85w6w4L6PBAttiYc+zsMQ8jbGylhXJ1D5m\\n3+Yxppvqx8I6DZvJ5CgzP9GXwyBi8hM12MajQRiM5DIzNw5LwVnThf8W06Gi/aYz\\nwj8VlISY2QKBgCpJMMdNiakCw8RLtjWpgWBMnarA+fRgIxI6Sx8WfUfHwjQPcNWq\\nT5Dx63CWXnfiABuvFTO7csa20RUkbh61Zc229zgdUDjPHXQ9Oe+zbdkRck6j/PeC\\nmEnuiuuTiQzGnBrr6W3U8qHdsEooh4TQ2MnoGzwP9W+Go1VeTDwJwqNtAoGBAJiu\\nkl6LoU28S/a1ejIF3tLOj7JWWq+xW2J5UWvSM/Q5MvDWioHTAQQ3ejDha/BuvLMO\\n5U91wCyVX3IypzhVx+Hy07avBM333eLPsH5GG7vrkoF0Fel6mJxmYBSuw6xVvAxV\\njotopEZIhz6pOS0ISTSZe0ETciu22KWvWxVGP8shAoGBAJjVfFQWpe1GlsCspun2\\nCXY9+qP0EEhJNE3ozDyscf9W3f5LVRe66WDsGquVUha+7jvrNSspPdArf9jQmawX\\nIxoBHE0pzHAb5PJSZ/pKFk/3OuH0itqOx6GZkQAbxeZGMpfBPRecgWW4Rbb+aW/o\\nkcM5tI+h3MEnNbwe69ZsKoNt\\n-----END PRIVATE KEY-----\\n\",\"client_email\":\"firebase-adminsdk-sgurf@climatechallenge-ae8c6.iam.gserviceaccount.com\",\"client_id\":\"104007920012277307447\",\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\",\"token_uri\":\"https://oauth2.googleapis.com/token\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\",\"client_x509_cert_url\":\"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-sgurf%40climatechallenge-ae8c6.iam.gserviceaccount.com\"}"
        val encodedString: String = Base64.getEncoder().encodeToString(serviceAccount.toByteArray())
        logger.debug("${encodedString}")

        val p: Path = Paths.get(serviceAccountFile)

        try {
            Files.newInputStream(p).use { serviceAccount ->
                val options: FirebaseOptions = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build()
                FirebaseApp.initializeApp(options)
            }
        } catch (e: IOException) {
            logger.error("init fcm", e)
        }
    }
}