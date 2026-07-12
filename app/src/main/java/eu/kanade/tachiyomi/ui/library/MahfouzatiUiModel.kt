package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable

@Immutable
data class MahfouzatiUiModel(
    val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val genre: String?,
    val totalChapters: Int,
    val readProgress: Float,
    val unreadCount: Long
)
