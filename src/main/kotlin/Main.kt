package net.shieru.kargo

import com.akuleshov7.ktoml.Toml
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.input.interactiveSelectList
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File

data class KargoConfig(val versionCatalog: String)

val t = Terminal()

suspend fun getCatalog(path: String): VersionCatalog = withContext(Dispatchers.IO) {
    VersionCatalog.parse(File(path).readText())
}

fun VersionCatalog.resolver(): Resolver = Resolver(this)

class Kargo : SuspendingCliktCommand() {
    override fun help(context: Context) = "Kargo: Gradle Version Catalog Manager"

    private val config: String by option(
        "-c",
        "--config",
        help = "Path to the Kargo configuration file, defaults to gradle/kargo.versions.toml or KARGO_CONFIG environment variable."
    ).default(System.getenv("KARGO_CONFIG") ?: "gradle/kargo.versions.toml")

    override suspend fun run() {
        currentContext.obj = KargoConfig(config)
    }
}

class Init : SuspendingCliktCommand() {
    private val config by requireObject<KargoConfig>()
    private val force by option(
        "-f",
        "--force",
        help = "Overwrite existing config file if it exists."
    ).flag("--no-force", default = false)
    private val check by option("--check", help = "Check if current directory is a Gradle project.").flag(
        "--no-check",
        default = true,
    )

    override fun help(context: Context) = "Initialize a new Kargo configuration for this project."

    override suspend fun run() {
        // Check current directory is a Gradle project
        val gradleGroovy = File("settings.gradle")
        val gradleKotlin = File("settings.gradle.kts")

        if (!gradleGroovy.exists() && !gradleKotlin.exists() && check) {
            t.println("Seems like current directory is not a Gradle project, aborting.", stderr = true)
            return
        }

        // Check config file exists
        val configFile = File(config.versionCatalog)
        if (configFile.exists() && !force) {
            if (YesNoPrompt(
                    "Config file already exists at ${TextColors.brightRed(configFile.path)}, overwrite?",
                    t,
                    default = false
                ).ask() == false
            ) {
                t.println("Config file already exists, aborting.", stderr = true)
                return
            }
            t.warning("Overwriting existing config file.")
        }
        configFile.writeText(
            """
            # Kargo generated file: don't edit manually!
            [versions]
            
            [libraries]
            
            [plugins]
            """.trimIndent()
        )

        println("Kargo version catalog created at ${configFile.path}.")

        val codeSnippet = run {
            val createSnippet = if (gradleGroovy.exists()) "kargo {" else "create(\"kargo\") {"
            """
                dependencyResolutionManagement {
                    versionCatalogs {
                        $createSnippet
                            from(files("${configFile.path}"))
                        }
                    }
                }
            """.trimIndent()
        }
        println()
        if (gradleGroovy.exists()) {
            println("Please add the following to your settings.gradle:")
        } else {
            println("Please add the following to your settings.gradle.kts:")
        }
        t.println(TextColors.brightYellow(codeSnippet))
    }
}

class Add : SuspendingCliktCommand() {
    override fun help(context: Context) = """
        Add a new library to the version catalog.
    """.trimIndent()

    private val config by requireObject<KargoConfig>()
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
        if (v == "@latest") {
            val latestVersion = fetchLatestVersion(g, a)
            val reference = if (ref != null) {
                resolveExplicitRef(ref!!)
            } else {
                suggestAndSelectRef(g, a, catalog)
            }
            addCatalogEntry(latestVersion.g, latestVersion.a, reference, latestVersion.v)
        } else {
            val reference = if (ref != null) {
                resolveExplicitRef(ref!!)
            } else {
                suggestAndSelectRef(g, a, catalog)
            }
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
                addEntry("${it.g}:${it.a}", TextColors.brightGreen("Latest: ${it.latestVersion}, Released at ${it.toInstant()}"))
            }
        } ?: error("No package selected")
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
        return t.interactiveSelectList(versions, title = "Select a version")
            ?: error("No version selected")
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
                    addEntry(it, TextColors.gray(catalog.versions[it] ?: "Unknown Version"))
                } else {
                    addEntry(it)
                }
            }
            addEntry("- Choose your own")
        } ?: error("No version reference selected")

        return if (selected == "- Choose your own") {
            t.prompt("What version reference name do you want to use for this package?")
                ?: error("No version reference name provided")
        } else {
            selected
        }
    }

    private fun gradleImplementationSnippet(alias: String) {
        fun String.toAccessor() =
            replace(":", ".").replace("-", ".").replace("_", ".")

        val groovySnippet = """
            dependencies {
                implementation "kargo.${alias.toAccessor()}"
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
        val catalog = getCatalog(config.versionCatalog).run {
            if (!this.versions.containsKey(ref)) {
                if (version == null) {
                    error("Reference not found, version not provided.")
                }
                return@run this.copy(versions = this.versions + (ref to version))
            }
            this
        }
        val module = Module(g, a, Version(ref))
        val versions = catalog.versions.toMutableMap()
        versions[ref] = version ?: catalog.resolver().resolveVersion(ref) ?: error("Version $ref not found in catalog")
        val libraries = catalog.libraries.toMutableMap()
        libraries[a] = module
        val plugins = catalog.plugins.toMutableMap()

        val toml = buildString {
            val rawToml = Toml.encodeToString(VersionCatalog(versions, libraries, plugins))
            appendLine("# Kargo generated file: don't edit manually!")
            appendLine()
            append(rawToml)
        }
        File(config.versionCatalog).writeText(toml)

        println(TextColors.brightGreen("Added $g:$a (ref: $ref, version: ${catalog.resolver().resolveVersion(ref)}) to the version catalog."))
        println(TextColors.brightGreen("To use this dependency, add the following snippet to your build.gradle(.kts):"))
        gradleImplementationSnippet(a)
    }

}

suspend fun main(args: Array<String>) = Kargo().subcommands(Init(), Add()).main(args)
