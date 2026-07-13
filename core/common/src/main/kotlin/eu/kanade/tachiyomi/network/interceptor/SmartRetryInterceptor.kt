package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SmartRetryInterceptor : Interceptor {

    private val hostUserAgents = ConcurrentHashMap<String, String>()

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Apply current session's User-Agent if already rotated
        val currentSessionUa = hostUserAgents[host]
        val initialRequest = if (currentSessionUa != null) {
            request.newBuilder()
                .removeHeader("User-Agent")
                .header("User-Agent", currentSessionUa)
                .build()
        } else {
            request
        }

        var response = chain.proceed(initialRequest)

        // 1. Rate Limit Handling (HTTP 429) with exponential backoff
        var attempt429 = 1
        while (response.code == 429 && attempt429 <= 3) {
            response.close()
            val backoff = (1L shl attempt429) * 1000L + Random.nextLong(0, 500)
            try {
                Thread.sleep(backoff)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            response = chain.proceed(initialRequest)
            attempt429++
        }

        // 2. Resilience Strategy (HTTP 503) with User-Agent rotation
        var attempt503 = 1
        var activeRequest = initialRequest
        while (response.code == 503 && attempt503 <= 2) {
            response.close()
            val currentUa = hostUserAgents[host] ?: activeRequest.header("User-Agent") ?: ""
            val nextUa = userAgents.filter { it != currentUa }.randomOrNull() ?: userAgents.random()
            hostUserAgents[host] = nextUa

            activeRequest = activeRequest.newBuilder()
                .removeHeader("User-Agent")
                .header("User-Agent", nextUa)
                .build()

            response = chain.proceed(activeRequest)
            attempt503++
        }

        return response
    }
}
