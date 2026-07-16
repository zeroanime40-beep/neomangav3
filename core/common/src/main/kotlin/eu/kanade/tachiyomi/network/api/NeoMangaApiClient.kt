package eu.kanade.tachiyomi.network.api

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class MangaDto(
    val title: String,
    val url: String,
    val thumbnail: String
)

@Serializable
data class CatalogResponseDto(
    val status: String,
    val site_url: String,
    val page: Int,
    val count: Int,
    val items: List<MangaDto>
)

@Serializable
data class LatestResponseDto(
    val status: String,
    val site_url: String,
    val count: Int,
    val updates: List<MangaDto>
)

@Serializable
data class ChapterDto(
    val title: String,
    val url: String,
    val chapter_number: Float
)

@Serializable
data class MangaDetailsResponseDto(
    val status: String,
    val manga_url: String,
    val description: String,
    val genres: List<String>,
    val total_chapters: Int,
    val chapters: List<ChapterDto>
)

@Serializable
data class PagesResponseDto(
    val status: String,
    val chapter_url: String,
    val total_pages: Int,
    val pages: List<String>
)

interface NeoMangaApi {
    @GET("manga/latest")
    suspend fun getLatestManga(
        @Query("site_url") siteUrl: String
    ): LatestResponseDto

    @GET("manga/catalog")
    suspend fun getMangaCatalog(
        @Query("site_url") siteUrl: String,
        @Query("page") page: Int? = null,
        @Query("pages") pages: Int? = null
    ): CatalogResponseDto

    @GET("manga/details")
    suspend fun getMangaDetails(
        @Query("manga") mangaSlug: String
    ): MangaDetailsResponseDto

    @GET("chapters/pages")
    suspend fun getChapterPages(
        @Query("chapter_url") chapterUrl: String
    ): PagesResponseDto
}

class NeoMangaApiClient(
    private val networkHelper: NetworkHelper,
    private val networkPreferences: NetworkPreferences,
    private val json: Json
) {
    private var cachedUrl: String? = null
    private var apiInstance: NeoMangaApi? = null

    private fun getApi(): NeoMangaApi {
        val currentUrl = networkPreferences.neoMangaApiUrl.get()
        if (currentUrl != cachedUrl || apiInstance == null) {
            cachedUrl = currentUrl
            apiInstance = Retrofit.Builder()
                .baseUrl(currentUrl)
                .client(networkHelper.client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(NeoMangaApi::class.java)
        }
        return apiInstance!!
    }

    suspend fun getLatestManga(siteUrl: String) = getApi().getLatestManga(siteUrl)
    suspend fun getMangaCatalog(siteUrl: String, page: Int? = null, pages: Int? = null) = getApi().getMangaCatalog(siteUrl, page, pages)
    suspend fun getMangaDetails(mangaUrl: String): MangaDetailsResponseDto {
        val slug = mangaUrl.trimEnd('/').substringAfterLast('/')
        return getApi().getMangaDetails(slug)
    }
    suspend fun getChapterPages(chapterUrl: String) = getApi().getChapterPages(chapterUrl)
}
