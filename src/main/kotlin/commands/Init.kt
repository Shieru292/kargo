package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.shieru.kargo.*
import java.io.File

class Init : SuspendingCliktCommand() {
    private val config by requireObject<KargoContext>()
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
        try {
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
                t.println(TextColors.yellow("Overwriting existing config file."))
            }

            val newCatalog = VersionCatalog(emptyMap(), emptyMap(), emptyMap())
            newCatalog.save(configFile)

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
        } catch (e: Exception) {
            t.println("${TextColors.brightRed("Error:")} ${e.message}", stderr = true)
            throw Abort()
        }
    }
}
