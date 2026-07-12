package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.Immutable

@Immutable
data class UpdatesTrackedUiModel(
    val mangaId: Long,
    val title: String,
    val thumbnailUrl: String?,
    val readProgress: Float,
    val unreadCount: Long
)
