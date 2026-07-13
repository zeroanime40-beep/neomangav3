package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun Element.selectText(css: String, defaultValue: String? = null): String? {
    return select(css).first()?.text() ?: defaultValue
}

fun Element.selectInt(css: String, defaultValue: Int = 0): Int {
    return select(css).first()?.text()?.toInt() ?: defaultValue
}

fun Element.attrOrText(css: String): String {
    return if (css != "text") attr(css) else text()
}

object JsoupHtmlSanitizer {
    private val styleRegex = Regex("(?i)<style[^>]*?>[\\s\\S]*?</style>")
    private val svgRegex = Regex("(?i)<svg[^>]*?>[\\s\\S]*?</svg>")
    private val iframeRegex = Regex("(?i)<iframe[^>]*?>[\\s\\S]*?</iframe>")
    private val commentRegex = Regex("<!--[\\s\\S]*?-->")
    private val scriptRegex = Regex("(?i)<script([^>]*?)>([\\s\\S]*?)</script>")

    fun sanitize(html: String): String {
        var clean = html
        clean = styleRegex.replace(clean, "")
        clean = svgRegex.replace(clean, "")
        clean = iframeRegex.replace(clean, "")
        clean = commentRegex.replace(clean, "")
        clean = scriptRegex.replace(clean) { matchResult ->
            val attributes = matchResult.groups[1]?.value ?: ""
            if (attributes.contains("application/json", ignoreCase = true) ||
                attributes.contains("application/ld+json", ignoreCase = true)) {
                matchResult.value
            } else {
                ""
            }
        }
        return clean
    }
}

/**
 * Returns a Jsoup document for this response.
 * @param html the body of the response. Use only if the body was read before calling this method.
 */
fun Response.asJsoup(html: String? = null): Document {
    val rawHtml = html ?: body.string()
    val cleanHtml = JsoupHtmlSanitizer.sanitize(rawHtml)
    return runBlocking(Dispatchers.Default) {
        Jsoup.parse(cleanHtml, request.url.toString())
    }
}
