package eu.kanade.tachiyomi.ui.recommendations

import android.util.Log
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.source.interactor.GetUnifiedGlobalCatalogUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.core.common.util.lang.launchIO
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicBoolean

class RecommendationsScreenModel(
    private val getUnifiedGlobalCatalogUseCase: GetUnifiedGlobalCatalogUseCase = Injekt.get(),
) : StateScreenModel<RecommendationsScreenModel.State>(State()) {

    private val isFetchingPage = AtomicBoolean(false)
    private val cacheMutex = Mutex()
    private var windowStart = 0
    private val masterCache = mutableListOf<RecommendationsUiModel>()

    init {
        screenModelScope.launchIO {
            val cachedMangas = getUnifiedGlobalCatalogUseCase.getRawPool()
            Log.d("NEO_RECOMMENDATIONS", "Cache pool check size: ${cachedMangas.size}")
            
            if (cachedMangas.isNotEmpty()) {
                val initialUiModels = cachedMangas.map { manga ->
                    RecommendationsUiModel(
                        mangaId = manga.id,
                        title = manga.title,
                        thumbnailUrl = manga.thumbnailUrl,
                        status = when (manga.status.toInt()) {
                            1 -> "مستمر"
                            2 -> "مكتمل"
                            3 -> "مستمر"
                            4 -> "متوقف"
                            else -> null
                        },
                        genre = manga.genre?.firstOrNull(),
                    )
                }
                
                cacheMutex.withLock {
                    masterCache.addAll(initialUiModels)
                }
                Log.d("NEO_RECOMMENDATIONS", "masterCache size after init cache: ${masterCache.size}")
                
                val itemsToEmit = masterCache.take(60)
                Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (cache), items count: ${itemsToEmit.size}")
                
                mutableState.update {
                    it.copy(
                        items = itemsToEmit,
                        isLoading = false,
                        windowStart = 0,
                        isInitialHydrationComplete = cachedMangas.size >= 60
                    )
                }

                // Case A (Partial Cache Hit): If cache contains items but less than 60, prefetch Pages 2-3
                if (cachedMangas.size < 60) {
                    Log.d("NEO_RECOMMENDATIONS", "Partial Cache Hit: masterCache size ${cachedMangas.size} is less than 60. Prefetching pages 2-3.")
                    try {
                        getUnifiedGlobalCatalogUseCase.fetchCatalogPagesBulk(2, 2)
                            .collectLatest { networkMangas ->
                                Log.d("NEO_RECOMMENDATIONS", "Prefetch for pages 2-3 returned: ${networkMangas.size} items")
                                
                                val uiModels = networkMangas.map { manga ->
                                    RecommendationsUiModel(
                                        mangaId = manga.id,
                                        title = manga.title,
                                        thumbnailUrl = manga.thumbnailUrl,
                                        status = when (manga.status.toInt()) {
                                            1 -> "مستمر"
                                            2 -> "مكتمل"
                                            3 -> "مستمر"
                                            4 -> "متوقف"
                                            else -> null
                                        },
                                        genre = manga.genre?.firstOrNull(),
                                    )
                                }
                                cacheMutex.withLock {
                                    val uniqueNewUiModels = uiModels.filter { newItem ->
                                        masterCache.none { it.mangaId == newItem.mangaId }
                                    }
                                    masterCache.addAll(uniqueNewUiModels)
                                }
                                Log.d("NEO_RECOMMENDATIONS", "masterCache size after partial prefetch: ${masterCache.size}")
                                
                                val updatedEmit = masterCache.take(60)
                                Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (partial prefetch), items count: ${updatedEmit.size}")
                                
                                mutableState.update {
                                    it.copy(
                                        items = updatedEmit,
                                        windowStart = 0,
                                        isInitialHydrationComplete = true
                                    )
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("NEO_RECOMMENDATIONS", "Partial cache prefetch error", e)
                        mutableState.update { it.copy(isInitialHydrationComplete = true) }
                    }
                }
            } else {
                // Case B (Total Cache Miss): Fetch pages 1-3 in parallel
                Log.d("NEO_RECOMMENDATIONS", "Total Cache Miss: Fallback fetch triggered for pages 1-3")
                try {
                    getUnifiedGlobalCatalogUseCase.fetchCatalogPagesBulk(1, 3)
                        .collectLatest { networkMangas ->
                            Log.d("NEO_RECOMMENDATIONS", "Fallback fetch returned: ${networkMangas.size} items")
                            
                            val uiModels = networkMangas.map { manga ->
                                RecommendationsUiModel(
                                    mangaId = manga.id,
                                    title = manga.title,
                                    thumbnailUrl = manga.thumbnailUrl,
                                    status = when (manga.status.toInt()) {
                                        1 -> "مستمر"
                                        2 -> "مكتمل"
                                        3 -> "مستمر"
                                        4 -> "متوقف"
                                        else -> null
                                    },
                                    genre = manga.genre?.firstOrNull(),
                                )
                            }
                            cacheMutex.withLock {
                                masterCache.addAll(uiModels)
                            }
                            Log.d("NEO_RECOMMENDATIONS", "masterCache size after fallback fetch: ${masterCache.size}")
                            
                            val itemsToEmit = masterCache.take(60)
                            Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (fallback), items count: ${itemsToEmit.size}")
                            
                            mutableState.update {
                                it.copy(
                                    items = itemsToEmit,
                                    isLoading = false,
                                    windowStart = 0,
                                    isInitialHydrationComplete = true
                                )
                            }
                        }
                } catch (e: Exception) {
                    Log.e("NEO_RECOMMENDATIONS", "Fallback fetch error", e)
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            isInitialHydrationComplete = true
                        )
                    }
                }
            }
        }
    }

    fun shiftWindowDown() {
        screenModelScope.launchIO {
            // 1. Shift UI viewport instantly if items exist in cache
            cacheMutex.withLock {
                if (masterCache.size > 60 && windowStart + 60 < masterCache.size) {
                    windowStart = minOf(windowStart + 20, masterCache.size - 60)
                    val viewportItems = masterCache.subList(windowStart, windowStart + 60).toList()
                    Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (shift down instant), items count: ${viewportItems.size}")
                    
                    mutableState.update {
                        it.copy(
                            items = viewportItems,
                            windowStart = windowStart
                        )
                    }
                }
            }

            // 2. Prefetch next pages in the background if we approach the end of masterCache
            val cacheSize = cacheMutex.withLock { masterCache.size }
            if (windowStart + 60 >= cacheSize - 20 && !state.value.hasReachedEnd) {
                if (isFetchingPage.compareAndSet(false, true)) {
                    mutableState.update { it.copy(isSecondaryLoading = true) }
                    val currentFetchPage = (cacheSize / 20) + 1
                    try {
                        getUnifiedGlobalCatalogUseCase.fetchCatalogPagesBulk(currentFetchPage, 3)
                            .collectLatest { networkMangas ->
                                if (networkMangas.isEmpty()) {
                                    mutableState.update {
                                        it.copy(
                                            isSecondaryLoading = false,
                                            hasReachedEnd = true
                                        )
                                    }
                                    isFetchingPage.set(false)
                                    return@collectLatest
                                }

                                val newUiModels = networkMangas.map { manga ->
                                    RecommendationsUiModel(
                                        mangaId = manga.id,
                                        title = manga.title,
                                        thumbnailUrl = manga.thumbnailUrl,
                                        status = when (manga.status.toInt()) {
                                            1 -> "مستمر"
                                            2 -> "مكتمل"
                                            3 -> "مستمر"
                                            4 -> "متوقف"
                                            else -> null
                                        },
                                        genre = manga.genre?.firstOrNull(),
                                    )
                                }

                                cacheMutex.withLock {
                                    val uniqueNewUiModels = newUiModels.filter { newItem ->
                                        masterCache.none { it.mangaId == newItem.mangaId }
                                    }
                                    masterCache.addAll(uniqueNewUiModels)
                                }
                                
                                val finalCacheSize = cacheMutex.withLock { masterCache.size }
                                Log.d("NEO_RECOMMENDATIONS", "masterCache size after shift down fetch: $finalCacheSize")

                                val viewportItems = cacheMutex.withLock {
                                    masterCache.subList(windowStart, minOf(windowStart + 60, masterCache.size)).toList()
                                }
                                Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (shift down fetch), items count: ${viewportItems.size}")
                                
                                mutableState.update { currentState ->
                                    currentState.copy(
                                        items = viewportItems,
                                        isSecondaryLoading = false,
                                        hasReachedEnd = networkMangas.size < 15
                                    )
                                }
                                isFetchingPage.set(false)
                            }
                    } catch (e: Exception) {
                        Log.e("NEO_RECOMMENDATIONS", "Shift down fetch error", e)
                        mutableState.update { it.copy(isSecondaryLoading = false) }
                        isFetchingPage.set(false)
                    }
                }
            }
        }
    }

    fun shiftWindowUp() {
        screenModelScope.launchIO {
            cacheMutex.withLock {
                if (windowStart > 0) {
                    windowStart = maxOf(0, windowStart - 20)
                    val viewportItems = masterCache.subList(windowStart, minOf(windowStart + 60, masterCache.size)).toList()
                    Log.d("NEO_RECOMMENDATIONS", "Emitting to UI layout (shift up), items count: ${viewportItems.size}")
                    
                    mutableState.update {
                        it.copy(
                            items = viewportItems,
                            windowStart = windowStart
                        )
                    }
                }
            }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = true,
        val isSecondaryLoading: Boolean = false,
        val hasReachedEnd: Boolean = false,
        val items: List<RecommendationsUiModel> = emptyList(),
        val windowStart: Int = 0,
        val isInitialHydrationComplete: Boolean = false,
    )
}
