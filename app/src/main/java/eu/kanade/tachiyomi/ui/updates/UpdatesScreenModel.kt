package eu.kanade.tachiyomi.ui.updates

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.asState
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.SubscribeTrackedManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class UpdatesScreenModel(
    private val subscribeTrackedManga: SubscribeTrackedManga = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<UpdatesScreenModel.State>(State()) {

    val lastUpdated by libraryPreferences.lastUpdatedTimestamp.asState(screenModelScope)

    init {
        screenModelScope.launchIO {
            subscribeTrackedManga.subscribe()
                .collectLatest { trackedMangaList ->
                    val uiModels = trackedMangaList.map { manga ->
                        val chapters = getChaptersByMangaId.await(manga.id)
                        val total = chapters.size
                        val readCount = chapters.count { it.read }
                        val progress = if (total > 0) readCount.toFloat() / total.toFloat() else 0f
                        val unread = chapters.count { !it.read }.toLong()

                        UpdatesTrackedUiModel(
                            mangaId = manga.id,
                            title = manga.title,
                            thumbnailUrl = manga.thumbnailUrl,
                            readProgress = progress,
                            unreadCount = unread
                        )
                    }
                    mutableState.update {
                        it.copy(
                            items = uiModels,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun updateLibrary(context: Context): Boolean {
        val started = LibraryUpdateJob.startNow(context)
        return started
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val items: List<UpdatesTrackedUiModel> = emptyList(),
    )
}
