package ua.syi.billing.library

import kotlinx.coroutines.delay
import java.io.IOException

internal object Utils {

    suspend fun <T> retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 300, // 0.3 second
        maxDelay: Long = 3000,    // 3 seconds
        factor: Double = 2.0,
        block: suspend () -> T): T
    {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: IOException) {
                println(e.message)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }

    fun extractDuration(value: String, regex: Regex): Int {
        val matchResults = regex.find(value)
        return matchResults?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    val DAYS_REGEX = Regex("(\\d*)D")
    val WEEKS_REGEX = Regex("(\\d*)W")
    val MONTH_REGEX = Regex("(\\d*)M")
    val YEAR_REGEX = Regex("(\\d*)Y")

}