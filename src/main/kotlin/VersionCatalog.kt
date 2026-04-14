package net.shieru.kargo

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class VersionCatalog(
    val versions: Map<String, String>,
    val libraries: Map<String, Module>,
    val plugins: Map<String, Plugin>
) {
    fun save(file: File) {
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { writer ->
            writer.write("# Kargo generated file: don't edit manually!")
            writer.newLine()
            writer.newLine()
            writer.write(Toml.encodeToString(this@VersionCatalog))
        }
    }

    companion object {
        fun parse(toml: String): VersionCatalog = Toml.decodeFromString(toml)
    }
}

@Serializable
@TomlInlineTable
data class Module(val group: String, val name: String, val version: Version)


@Serializable
data class Version(val ref: String)

@Serializable
data class Plugin(val id: String, val version: Version)

fun String.toAccessor() = replace(":", ".").replace("-", ".").replace("_", ".")

class Resolver(val catalog: VersionCatalog) {
    fun resolveVersion(version: String): String? = catalog.versions[version]
    fun suggestVersionRefs(groupId: String, artifactId: String): Set<String> {
        val versionRefs = mutableSetOf<String>()

        run {
            val parts = groupId.split('.')
            versionRefs.add(parts.last())
        }

        run {
            val parts = artifactId.split('-')
            versionRefs.addAll(
                parts.runningFold("") { acc, part ->
                    if (acc.isEmpty()) part else "$acc-$part"
                }.drop(1)
            )
        }

        return versionRefs
    }

    fun isVersionUsed(ref: String, excludeLibraryAlias: String? = null): Boolean {
        catalog.libraries.forEach { (alias, module) ->
            if (alias != excludeLibraryAlias && module.version.ref == ref) {
                return true
            }
        }
        catalog.plugins.values.forEach { plugin ->
            if (plugin.version.ref == ref) {
                return true
            }
        }
        return false
    }
}
