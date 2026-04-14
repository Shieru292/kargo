package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextColors
import net.shieru.kargo.*

class Generate : SuspendingCliktCommand() {
    override fun help(context: Context) = "Generate code snippets for build.gradle(.kts) from the version catalog."

    private val config by requireObject<KargoContext>()
    private val lang by option("-l", "--lang", help = "The language of the build script.")
        .choice("kotlin", "groovy")
        .default("kotlin")

    override suspend fun run() {
        try {
            val catalog = getCatalog(config.versionCatalog)
            
            if (catalog.libraries.isEmpty() && catalog.plugins.isEmpty()) {
                t.println(TextColors.brightYellow("Version catalog is empty. Nothing to generate."))
                return
            }

            val snippet = when (lang) {
                "kotlin" -> generateKotlinSnippet(catalog)
                "groovy" -> generateGroovySnippet(catalog)
                else -> throw Abort()
            }

            t.println(snippet)
        } catch (e: Abort) {
            throw e
        } catch (e: Exception) {
            t.println("${TextColors.brightRed("Error:")} ${e.message}", stderr = true)
            throw Abort()
        }
    }

    private fun generateKotlinSnippet(catalog: VersionCatalog): String {
        val sb = StringBuilder()
        
        if (catalog.plugins.isNotEmpty()) {
            sb.append("plugins {\n")
            catalog.plugins.forEach { (alias, _) ->
                // Version Catalog plugins are accessed via alias in Kotlin DSL
                // alias(kargo.plugins.xxx)
                sb.append("    alias(kargo.plugins.${alias.toAccessor()})\n")
            }
            sb.append("}\n\n")
        }

        if (catalog.libraries.isNotEmpty()) {
            sb.append("dependencies {\n")
            catalog.libraries.forEach { (alias, _) ->
                sb.append("    implementation(kargo.${alias.toAccessor()})\n")
            }
            sb.append("}\n")
        }

        return sb.toString()
    }

    private fun generateGroovySnippet(catalog: VersionCatalog): String {
        val sb = StringBuilder()
        
        if (catalog.plugins.isNotEmpty()) {
            sb.append("plugins {\n")
            catalog.plugins.forEach { (alias, _) ->
                // alias kargo.plugins.xxx
                sb.append("    alias kargo.plugins.${alias.toAccessor()}\n")
            }
            sb.append("}\n\n")
        }

        if (catalog.libraries.isNotEmpty()) {
            sb.append("dependencies {\n")
            catalog.libraries.forEach { (alias, _) ->
                sb.append("    implementation kargo.${alias.toAccessor()}\n")
            }
            sb.append("}\n")
        }

        return sb.toString()
    }
}
