import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import tesla.Response
import tesla.Tesla
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Logger

class Main : BackgroundFunction<PubSubMessage> {
    private val tibberAuthToken = System.getenv("TIBBER")
    private val refreshToken =System.getenv("TESLA_REFRESH")
    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val client = HttpClient.newBuilder().build()

    override fun accept(p0: PubSubMessage, p1: Context) {
        val (overLimit, change) = getOverLimit(0.30)
        if (change) {
            val response = getCharge()
            val charge = response.battery_level

            when {
                charge < 20 -> {
                    startCharge(response.charging_state)
                }
                charge < 80 -> {
                    if (overLimit) stopCharge(response.charging_state) else startCharge(response.charging_state)
                }
            }
        }
    }


    private fun getCharge(): Response {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://tesla-info.com/api/control_v2.php?refresh=$refreshToken&request=get_charge"))
            .header("Accept", "application/json")
            .GET(
            )
            .build()
        val r = client.send(request, HttpResponse.BodyHandlers.ofString())
        val message = r.body()
        val tesla = objectMapper.readValue<Tesla>(message)
        return tesla.response
    }

    private fun startCharge(chargingState: String) {
        if (chargingState == "Stopped") {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://tesla-info.com/api/control_v2.php?refresh=$refreshToken&request=charge"))
                .header("Accept", "application/json")
                .GET(
                )
                .build()
            log.info("start charge")
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
    }

    private fun stopCharge(chargingState: String) {
        if (chargingState == "Charging") {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://tesla-info.com/api/control_v2.php?refresh=$refreshToken&request=stopcharge"))
                .header("Accept", "application/json")
                .GET(
                )
                .build()
            log.info("stop charge")
            val r = client.send(request, HttpResponse.BodyHandlers.ofString())
            println(r.body())
        }
    }

    private fun getOverLimit(d: Double): Pair<Boolean, Boolean> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.tibber.com/v1-beta/gql"))
            .header("Authorization", "Bearer $tibberAuthToken")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper
                        .writeValueAsString(GraphQl())
                )
            )
            .build()
        val r = client.send(request, HttpResponse.BodyHandlers.ofString())
        val message = r.body()
        val priceInfo = objectMapper.readValue<Tibber>(message).data.viewer.homes[0].currentSubscription.priceInfo

        val current =
            priceInfo.current.total
        val startsAt = priceInfo.current.startsAt


        for ((index, today) in priceInfo.today.withIndex()) {
            if (today.startsAt == startsAt) {
                val prev = priceInfo.today[if (index == 0) 0 else index - 1]
                val prevOverLimit = calcOverLimit(priceInfo = priceInfo, current = prev.total, d = d)
                val currentOverLimit = calcOverLimit(priceInfo = priceInfo, current = today.total, d = d)
                if (prevOverLimit != currentOverLimit) {
                    log.info("Over limit for this hour")
                    return Pair(currentOverLimit, true)
                }
            }
        }
        log.info("Under limit for this hour")
        return Pair(priceInfo.today.count { it.total >= current }
            .toDouble()
            .div(24) <= d, false)
    }

    private fun calcOverLimit(priceInfo: PriceInfo, current: Double, d: Double): Boolean {
        return priceInfo.today.count { it.total >= current }
            .toDouble()
            .div(24) <= d
    }
    companion object {
        private val log = Logger.getLogger(Main::class.java.name)
    }
}

data class PubSubMessage(
    val data: String,
    val messageId: String,
    val publishTime: String,
    val attributes: Map<String, String>,
)