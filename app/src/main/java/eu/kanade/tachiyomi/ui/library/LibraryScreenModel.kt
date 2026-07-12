package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.manga.interactor.GetLibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryScreenModel(
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
) : StateScreenModel<LibraryScreenModel.State>(State()) {

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<MahfouzatiUiModel> = emptyList()
    ) {
        val isEmpty: Boolean = items.isEmpty()
    }

    init {
        screenModelScope.launchIO {
            getLibraryManga.subscribe()
                .collectLatest { libraryMangaList ->
                    val uiModels = libraryMangaList.map { lm ->
                        val chapters = getChaptersByMangaId.await(lm.manga.id)
                        val total = chapters.size
                        val readCount = chapters.count { it.read }
                        val progress = if (total > 0) readCount.toFloat() / total.toFloat() else 0f

                        MahfouzatiUiModel(
                            mangaId = lm.manga.id,
                            title = lm.manga.title,
                            thumbnailUrl = lm.manga.thumbnailUrl,
                            genre = lm.manga.genre?.firstOrNull(),
                            totalChapters = total,
                            readProgress = progress,
                            unreadCount = lm.unreadCount
                        )
                    }
                    mutableState.value = state.value.copy(
                        items = uiModels,
                        isLoading = false
                    )
                }
        }
    }
}
