package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository

class SetMangaTracking(
    private val mangaRepository: MangaRepository,
) {
    suspend fun await(id: Long, isTracked: Boolean) {
        mangaRepository.updateTracking(id, isTracked)
    }
}
