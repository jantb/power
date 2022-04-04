package tibber

data class Current(
    val currency: String,
    val energy: Double,
    val startsAt: String,
    val tax: Double,
    val total: Double,
)