package net.shieru.kargo

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.shieru.kargo.commands.Add
import net.shieru.kargo.commands.Init
import net.shieru.kargo.commands.Remove
import java.io.File

data class KargoContext(val versionCatalog: String)

var t = Terminal()

suspend fun getCatalog(path: String): VersionCatalog = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists()) {
        t.println("${TextColors.brightRed("Error:")} Version catalog file not found at $path. Please run 'init' first.", stderr = true)
        throw Abort()
    }
    try {
        VersionCatalog.parse(file.readText())
    } catch (e: Exception) {
        t.println("${TextColors.brightRed("Error:")} Failed to parse version catalog at $path: ${e.message}", stderr = true)
        throw Abort()
    }
}

fun VersionCatalog.resolver(): Resolver = Resolver(this)

fun String?.asNonEmpty(): String? {
    return this?.takeIf { it.isNotEmpty() }
}

class Kargo : SuspendingCliktCommand() {
    override fun help(context: Context) = "Kargo: Gradle Version Catalog Manager"

    private val config: String by option(
        "-c",
        "--config",
        help = "Path to the Kargo configuration file, defaults to gradle/kargo.versions.toml or KARGO_CONFIG environment variable."
    ).default(System.getenv("KARGO_CONFIG") ?: "gradle/kargo.versions.toml")

    override suspend fun run() {
        currentContext.obj = KargoContext(config)
    }
}

suspend fun main(args: Array<String>) = Kargo().subcommands(Init(), Add(), Remove()).main(args)
