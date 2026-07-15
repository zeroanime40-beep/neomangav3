package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class NeoMangaMasterExtension : HttpSource() {

    override val name: String = "Olympus Staff"
    override val id: Long = 1382165189279060276L
    override val baseUrl: String = "https://neomanga-api-server-beryl.vercel.app/api/v1"
    override val lang: String = "ar"
    override val supportsLatest: Boolean = true

    private val siteUrl = "https://olympustaff.com"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun getMangaUrl(manga: SManga): String {
        val url = manga.url
        return if (url.startsWith("http")) url else "$siteUrl$url"
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val url = chapter.url
        return if (url.startsWith("http")) url else "$siteUrl$url"
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        val url = "$baseUrl/manga/catalog".toHttpUrl().newBuilder()
            .addQueryParameter("site_url", siteUrl)
            .addQueryParameter("pages", page.toString())
            .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fetch catalog: ${response.code}")
        val body = response.body.string()
        val catalog = json.decodeFromString<CatalogResponse>(body)
        val mangas = catalog.items.map { item ->
            SManga.create().apply {
                title = item.title
                setUrlWithoutDomain(item.url)
                thumbnail_url = item.thumbnail
            }
        }
        val hasNextPage = mangas.size >= 10
        return MangasPage(mangas, hasNextPage)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val url = "$baseUrl/manga/latest".toHttpUrl().newBuilder()
            .addQueryParameter("site_url", siteUrl)
            .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fetch latest: ${response.code}")
        val body = response.body.string()
        val latest = json.decodeFromString<LatestResponse>(body)
        val mangas = latest.updates.map { item ->
            SManga.create().apply {
                title = item.title
                setUrlWithoutDomain(item.url)
                thumbnail_url = item.thumbnail
            }
        }
        val hasNextPage = mangas.size >= 10
        return MangasPage(mangas, hasNextPage)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        val mangasPage = getPopularManga(page)
        if (query.isBlank()) return mangasPage
        val filtered = mangasPage.mangas.filter { it.title.contains(query, ignoreCase = true) }
        return MangasPage(filtered, mangasPage.hasNextPage)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val url = "$baseUrl/manga/details".toHttpUrl().newBuilder()
            .addQueryParameter("manga_url", getMangaUrl(manga))
            .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fetch details: ${response.code}")
        val body = response.body.string()
        val details = json.decodeFromString<DetailsResponse>(body)

        val updatedManga = if (fetchDetails) {
            manga.copy().apply {
                description = details.description
                genre = details.genres.joinToString(", ")
                initialized = true
            }
        } else manga

        val updatedChapters = if (fetchChapters) {
            details.chapters.map { chapterItem ->
                SChapter.create().apply {
                        this.url = chapterItem.url
                    name = chapterItem.title
                    chapter_number = chapterItem.chapter_number
                    date_upload = System.currentTimeMillis()
                }
            }
        } else chapters

        return SMangaUpdate(updatedManga, updatedChapters)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val url = "$baseUrl/chapters/pages".toHttpUrl().newBuilder()
            .addQueryParameter("chapter_url", getChapterUrl(chapter))
            .build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to fetch pages: ${response.code}")
        val body = response.body.string()
        val pagesRes = json.decodeFromString<PagesResponse>(body)
        return pagesRes.pages.mapIndexed { index, imageUrl ->
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

    @Serializable
    data class LatestResponse(
        val status: String,
        val site_url: String,
        val count: Int,
        val updates: List<MangaItem>
    )

    @Serializable
    data class CatalogResponse(
        val status: String,
        val site_url: String,
        val pages_scraped: Int? = null,
        val count: Int,
        val items: List<MangaItem>
    )

    @Serializable
    data class MangaItem(
        val title: String,
        val url: String,
        val thumbnail: String,
        val latest_chapter: String? = null
    )

    @Serializable
    data class DetailsResponse(
        val status: String,
        val manga_url: String,
        val description: String,
        val genres: List<String>,
        val total_chapters: Int,
        val chapters: List<ChapterItem>
    )

    @Serializable
    data class ChapterItem(
        val title: String,
        val url: String,
        val chapter_number: Float
    )

    @Serializable
    data class PagesResponse(
        val status: String,
        val chapter_url: String,
        val total_pages: Int,
        val pages: List<String>
    )
}
