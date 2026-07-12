package eu.kanade.tachiyomi.ui.dashboard

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetUnifiedGlobalCatalogUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DashboardScreenModel(
    private val getHistory: GetHistory = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getGlobalCatalog: GetUnifiedGlobalCatalogUseCase = Injekt.get()
) : StateScreenModel<DashboardScreenModel.State>(State()) {

    data class State(
        val continueReading: ContinueReadingUiModel? = null,
        val featuredMangaList: List<Manga> = emptyList(),
        val recommendations: List<Manga> = emptyList(),
        val needsCatalogRefresh: Boolean = true,
        val isLoading: Boolean = false // Strict 0ms loading
    )

    fun refreshCatalog() {
        val lists = getGlobalCatalog.getShuffledDisplayLists()
        if (lists != null) {
            mutableState.value = state.value.copy(
                featuredMangaList = lists.first,
                recommendations = lists.second,
                needsCatalogRefresh = false
            )
        }
    }

    fun markNeedsRefresh() {
        mutableState.value = state.value.copy(needsCatalogRefresh = true)
    }

    init {
        // Immediate, reactive history stream
        screenModelScope.launch {
            getHistory.subscribe("")
                .distinctUntilChanged()
                .collectLatest { historyList ->
                    val latestHistory = historyList.firstOrNull()
                    var continueReadingModel: ContinueReadingUiModel? = null

                    if (latestHistory != null) {
                        val nextChapters = getNextChapters.await(latestHistory.mangaId, latestHistory.chapterId)
                        val nextChap = nextChapters.firstOrNull()

                        val allChapters = getChaptersByMangaId.await(latestHistory.mangaId, applyScanlatorFilter = true)
                        val total = allChapters.size
                        val readCount = allChapters.count { it.read }
                        val progress = if (total > 0) readCount.toFloat() / total.toFloat() else 0f

                        continueReadingModel = ContinueReadingUiModel(
                            mangaId = latestHistory.mangaId,
                            coverUrl = latestHistory.coverData.url,
                            title = latestHistory.title,
                            currentChapterName = "الفصل ${latestHistory.chapterNumber.toString().removeSuffix(".0")}",
                            nextChapterName = nextChap?.name,
                            nextChapterId = nextChap?.id,
                            readProgress = progress
                        )
                    }

                    mutableState.value = state.value.copy(
                        continueReading = continueReadingModel
                    )
                }
        }

        // Silent background hydration for Featured and Recommendations
        screenModelScope.launch {
            var attempts = 0
            var success = false
            while (!success && attempts < 3) {
                try {
                    // Exclusively pull from "Team X" catalog via GetUnifiedGlobalCatalogUseCase priority cache
                    getGlobalCatalog.preloadPriorityCatalog("Team X")
                    getGlobalCatalog.await(1).collect { cached ->
                        if (cached.isNotEmpty()) {
                            refreshCatalog()
                            success = true
                        }
                    }
                } catch (e: Exception) {
                    // Fail silently, preserving empty state for zero-blocking UI
                }

                if (!success) {
                    attempts++
                    if (attempts < 3) {
                        kotlinx.coroutines.delay(2000L)
                    }
                }
            }
        }
    }
}
