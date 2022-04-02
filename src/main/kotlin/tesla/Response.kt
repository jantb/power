package tesla

data class Response(
    val battery_level: Int,
    val charging_state: String,
)