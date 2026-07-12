package eu.kanade.tachiyomi.ui.dashboard

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.FadeTransition
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.presentation.util.Tab
import tachiyomi.i18n.MR

import eu.kanade.tachiyomi.R

object DashboardTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = "الرئيسية", // Arabic "Home" or "Dashboard"
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.popUntilRoot()
    }

    @Composable
    override fun Content() {
        Navigator(DashboardScreen()) {
            FadeTransition(it)
        }
    }
}
