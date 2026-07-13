// NEO MANGA: Smart Onboarding overlay utilizing OledScaffold and auto-repo injection + bulk download for Arabic extensions.
package eu.kanade.tachiyomi.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.domain.extension.interactor.AddExtensionStore
import tachiyomi.presentation.core.components.OledScaffold
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val ARABIC_REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

@Composable
fun NeoMangaOnboarding(
    onComplete: () -> Unit,
) {
    val extensionManager = remember { Injekt.get<ExtensionManager>() }
    val addExtensionStore = remember { Injekt.get<AddExtensionStore>() }
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("جاري تهيئة الإعدادات...") }
    var isRepoInjected by remember { mutableStateOf(false) }

    val availableExtensions by extensionManager.availableExtensionsFlow.collectAsState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // NEO MANGA: Programmatically inject the Arabic Extension Repository silently on first load
            addExtensionStore(ARABIC_REPO_URL)
            // Fetch available extensions immediately so the list is ready
            extensionManager.findAvailableExtensions()
            isRepoInjected = true
            statusText = "البيئة جاهزة"
        }
    }

    OledScaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحباً بك في نيو مانجا",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "تجربة القراءة الأفضل للمانجا العربية",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (!isDownloading) {
                Button(
                    onClick = {
                        isDownloading = true
                        statusText = "جاري التثبيت..."
                        scope.launch(Dispatchers.IO) {
                            // NEO MANGA: Dynamic Targeting Strategy (Filter by lang == "ar")
                            val arabicExtensions = availableExtensions.filter { it.lang == "ar" }
                            val total = arabicExtensions.size
                            if (total == 0) {
                                withContext(Dispatchers.Main) { onComplete() }
                                return@launch
                            }
                            
                            var completed = 0
                            arabicExtensions.forEach { extension ->
                                scope.launch(Dispatchers.IO) {
                                    // NEO MANGA: Native Session Installer pipeline triggered in parallel, allowing OS popups to sequence naturally
                                    extensionManager.installExtension(extension).collect { step ->
                                        if (step.isCompleted()) {
                                            withContext(Dispatchers.Main) {
                                                completed++
                                                progress = completed.toFloat() / total
                                                if (completed >= total) {
                                                    onComplete()
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                statusText = "حالة ${extension.name}: ${step.name}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    ),
                    enabled = isRepoInjected
                ) {
                    Text(
                        text = "اضغط 'تثبيت' على نوافذ النظام المتتالية لتجهيز كافة المصادر العربية فوراً بلمح البصر.",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("تخطي")
                }
            } else {
                Text(
                    text = statusText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00E5FF),
                    trackColor = Color.DarkGray
                )
            }
        }
    }
}
