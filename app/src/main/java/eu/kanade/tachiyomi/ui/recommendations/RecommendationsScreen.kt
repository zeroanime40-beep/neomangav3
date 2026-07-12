package eu.kanade.tachiyomi.ui.recommendations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.LoadingScreen

private enum class ScrollBoundary {
    NONE,
    TOP,
    BOTTOM
}

class RecommendationsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { RecommendationsScreenModel() }
        val state by screenModel.state.collectAsState()

        val cyberTeal = Color(0xFF00E5FF)
        val cyberTealTranslucent = Color(0x1F00E5FF)
        val shimmerTealTranslucent = Color(0x0A00E5FF)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                topBar = { scrollBehavior ->
                    AppBar(
                        title = "اقتراحات الأعمال",
                        navigateUp = { navigator.pop() },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                        .padding(contentPadding)
                ) {
                    if (state.isLoading) {
                        LoadingScreen()
                    } else {
                        val gridState = rememberLazyGridState()

                        // Monitor boundaries inside derivedStateOf to prevent infinite loops
                        val scrollBoundary = remember {
                            derivedStateOf {
                                val layoutInfo = gridState.layoutInfo
                                val totalItemsCount = layoutInfo.totalItemsCount
                                val visibleItems = layoutInfo.visibleItemsInfo
                                if (totalItemsCount > 0 && visibleItems.isNotEmpty()) {
                                    val firstVisible = visibleItems.first().index
                                    val lastVisible = visibleItems.last().index
                                    when {
                                        lastVisible >= totalItemsCount - 5 -> ScrollBoundary.BOTTOM
                                        firstVisible <= 5 -> ScrollBoundary.TOP
                                        else -> ScrollBoundary.NONE
                                    }
                                } else {
                                    ScrollBoundary.NONE
                                }
                            }
                        }

                        LaunchedEffect(scrollBoundary.value) {
                            when (scrollBoundary.value) {
                                ScrollBoundary.BOTTOM -> screenModel.shiftWindowDown()
                                ScrollBoundary.TOP -> screenModel.shiftWindowUp()
                                ScrollBoundary.NONE -> {}
                            }
                        }

                        // Jitter-free scroll position correction
                        val windowStart = state.windowStart
                        var prevWindowStart by remember { mutableStateOf(0) }

                        LaunchedEffect(windowStart) {
                            val diff = windowStart - prevWindowStart
                            if (diff != 0) {
                                val currentFirstVisibleItem = gridState.firstVisibleItemIndex
                                val targetIndex = currentFirstVisibleItem - diff
                                gridState.scrollToItem(
                                    index = maxOf(0, targetIndex),
                                    scrollOffset = gridState.firstVisibleItemScrollOffset
                                )
                            }
                            prevWindowStart = windowStart
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            state = gridState,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.items,
                                key = { it.mangaId }
                            ) { item ->
                                RecommendationGridCard(
                                    item = item,
                                    cyberTeal = cyberTeal,
                                    cyberTealTranslucent = cyberTealTranslucent,
                                    onClick = { navigator.push(MangaScreen(item.mangaId)) }
                                )
                            }

                            if (state.isSecondaryLoading) {
                                items(
                                    count = 3,
                                    span = { GridItemSpan(1) }
                                ) {
                                    RecommendationShimmerItem(shimmerTealTranslucent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecommendationGridCard(
        item: RecommendationsUiModel,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .padding(6.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cyberTealTranslucent),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                    fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom black gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                                startY = 120f
                            )
                        )
                )

                // Badges and Text Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    // Top Row for badges
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                    ) {
                        item.status?.let {
                            BadgeText(
                                text = it,
                                backgroundColor = Color.Black.copy(alpha = 0.6f),
                                textColor = Color.White
                            )
                        }
                        item.genre?.let {
                            BadgeText(
                                text = it,
                                backgroundColor = cyberTealTranslucent,
                                textColor = cyberTeal
                            )
                        }
                    }

                    // Title at bottom
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    @Composable
    private fun RecommendationShimmerItem(borderColor: Color) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .padding(6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp
                )
            }
        }
    }

    @Composable
    private fun BadgeText(
        text: String,
        backgroundColor: Color,
        textColor: Color,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
