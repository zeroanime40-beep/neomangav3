package eu.kanade.domain.extension.interactor

import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Locale

class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {
        return combine(
            preferences.enabledLanguages.changes(),
            extensionManager.installedExtensionsFlow,
            extensionManager.untrustedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguages, _installed, _untrusted, _available ->
            val inappropriateKeywords = arrayOf("+18", "18+", "محتوى غير لائق", "المحتوى غير لائق")
            val isAllowed = { ext: Extension ->
                // Rule 1: STRICT ARABIC ONLY
                val isArabic = ext.lang == "ar"

                // Rule 2: STRICT NSFW BLOCK
                val isNotNsfw = !ext.isNsfw

                // Rule 3: KEYWORD TEXT CLEANUP
                val nameLower = ext.name.lowercase(Locale.ROOT)
                val noInappropriateKeywords = inappropriateKeywords.none { keyword ->
                    nameLower.contains(keyword.lowercase(Locale.ROOT))
                }

                val sourcesList = when (ext) {
                    is Extension.Installed -> ext.sources.map { it.name }
                    is Extension.Available -> ext.sources.map { it.name }
                    else -> emptyList()
                }
                val noSourceInappropriateKeywords = sourcesList.none { sourceName ->
                    val sNameLower = sourceName.lowercase(Locale.ROOT)
                    inappropriateKeywords.any { keyword ->
                        sNameLower.contains(keyword.lowercase(Locale.ROOT))
                    }
                }

                isArabic && isNotNsfw && noInappropriateKeywords && noSourceInappropriateKeywords
            }

            val (updates, installed) = _installed
                .filter { isAllowed(it) }
                .sortedWith(
                    compareBy<Extension.Installed> { !it.isObsolete }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )
                .partition { it.hasUpdate }

            val untrusted = _untrusted
                .filter { isAllowed(it) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            val available = _available
                .filter { extension ->
                    _installed.none { it.pkgName == extension.pkgName } &&
                        _untrusted.none { it.pkgName == extension.pkgName } &&
                        isAllowed(extension)
                }
                .flatMap { ext ->
                    ext.sources.filter { it.lang == "ar" }
                        .map {
                            ext.copy(
                                name = it.name,
                                lang = it.lang,
                                pkgName = "${ext.pkgName}-${it.id}",
                                sources = listOf(it),
                            )
                        }
                }
                .filter { isAllowed(it) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            Extensions(updates, installed, available, untrusted)
        }
    }
}

