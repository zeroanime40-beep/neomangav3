package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import tachiyomi.presentation.core.components.OledScaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.i18n.MR

data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        requestOpenSettingsSheet()
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { LibraryScreenModel() }
        val state by screenModel.state.collectAsState()

        val pitchBlack = Color(0xFF000000)
        val cyberTeal = Color(0xFF00E5FF)
        val cyberTealTranslucent = cyberTeal.copy(alpha = 0.15f)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            OledScaffold(
                topBar = {
                    AppBar(
                        title = "محفوظاتي",
                        navigateUp = null
                    )
                }
            ) { contentPadding ->
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = cyberTeal)
                        }
                    }
                    state.isEmpty -> {
                        MahfouzatiEmptyState(contentPadding, cyberTeal)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(pitchBlack),
                            contentPadding = contentPadding
                        ) {
                            items(state.items, key = { it.mangaId }) { item ->
                                MahfouzatiBannerCard(item, cyberTeal, cyberTealTranslucent, navigator)
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            HomeScreen.showBottomNav(true)
            launch { queryEvent.receiveAsFlow().collect {} }
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest {} }
        }
    }

    @Composable
    private fun MahfouzatiEmptyState(contentPadding: PaddingValues, cyberTeal: Color) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = cyberTeal,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "هنا بتتحط القصص أو المنهوات اللي أنت مهتم بيها",
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أضف مانجا إلى مفضلتك من شاشة التصفح",
                    color = Color.White.copy(alpha = 0.25f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun MahfouzatiBannerCard(
        item: MahfouzatiUiModel,
        cyberTeal: Color,
        cyberTealTranslucent: Color,
        navigator: Navigator
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(200.dp)
                .clickable { navigator.push(MangaScreen(item.mangaId)) },
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cyberTealTranslucent)
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

                // Background Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.98f)),
                                startY = 80f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    // Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.unreadCount > 0) {
                            BadgeText("${item.unreadCount} جديد", cyberTealTranslucent, cyberTeal)
                        }
                        BadgeText("${item.totalChapters} فصل", cyberTealTranslucent, cyberTeal)
                        BadgeText(item.genre ?: "مانجا", cyberTealTranslucent, cyberTeal)
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Title
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = item.readProgress,
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = cyberTeal,
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(item.readProgress * 100).toInt()}%",
                            color = cyberTeal,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
}
