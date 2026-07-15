package eu.kanade.tachiyomi.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen

class DashboardScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current
        val screenModel = rememberScreenModel { DashboardScreenModel() }
        val state by screenModel.state.collectAsState()
        val context = LocalContext.current

        val pitchBlack = Color(0xFF000000)
        val cyberTeal = Color(0xFF00E5FF)
        val cyberTealTranslucent = cyberTeal.copy(alpha = 0.15f)

        LaunchedEffect(state.needsCatalogRefresh) {
            if (state.needsCatalogRefresh) {
                screenModel.refreshCatalog()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                screenModel.markNeedsRefresh()
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                topBar = {
                    AppBar(
                        title = "الرئيسية",
                        navigateUp = navigator::pop
                    )
                },
                containerColor = pitchBlack
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 1. Featured Manga Carousel (البانر العلوي)
                    item {
                        FeaturedMangaCarousel(state.featuredMangaList, cyberTeal, cyberTealTranslucent, navigator)
                    }

                    // 2. Quick Navigation Row (أزرار التنقل السريع)
                    item {
                        QuickNavigationRow(tabNavigator, cyberTeal, cyberTealTranslucent)
                    }

                    // 3. Continue Reading Hub (استكمل القراءة)
                    item {
                        state.continueReading?.let { model ->
                            ContinueReadingHub(
                                model = model,
                                cyberTeal = cyberTeal,
                                cyberTealTranslucent = cyberTealTranslucent,
                                onContinueClick = { mangaId, chapterId ->
                                    val intent = ReaderActivity.newIntent(context, mangaId, chapterId)
                                    context.startActivity(intent)
                                },
                                onDetailsClick = { mangaId ->
                                    navigator.push(MangaScreen(mangaId))
                                }
                            )
                        }
                    }

                    // 4. Manga Recommendations Section (اقتراحات الأعمال)
                    item {
                        RecommendationsSection(state.recommendations, state.prioritySourceId, cyberTeal, cyberTealTranslucent, navigator)
                    }
                    
                    // 5. Explore Other Sources Button
                    item {
                        ExploreOtherSourcesButton(tabNavigator, cyberTeal, cyberTealTranslucent)
                    }
                }
            }
        }
    }

    @Composable
    private fun ExploreOtherSourcesButton(
        tabNavigator: cafe.adriel.voyager.navigator.tab.TabNavigator,
        cyberTeal: Color,
        cyberTealTranslucent: Color
    ) {
        Button(
            onClick = { tabNavigator.current = eu.kanade.tachiyomi.ui.browse.BrowseTab },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, cyberTealTranslucent),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text("استكشاف باقي المصادر", color = cyberTeal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }

    @Composable
    private fun FeaturedMangaCarousel(
        featuredMangaList: List<tachiyomi.domain.manga.model.Manga>,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
        navigator: cafe.adriel.voyager.navigator.Navigator
    ) {
        if (featuredMangaList.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(260.dp),
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, cyberTealTranslucent)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = cyberTeal)
                }
            }
            return
        }

        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { featuredMangaList.size })

        Column(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                reverseLayout = false,
                pageSize = androidx.compose.foundation.pager.PageSize.Fill
            ) { page ->
                val featuredManga = featuredMangaList[page]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(260.dp)
                        .clickable { navigator.push(MangaScreen(featuredManga.id)) },
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, cyberTealTranslucent)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(featuredManga.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                            fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Background Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                                        startY = 150f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            // Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BadgeText("10.0 ★", cyberTealTranslucent, cyberTeal)
                                BadgeText("109 فصل", cyberTealTranslucent, cyberTeal)
                                BadgeText(featuredManga.genre?.firstOrNull() ?: "أكشن", cyberTealTranslucent, cyberTeal)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Title
                            Text(
                                text = featuredManga.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Action Button
                            Button(
                                onClick = { navigator.push(MangaScreen(featuredManga.id)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = cyberTeal),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("اقرأ الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Page Indicators
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) cyberTeal else Color.DarkGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun BadgeText(text: String, backgroundColor: Color, textColor: Color) {
        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = text, color = textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun QuickNavigationRow(
        tabNavigator: cafe.adriel.voyager.navigator.tab.TabNavigator,
        cyberTeal: Color,
        cyberTealTranslucent: Color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "محفوظاتي" to eu.kanade.tachiyomi.ui.library.LibraryTab,
                "حديثة" to eu.kanade.tachiyomi.ui.updates.UpdatesTab,
                "جديدة" to eu.kanade.tachiyomi.ui.browse.BrowseTab
            ).forEach { (label, tab) ->
                Button(
                    onClick = { tabNavigator.current = tab },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, cyberTealTranslucent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(label, color = cyberTeal, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(
        title: String,
        cyberTeal: Color,
        actionLabel: String = "عرض الكل",
        onActionClick: (() -> Unit)? = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (onActionClick != null) {
                Text(
                    text = actionLabel,
                    color = cyberTeal,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
    }

    @Composable
    private fun CoverWithBadge(
        coverUrl: String?,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
    ) {
        Box(
            modifier = Modifier
                .size(90.dp, 126.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(cyberTealTranslucent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "متابعة",
                    color = cyberTeal,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun ContinueReadingHub(
        model: ContinueReadingUiModel,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
        onContinueClick: (mangaId: Long, chapterId: Long) -> Unit,
        onDetailsClick: (mangaId: Long) -> Unit,
    ) {
        val tabNavigator = cafe.adriel.voyager.navigator.tab.LocalTabNavigator.current

        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = "استكمل القراءة",
                cyberTeal = cyberTeal,
                actionLabel = "عرض الكل >",
                onActionClick = {
                    tabNavigator.current = eu.kanade.tachiyomi.ui.history.HistoryTab
                }
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, cyberTealTranslucent),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverWithBadge(model.coverUrl, cyberTeal, cyberTealTranslucent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = model.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val progressText = if (model.nextChapterName != null) {
                            "الفصل الحالي: ${model.currentChapterName.removePrefix("الفصل ")}  |  التالي: ${model.nextChapterName}"
                        } else {
                            "لا يوجد فصول جديدة"
                        }
                        Text(
                            text = progressText,
                            color = if (model.nextChapterName != null) Color.LightGray else Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { model.readProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp),
                                color = cyberTeal,
                                trackColor = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${(model.readProgress * 100).toInt()}%",
                                color = cyberTeal,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onDetailsClick(model.mangaId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = cyberTeal
                                ),
                                border = BorderStroke(1.dp, cyberTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("التفاصيل", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { 
                                    if (model.nextChapterId != null) {
                                        onContinueClick(model.mangaId, model.nextChapterId)
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cyberTeal,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                enabled = model.nextChapterId != null
                            ) {
                                Text("متابعة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RecommendationsSection(
        recommendations: List<tachiyomi.domain.manga.model.Manga>,
        prioritySourceId: Long?,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
        navigator: cafe.adriel.voyager.navigator.Navigator
    ) {
        if (recommendations.isEmpty()) return

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            SectionHeader(
                title = "اقتراحات الأعمال",
                cyberTeal = cyberTeal,
                actionLabel = "عرض كل الاقتراحات",
                onActionClick = {
                    prioritySourceId?.let { sourceId ->
                        navigator.push(BrowseSourceScreen(sourceId, null))
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations.size) { index ->
                    val manga = recommendations[index]
                    Card(
                        modifier = Modifier
                            .size(100.dp, 150.dp)
                            .clickable { navigator.push(MangaScreen(manga.id)) },
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        border = BorderStroke(1.dp, Color(0x1F00E5FF))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(manga.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                            fallback = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
