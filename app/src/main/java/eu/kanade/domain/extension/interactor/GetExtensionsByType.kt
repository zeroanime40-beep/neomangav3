package eu.kanade.domain.extension.interactor

import eu.kanade.domain.extension.model.Extensions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetExtensionsByType(
    private val preferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {

    fun subscribe(): Flow<Extensions> {
        val showNsfwSources = preferences.showNsfwSource.get()

        return combine(
            preferences.enabledLanguages.changes(),
            extensionManager.installedExtensionsFlow,
            extensionManager.untrustedExtensionsFlow,
            extensionManager.availableExtensionsFlow,
        ) { enabledLanguages, _installed, _untrusted, _available ->
            val inappropriateKeywords = arrayOf("+18", "محتوى غير لائق", "المحتوى غير لائق")
            val isInappropriate = { ext: Extension ->
                val name = ext.name.trim()
                inappropriateKeywords.any { keyword ->
                    name.contains(keyword.trim(), ignoreCase = true)
                }
            }

            val (updates, installed) = _installed
                .filter { (showNsfwSources || !it.isNsfw) && !isInappropriate(it) }
                .sortedWith(
                    compareBy<Extension.Installed> { !it.isObsolete }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )
                .partition { it.hasUpdate }

            val untrusted = _untrusted
                .filter { !isInappropriate(it) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            val available = _available
                .filter { extension ->
                    _installed.none { it.pkgName == extension.pkgName } &&
                        _untrusted.none { it.pkgName == extension.pkgName } &&
                        (showNsfwSources || !extension.isNsfw) &&
                        !isInappropriate(extension)
                }
                .flatMap { ext ->
                    ext.sources.filter { it.lang in enabledLanguages }
                        .map {
                            ext.copy(
                                name = it.name,
                                lang = it.lang,
                                pkgName = "${ext.pkgName}-${it.id}",
                                sources = listOf(it),
                            )
                        }
                }
                .filter { !isInappropriate(it) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

            Extensions(updates, installed, available, untrusted)
        }
    }
}
