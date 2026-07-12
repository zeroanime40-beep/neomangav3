package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.theme.NeoMangaTheme

// NEO MANGA: Premium Compose replacement for the legacy reader_error.xml XML layout.
@Composable
fun ReaderErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenInWebView: () -> Unit,
    showWebView: Boolean,
) {
    NeoMangaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text(text = "Retry")
            }

            if (showWebView) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenInWebView) {
                    Text(text = "Open in WebView")
                }
            }
        }
    }
}
