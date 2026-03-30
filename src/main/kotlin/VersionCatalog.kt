package net.shieru.kargo

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class VersionCatalog(
    val versions: Map<String, String>,
    val libraries: Map<String, Module>,
    val plugins: Map<String, Plugin>
) {
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

class Resolver(val catalog: VersionCatalog) {
    fun resolveVersion(version: String): String? = catalog.versions[version]
    fun toModuleString(module: Module): String = "${module.group}:${module.name}:${resolveVersion(module.version.ref)}"
    fun checkUsedVersion(groupId: String, artifactId: String): String? {
        val versionRefs = suggestVersionRefs(groupId, artifactId)
        versionRefs.forEach {
            if (catalog.versions.containsKey(it)) {
                return it
            }
        }
        return null
    }
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
}
