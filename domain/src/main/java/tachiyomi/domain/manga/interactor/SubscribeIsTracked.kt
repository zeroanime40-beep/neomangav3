package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.repository.MangaRepository

class SubscribeIsTracked(
    private val mangaRepository: MangaRepository,
) {
    fun subscribe(id: Long): Flow<Boolean> {
        return mangaRepository.subscribeIsTracked(id)
    }
}
