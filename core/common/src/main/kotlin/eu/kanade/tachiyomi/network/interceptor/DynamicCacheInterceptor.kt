package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class DynamicCacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val hasQueryParams = !request.url.query.isNullOrEmpty()
        val cacheControlHeader = request.header("Cache-Control")
        val isManualRefresh = cacheControlHeader != null && 
            (cacheControlHeader.contains("no-cache") || cacheControlHeader.contains("no-store"))

        if (hasQueryParams || isManualRefresh) {
            return response
        }

        val contentType = response.body?.contentType()?.toString()
        val isHtml = contentType != null && contentType.contains("text/html", ignoreCase = true)

        if (isHtml) {
            return response.newBuilder()
                .removeHeader("Cache-Control")
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=600, stale-while-revalidate=3600")
                .build()
        }

        return response
    }
}
