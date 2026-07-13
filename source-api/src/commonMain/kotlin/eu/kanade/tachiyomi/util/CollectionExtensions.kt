package eu.kanade.tachiyomi.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <T, R> Iterable<T>.mapParallel(
    transform: suspend (T) -> R
): List<R> {
    val list = if (this is List) this else this.toList()
    return if (list.size <= 10) {
        list.map { transform(it) }
    } else {
        coroutineScope {
            list.map { item ->
                async(Dispatchers.Default) {
                    transform(item)
                }
            }.awaitAll()
        }
    }
}
