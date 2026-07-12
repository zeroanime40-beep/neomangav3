package tachiyomi.presentation.core.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.theme.NeoShapes

// NEO MANGA: Parametric Unification. MangaCard now dynamically supports both Compact and Comfortable layouts while enforcing strict glassmorphic performance budgets.
@Composable
fun MangaCard(
    title: String?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = true,
    coverBadgeStart: @Composable RowScope.() -> Unit = {},
    coverBadgeEnd: @Composable RowScope.() -> Unit = {},
    coverContent: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = NeoShapes.Shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        if (isCompact) {
            Box(modifier = Modifier.aspectRatio(2f / 3f).fillMaxSize()) {
                // Cover Image layer
                coverContent()

                if (!title.isNullOrEmpty()) {
                    // Dark gradient overlay to ensure text contrast at the bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
                                    ),
                                    startY = 0.5f // Gradient starts halfway down
                                )
                            )
                    )

                    // Title at the bottom
                    Text(
                        text = title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges
                Row(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                    coverBadgeStart()
                }
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    coverBadgeEnd()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.aspectRatio(2f / 3f).fillMaxWidth()) {
                    coverContent()
                    
                    // Badges
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                        coverBadgeStart()
                    }
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        coverBadgeEnd()
                    }
                }
                
                if (!title.isNullOrEmpty()) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
