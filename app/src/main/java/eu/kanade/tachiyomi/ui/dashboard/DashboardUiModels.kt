package eu.kanade.tachiyomi.ui.dashboard

data class ContinueReadingUiModel(
    val mangaId: Long,
    val coverUrl: String?,
    val title: String,
    val currentChapterName: String,
    val nextChapterName: String?,
    val nextChapterId: Long?,
    val readProgress: Float,
)
