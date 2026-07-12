// NEO MANGA: Premium Splash Screen intercept with scale + alpha Render Thread animations
package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NeoMangaSlogans = listOf(
    "رحلة قراءة مبهرة تبدأ الآن...",
    "عالمك المفضل، بنكهة عربية.",
    "قصص تنبض بالحياة، بين يديك.",
    "المانجا كما لم تراها من قبل."
)

@Composable
fun NeoMangaSplash(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.0f) }
    val alpha = remember { Animatable(1f) }
    val slogan = remember { NeoMangaSlogans.random() }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        delay(800)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_mihon),
                contentDescription = "Neo Manga Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = slogan,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
