package eu.kanade.tachiyomi.ui.recommendations

import androidx.compose.runtime.Immutable

@Immutable
data class RecommendationsUiModel(
    val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val status: String?,
    val genre: String?,
)
