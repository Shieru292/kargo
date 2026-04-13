package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.shieru.kargo.*
import java.io.File

class Remove : SuspendingCliktCommand() {
    override fun help(context: Context) = "Remove a dependency from the version catalog."

    private val config by requireObject<KargoContext>()
    private val alias by argument("alias", help = "The alias of the library to remove.")

    override suspend fun run() {
        val catalog = getCatalog(config.versionCatalog)
        val module = catalog.libraries[alias] ?: run {
            t.println("${TextColors.brightRed("Error:")} Library with alias '$alias' not found in the version catalog.", stderr = true)
            throw Abort()
        }

        val versionRef = module.version.ref
        val newLibraries = catalog.libraries.filter { it.key != alias }
        var newVersions = catalog.versions

        val resolver = catalog.resolver()
        if (!resolver.isVersionUsed(versionRef, excludeLibraryAlias = alias)) {
            val currentVersion = resolver.resolveVersion(versionRef)
            if (YesNoPrompt(
                    "Version reference '$versionRef' ($currentVersion) is no longer used. Do you want to remove it too?",
                    t,
                    default = true
                ).ask() == true
            ) {
                newVersions = catalog.versions.filter { it.key != versionRef }
                t.println(TextColors.brightGreen("Removed version reference '$versionRef'."))
            }
        }

        val newCatalog = VersionCatalog(newVersions, newLibraries, catalog.plugins)
        newCatalog.save(File(config.versionCatalog))

        t.println(TextColors.brightGreen("Removed library '$alias' from the version catalog."))
    }
}
