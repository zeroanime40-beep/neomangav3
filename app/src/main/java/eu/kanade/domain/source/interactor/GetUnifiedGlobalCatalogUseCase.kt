package eu.kanade.domain.source.interactor

import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference

class GetUnifiedGlobalCatalogUseCase(
    private val getEnabledSources: GetEnabledSources,
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
) {
    enum class GlobalSortType {
        POPULAR, LATEST, RATING
    }

    private val cachedPriorityMangas = AtomicReference<List<Manga>?>(null)
    private val rawPool = AtomicReference<List<Manga>?>(null)
    private val loadMutex = Mutex()

    suspend fun ensurePriorityCatalogLoaded(prioritySourceName: String): List<Manga> = withContext(Dispatchers.IO) {
        val current = rawPool.get()
        if (current != null) return@withContext current

        loadMutex.withLock {
            val doubleCheck = rawPool.get()
            if (doubleCheck != null) return@withLock doubleCheck

            var prioritySource: CatalogueSource? = null
            var attempts = 0
            while (prioritySource == null && attempts < 3) {
                val enabledSources = withTimeoutOrNull(5000L) {
                    getEnabledSources.subscribe().first()
                } ?: emptyList()

                prioritySource = enabledSources
                    .mapNotNull { sourceManager.get(it.id) as? CatalogueSource }
                    .firstOrNull { it.name.equals(prioritySourceName, ignoreCase = true) }

                if (prioritySource == null) {
                    attempts++
                    if (attempts < 3) {
                        kotlinx.coroutines.delay(1000L)
                    }
                }
            }

            if (prioritySource == null) {
                val enabledSources = getEnabledSources.subscribe().first()
                prioritySource = enabledSources
                    .mapNotNull { sourceManager.get(it.id) as? CatalogueSource }
                    .firstOrNull()
            }

            if (prioritySource != null) {
                try {
                    val networkMangas1 = prioritySource.getPopularManga(1).mangas
                    val networkMangas2 = prioritySource.getPopularManga(2).mangas
                    val networkMangas3 = prioritySource.getPopularManga(3).mangas
                    
                    val domainMangas1 = networkMangas1.map { it.toDomainManga(prioritySource.id) }
                    val domainMangas2 = networkMangas2.map { it.toDomainManga(prioritySource.id) }
                    val domainMangas3 = networkMangas3.map { it.toDomainManga(prioritySource.id) }
                    
                    val persistentMangas1 = networkToLocalManga.invoke(domainMangas1)
                    val persistentMangas2 = networkToLocalManga.invoke(domainMangas2)
                    val persistentMangas3 = networkToLocalManga.invoke(domainMangas3)
                    
                    val combined = persistentMangas1 + persistentMangas2 + persistentMangas3
                    rawPool.set(combined)
                    cachedPriorityMangas.set(persistentMangas1)
                    combined
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun getShuffledDisplayLists(): Pair<List<Manga>, List<Manga>>? {
        val pool = rawPool.get() ?: cachedPriorityMangas.get() ?: return null
        if (pool.isEmpty()) return null
        val shuffled = pool.shuffled()
        return Pair(shuffled.take(5), shuffled.drop(5).take(10))
    }

    fun await(page: Int, sortType: GlobalSortType = GlobalSortType.POPULAR): Flow<List<Manga>> = flow {
        // 1. Immediately emit cached priority catalog on page 1 for POPULAR
        if (page == 1 && sortType == GlobalSortType.POPULAR) {
            val cache = cachedPriorityMangas.get()
            if (!cache.isNullOrEmpty()) {
                emit(cache)
                return@flow
            }
        }
        
        // 2. Cache is empty or it's another page/sort. Fallback to direct network fetch.
        try {
            val prioritySourceName = "Team X"
            var prioritySource: CatalogueSource? = null
            var attempts = 0
            while (prioritySource == null && attempts < 3) {
                val enabledSources = withTimeoutOrNull(5000L) {
                    getEnabledSources.subscribe().first()
                } ?: emptyList()

                prioritySource = enabledSources
                    .mapNotNull { sourceManager.get(it.id) as? CatalogueSource }
                    .firstOrNull { it.name.equals(prioritySourceName, ignoreCase = true) }

                if (prioritySource == null) {
                    attempts++
                    if (attempts < 3) {
                        kotlinx.coroutines.delay(1000L)
                    }
                }
            }

            if (prioritySource == null) {
                val enabledSources = getEnabledSources.subscribe().first()
                prioritySource = enabledSources
                    .mapNotNull { sourceManager.get(it.id) as? CatalogueSource }
                    .firstOrNull()
            }

            if (prioritySource != null) {
                val networkMangas = when (sortType) {
                    GlobalSortType.LATEST -> prioritySource.getLatestUpdates(page).mangas
                    else -> prioritySource.getPopularManga(page).mangas
                }
                
                val domainMangas = networkMangas.map { it.toDomainManga(prioritySource.id) }
                val persistentMangas = networkToLocalManga.invoke(domainMangas)
                
                if (page == 1 && sortType == GlobalSortType.POPULAR) {
                    cachedPriorityMangas.set(persistentMangas)
                    if (rawPool.get() == null) {
                        rawPool.set(persistentMangas)
                    }
                }
                
                emit(persistentMangas)
            } else {
                emit(emptyList()) // No source found, emit empty gracefully
            }
        } catch (e: Exception) {
            emit(emptyList()) // Catch errors and emit empty list to stop spinner
        }
    }

    fun getRawPool(): List<Manga> {
        return rawPool.get() ?: cachedPriorityMangas.get() ?: emptyList()
    }

    fun fetchCatalogPage(page: Int): Flow<List<Manga>> {
        return await(page, GlobalSortType.POPULAR)
    }

    fun fetchCatalogPagesBulk(startPage: Int, count: Int): Flow<List<Manga>> = flow {
        val pagesToFetch = (startPage until startPage + count).toList()
        val results = kotlinx.coroutines.coroutineScope {
            pagesToFetch.map { page ->
                async(Dispatchers.IO) {
                    try {
                        await(page, GlobalSortType.POPULAR).first()
                    } catch (e: Exception) {
                        emptyList<Manga>()
                    }
                }
            }.awaitAll()
        }
        val mergedList = results.flatten().distinctBy { it.id }
        emit(mergedList)
    }
}
