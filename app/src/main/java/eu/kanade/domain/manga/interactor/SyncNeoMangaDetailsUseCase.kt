package eu.kanade.domain.manga.interactor

import eu.kanade.tachiyomi.network.api.NeoMangaApiClient
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.model.Chapter

class SyncNeoMangaDetailsUseCase(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val apiClient: NeoMangaApiClient,
) {
    suspend fun await(mangaId: Long, mangaUrl: String) {
        val detailsDto = apiClient.getMangaDetails(mangaUrl)
        
        val mappedStatus = when (detailsDto.status.lowercase()) {
            "ongoing" -> SManga.ONGOING.toLong()
            "completed" -> SManga.COMPLETED.toLong()
            else -> SManga.UNKNOWN.toLong()
        }

        val mangaUpdate = MangaUpdate(
            id = mangaId,
            description = detailsDto.description.trim(),
            genre = detailsDto.genres,
            status = mappedStatus,
            initialized = true
        )
        mangaRepository.update(mangaUpdate)

        val dbChapters = chapterRepository.getChapterByMangaId(mangaId)
        val progressMap = dbChapters.associate { 
            it.url to Triple(it.read, it.bookmark, it.lastPageRead) 
        }

        if (dbChapters.isNotEmpty()) {
            chapterRepository.removeChaptersWithIds(dbChapters.map { it.id })
        }

        try {
            val newChapters = detailsDto.chapters.mapIndexed { index, chapterDto ->
                val progress = progressMap[chapterDto.url]
                Chapter.create().copy(
                    mangaId = mangaId,
                    url = chapterDto.url,
                    name = chapterDto.title,
                    chapterNumber = chapterDto.chapter_number.toDouble(),
                    sourceOrder = index.toLong(),
                    read = progress?.first ?: false,
                    bookmark = progress?.second ?: false,
                    lastPageRead = progress?.third ?: 0L,
                    dateFetch = System.currentTimeMillis()
                )
            }
            chapterRepository.addAll(newChapters)
        } catch (e: Exception) {
            // Safety Rollback: Restore original chapters on failure to maintain database integrity
            if (dbChapters.isNotEmpty()) {
                chapterRepository.addAll(dbChapters)
            }
            throw e
        }
    }
}
