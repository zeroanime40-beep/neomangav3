package eu.kanade.presentation.globaldiscovery.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover

@Composable
fun GlobalMangaCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Generate Mihon's internal MangaCover object to automatically inject source HTTP headers
    val coverModel = MangaCover(
        mangaId = manga.id,
        sourceId = manga.source,
        isMangaFavorite = manga.favorite,
        url = manga.thumbnailUrl,
        lastModified = manga.coverLastModified
    )

    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000)
        ),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.15f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.aspectRatio(2f / 3f).fillMaxSize()) {
            AsyncImage(
                model = coverModel,
                contentDescription = manga.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay to ensure text contrast at the bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 0.5f
                        )
                    )
            )

            // Title at the bottom
            Text(
                text = manga.title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
