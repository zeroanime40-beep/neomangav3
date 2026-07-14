package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.api.NeoMangaApiClient
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import rx.Observable
import rx.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NeoMangaVirtualSource : HttpSource() {
    override val id: Long = 9999L
    override val name: String = "NeoManga"
    override val lang: String = "ar"
    override val supportsLatest: Boolean = true
    
    override val baseUrl: String 
        get() = networkPreferences.neoMangaApiUrl.get()

    private val apiClient: NeoMangaApiClient by Injekt.injectLazy()
    private val networkPreferences: NetworkPreferences by Injekt.injectLazy()
    
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return Observable.unsafeCreate { subscriber ->
            val job = scope.launch {
                try {
                    val detailsDto = apiClient.getMangaDetails(manga.url)
                    manga.description = detailsDto.description
                    manga.genre = detailsDto.genres.joinToString(", ")
                    manga.initialized = true
                    
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(manga)
                        subscriber.onCompleted()
                    }
                } catch (t: Throwable) {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(t)
                    }
                }
            }
            subscriber.add(object : Subscription {
                override fun unsubscribe() = job.cancel()
                override fun isUnsubscribed(): Boolean = job.isCancelled
            })
        }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return Observable.unsafeCreate { subscriber ->
            val job = scope.launch {
                try {
                    val detailsDto = apiClient.getMangaDetails(manga.url)
                    val sChapters = detailsDto.chapters.map { chapterDto ->
                        SChapter.create().apply {
                            this.url = chapterDto.url
                            this.name = chapterDto.title
                            this.chapter_number = chapterDto.chapter_number
                            this.date_upload = System.currentTimeMillis()
                        }
                    }
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(sChapters)
                        subscriber.onCompleted()
                    }
                } catch (t: Throwable) {
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onError(t)
                    }
                }
            }
            subscriber.add(object : Subscription {
                override fun unsubscribe() = job.cancel()
                override fun isUnsubscribed(): Boolean = job.isCancelled
            })
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val pagesResponse = apiClient.getChapterPages(chapter.url)
        return pagesResponse.pages.mapIndexed { index, imageUrl ->
            Page(index, "", imageUrl)
        }
    }

    override fun imageRequest(page: Page): Request {
        val imageUrl = page.imageUrl ?: throw Exception("Page image URL is missing")
        val httpUrl = imageUrl.toHttpUrl()
        val refererValue = "${httpUrl.scheme}://${httpUrl.host}/"
        val newHeaders = headersBuilder()
            .set("Referer", refererValue)
            .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        return Request.Builder()
            .url(imageUrl)
            .headers(newHeaders)
            .build()
    }

    override suspend fun getPopularManga(page: Int): MangasPage = throw UnsupportedOperationException()
    override suspend fun getLatestUpdates(page: Int): MangasPage = throw UnsupportedOperationException()
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = throw UnsupportedOperationException()
}
