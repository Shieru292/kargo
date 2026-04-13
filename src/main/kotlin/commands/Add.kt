package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.prompt
import net.shieru.kargo.*
import java.io.File

class Add : SuspendingCliktCommand() {
    override fun help(context: Context) = """
        Add a new library to the version catalog.
    """.trimIndent()

    private val config by requireObject<KargoContext>()
    private val dependency by argument(
        "package", help = "The package name or search query."
    )
    private val ref by option(
        "-r",
        "--ref",
        help = "The version reference name. Leave this field empty to suggest some good names for you."
    )

    private val mavenCentralClient = MavenCentralClient()

    override suspend fun run() {
        val parts = dependency.split(":")
        val catalog = getCatalog(config.versionCatalog)

        when (parts.size) {
            3 -> handleThreeParts(parts, catalog)
            2 -> handleTwoParts(parts, catalog)
            1 -> handleOnePart(parts, catalog)
        }
    }

    private suspend fun handleThreeParts(parts: List<String>, catalog: VersionCatalog) {
        val (g, a, v) = parts
        val reference = if (ref != null) {
            resolveExplicitRef(ref!!)
        } else {
            suggestAndSelectRef(g, a, catalog)
        }
        if (v == "@latest") {
            val latestVersion = fetchLatestVersion(g, a)
            addCatalogEntry(latestVersion.g, latestVersion.a, reference, latestVersion.v)
        } else {
            addCatalogEntry(g, a, reference,v)
        }
    }

    private suspend fun handleTwoParts(parts: List<String>, catalog: VersionCatalog) {
        val (g, a) = parts
        val versions = fetchVersions(g, a)
        val preferredVersion by lazy { selectVersion(versions) }

        val reference = if (ref != null) {
            resolveExplicitRef(ref!!)
        } else {
            suggestAndSelectRef(g, a, catalog)
        }

        if (catalog.resolver().resolveVersion(reference) == null) {
            addCatalogEntry(g, a, reference, preferredVersion)
        } else {
            addCatalogEntry(g, a, reference)
        }
    }

    private suspend fun handleOnePart(parts: List<String>, catalog: VersionCatalog) {
        val (query) = parts
        val response = mavenCentralClient.searchMavenByText(query)
        val docs = response.response.docs
        val selectedDoc = t.interactiveSelectList {
            title("Search results for $query")
            docs.forEach {
                addEntry("${it.g}:${it.a}", TextColors.brightGreen("  - Latest: ${it.latestVersion}, Released at ${it.toInstant()}\n"))
            }
        } ?: run {
            echo("No package selected", err = true)
            throw Abort()
        }
        val (g, a) = selectedDoc.split(":")
        handleTwoParts(listOf(g, a), catalog)
    }

    private suspend fun fetchLatestVersion(g: String, a: String): MavenGavDocument {
        val response = mavenCentralClient.searchVersionsGav(g, a)
        return response.response.docs.firstOrNull() ?: error("No version found for $g:$a")
    }

    private suspend fun fetchVersions(g: String, a: String): List<String> {
        val response = mavenCentralClient.searchVersionsGav(g, a)
        return response.response.docs.map { it.v }
    }

    private fun selectVersion(versions: List<String>): String {
        if (versions.isEmpty()) {
            echo("No versions found for this package. This occurs when the package is not available in Maven Central.", err = true)
            return t.prompt("Please enter the version manually").asNonEmpty() ?: run {
                echo("No version entered", err = true)
                throw Abort()
            }
        }
        val selected = t.interactiveSelectList {
            entries(versions)
            addEntry("- Enter manually")
            title("Select a version")
        } ?: run {
            echo("No version selected", err = true)
            throw Abort()
        }
        if (selected == "- Enter manually") {
            return t.prompt("Please enter the version manually").asNonEmpty() ?: run {
                echo("No version entered", err = true)
                throw Abort()
            }
        }
        return selected
    }

    private fun resolveExplicitRef(ref: String): String {
        return ref
    }

    private fun suggestAndSelectRef(g: String, a: String, catalog: VersionCatalog): String {
        val suggestedRefs = catalog.resolver().suggestVersionRefs(g, a)
        val filteredRefs = suggestedRefs.filter { catalog.versions.containsKey(it) }

        return if (filteredRefs.isEmpty()) {
            t.println("We couldn't find any version reference for $g:$a. Please select one from the following:")
            selectRefFromOptions(suggestedRefs, null, catalog)
        } else {
            t.println("We detected that you are using a similar package. Which version do you want to use?")
            selectRefFromOptions(suggestedRefs, filteredRefs, catalog)
        }
    }

    private fun selectRefFromOptions(allSuggested: Set<String>, filtered: List<String>?, catalog: VersionCatalog): String {
        val options = (filtered?.toSet() ?: emptySet()) + allSuggested
        val selected = t.interactiveSelectList {
            title("Select a version reference")
            options.forEach {
                if (catalog.versions.containsKey(it)) {
                    addEntry(it, TextColors.gray("  - " + catalog.versions[it]))
                } else {
                    addEntry(it)
                }
            }
            addEntry("- Choose your own")
        } ?: run {
            echo("No version reference selected", err = true)
            throw Abort()
        }

        return if (selected == "- Choose your own") {
            t.prompt("What version reference name do you want to use for this package?")
                ?: run {
                    echo("No version reference name provided", err = true)
                    throw Abort()
                }
        } else {
            selected
        }
    }

    private fun gradleImplementationSnippet(alias: String) {
        fun String.toAccessor() =
            replace(":", ".").replace("-", ".").replace("_", ".")

        val groovySnippet = """
            dependencies {
                implementation kargo.${alias.toAccessor()}
            }
        """.trimIndent()
        val kotlinSnippet = """
            dependencies {
                implementation(kargo.${alias.toAccessor()})
            }
        """.trimIndent()
        t.println(TextColors.gray("build.gradle (Groovy):"))
        t.println(TextColors.brightYellow(groovySnippet))
        t.println()
        t.println(TextColors.gray("build.gradle.kts (Kotlin):"))
        t.println(TextColors.brightYellow(kotlinSnippet))
    }

    suspend fun addCatalogEntry(g: String, a: String, ref: String, version: String? = null) {
        val initialCatalog = getCatalog(config.versionCatalog)
        val catalogWithVersion = if (!initialCatalog.versions.containsKey(ref)) {
            if (version == null) {
                error("Reference not found, version not provided.")
            }
            initialCatalog.copy(versions = initialCatalog.versions + (ref to version))
        } else {
            initialCatalog
        }

        val module = Module(g, a, Version(ref))
        val versions = catalogWithVersion.versions.toMutableMap()
        versions[ref] = version ?: catalogWithVersion.resolver().resolveVersion(ref) ?: error("Version $ref not found in catalog")
        val libraries = catalogWithVersion.libraries.toMutableMap()
        libraries[a] = module
        val plugins = catalogWithVersion.plugins.toMutableMap()

        val newCatalog = VersionCatalog(versions, libraries, plugins)
        newCatalog.save(File(config.versionCatalog))

        println(TextColors.brightGreen("Added $g:$a (ref: $ref, version: ${newCatalog.resolver().resolveVersion(ref)}) to the version catalog."))
        println(TextColors.brightGreen("To use this dependency, add the following snippet to your build.gradle(.kts):"))
        gradleImplementationSnippet(a)
    }
}
