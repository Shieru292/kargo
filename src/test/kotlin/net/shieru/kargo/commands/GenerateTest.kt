package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.runBlocking
import net.shieru.kargo.Kargo
import net.shieru.kargo.t
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class GenerateTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var recorder: TerminalRecorder

    @BeforeEach
    fun setup() {
        recorder = TerminalRecorder()
        t = Terminal(terminalInterface = recorder)
    }

    @Test
    fun `generate kotlin snippets`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val content = """
            [versions]
            kotlin = "1.9.0"
            junit = "5.10.0"
            
            [libraries]
            junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version = { ref = "junit" } }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = { ref = "kotlin" } }
        """.trimIndent()
        catalogFile.writeText(content)

        Kargo().subcommands(Generate()).parse(listOf("-c", catalogFile.absolutePath, "generate", "--lang", "kotlin"))

        val output = recorder.stdout()
        assertTrue(output.contains("plugins {"))
        assertTrue(output.contains("alias(kargo.plugins.kotlin.jvm)"))
        assertTrue(output.contains("dependencies {"))
        assertTrue(output.contains("implementation(kargo.junit.jupiter)"))
    }

    @Test
    fun `generate groovy snippets`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val content = """
            [versions]
            kotlin = "1.9.0"
            junit = "5.10.0"
            
            [libraries]
            junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version = { ref = "junit" } }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = { ref = "kotlin" } }
        """.trimIndent()
        catalogFile.writeText(content)

        Kargo().subcommands(Generate()).parse(listOf("-c", catalogFile.absolutePath, "generate", "--lang", "groovy"))

        val output = recorder.stdout()
        assertTrue(output.contains("plugins {"))
        assertTrue(output.contains("alias kargo.plugins.kotlin.jvm"))
        assertTrue(output.contains("dependencies {"))
        assertTrue(output.contains("implementation kargo.junit.jupiter"))
    }

    @Test
    fun `generate empty catalog`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val content = """
            [versions]
            [libraries]
            [plugins]
        """.trimIndent()
        catalogFile.writeText(content)

        Kargo().subcommands(Generate()).parse(listOf("-c", catalogFile.absolutePath, "generate"))

        val output = recorder.stdout()
        assertTrue(output.contains("Version catalog is empty"))
    }
}
