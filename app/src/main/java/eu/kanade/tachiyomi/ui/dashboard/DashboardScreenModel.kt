package eu.kanade.tachiyomi.ui.dashboard

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetUnifiedGlobalCatalogUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.manga.model.Manga
import tachiyomi.core.common.util.system.logcat
import logcat.LogPriority
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
        val isLoading: Boolean = false, // Strict 0ms loading
        val prioritySourceId: Long? = null
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
        // Resolve target source ID for nativeCatalogue navigation
        screenModelScope.launch {
            try {
                val sourceId = getGlobalCatalog.getPrioritySourceId("Team X")
                mutableState.value = state.value.copy(prioritySourceId = sourceId)
            } catch (e: Exception) {
                // Ignore
            }
        }

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
        // Budget: 5 attempts × 3s delay = 15s total (covers Render.com cold-start)
        screenModelScope.launch {
            var attempts = 0
            var success = false
            while (!success && attempts < 5) {
                try {
                    // Pull from "Team X" catalog via GetUnifiedGlobalCatalogUseCase priority cache
                    getGlobalCatalog.ensurePriorityCatalogLoaded("Team X")

                    // Bounded fetch: 10s timeout, single emission, no hanging collect
                    val result = withTimeoutOrNull(10_000L) {
                        getGlobalCatalog.await(1).firstOrNull()
                    }

                    if (!result.isNullOrEmpty()) {
                        refreshCatalog()
                        success = true
                        logcat(LogPriority.INFO) { "Dashboard hydrated with ${result.size} manga" }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.WARN) { "Dashboard hydration attempt ${attempts + 1} failed: ${e.message}" }
                }

                if (!success) {
                    attempts++
                    if (attempts < 5) {
                        kotlinx.coroutines.delay(3000L)
                    }
                }
            }

            if (!success) {
                logcat(LogPriority.WARN) { "Dashboard hydration exhausted all 5 attempts" }
            }
        }
    }
}
