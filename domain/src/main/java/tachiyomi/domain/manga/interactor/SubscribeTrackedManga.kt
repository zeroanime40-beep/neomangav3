package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class SubscribeTrackedManga(
    private val mangaRepository: MangaRepository,
) {
    fun subscribe(): Flow<List<Manga>> {
        return mangaRepository.subscribeTrackedManga()
    }
}
