package eu.kanade.presentation.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import eu.kanade.tachiyomi.ui.updates.UpdatesTrackedUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesScreenModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    onClickCard: (Long) -> Unit,
    onUpdateLibrary: () -> Boolean,
) {
    val cyberTeal = Color(0xFF00E5FF)
    val cyberTealTranslucent = Color(0x3300E5FF)

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_recent_updates),
                actions = {
                    AppBarActions(
                        listOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_update_library),
                                icon = Icons.Outlined.Sync,
                                onClick = { onUpdateLibrary() },
                            )
                        )
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(contentPadding)
        ) {
            when {
                state.isLoading -> LoadingScreen()
                state.items.isEmpty() -> {
                    EmptyScreen(
                        stringRes = MR.strings.information_no_recent,
                    )
                }
                else -> {
                    val scope = rememberCoroutineScope()
                    var isRefreshing by remember { mutableStateOf(false) }

                    PullRefresh(
                        refreshing = isRefreshing,
                        onRefresh = {
                            val started = onUpdateLibrary()
                            if (!started) return@PullRefresh
                            scope.launch {
                                isRefreshing = true
                                delay(1.seconds)
                                isRefreshing = false
                            }
                        },
                        enabled = true,
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                        ) {
                            items(
                                items = state.items,
                                key = { it.mangaId }
                            ) { item ->
                                UpdatesTrackedCard(
                                    item = item,
                                    cyberTeal = cyberTeal,
                                    cyberTealTranslucent = cyberTealTranslucent,
                                    onClick = onClickCard
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesTrackedCard(
    item: UpdatesTrackedUiModel,
    cyberTeal: Color,
    cyberTealTranslucent: Color,
    onClick: (Long) -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(180.dp)
                .clickable { onClick(item.mangaId) },
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

                // Cyber-Teal LinearProgressIndicator displaying the reading progress percentage over the card surface
                LinearProgressIndicator(
                    progress = { item.readProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(6.dp),
                    color = cyberTeal,
                    trackColor = Color.Transparent,
                )

                // Content Column (Right-aligned / RTL scope)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right-aligned permanent badge: "جاري التتبع"
                        BadgeText(
                            text = "جاري التتبع",
                            backgroundColor = cyberTealTranslucent,
                            textColor = cyberTeal
                        )

                        // Left-aligned badge indicating unread chapters count (if any)
                        if (item.unreadCount > 0) {
                            BadgeText(
                                text = "${item.unreadCount} جديد",
                                backgroundColor = Color.Red.copy(alpha = 0.2f),
                                textColor = Color.Red
                            )
                        } else {
                            BadgeText(
                                text = "مكتمل القراءة",
                                backgroundColor = Color.Gray.copy(alpha = 0.2f),
                                textColor = Color.LightGray
                            )
                        }
                    }

                    // Bottom Section: Title and Progress Text
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نسبة القراءة: ${(item.readProgress * 100).toInt()}%",
                            color = Color.LightGray.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
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
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
