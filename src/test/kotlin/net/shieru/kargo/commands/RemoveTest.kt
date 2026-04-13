package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.runBlocking
import net.shieru.kargo.Kargo
import net.shieru.kargo.t
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class RemoveTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var recorder: TerminalRecorder

    @BeforeEach
    fun setup() {
        recorder = TerminalRecorder()
        t = Terminal(terminalInterface = recorder)
    }

    @Test
    fun `remove library from catalog`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val initialContent = """
            [versions]
            junit = "5.10.0"
            
            [libraries]
            junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
            
            [plugins]
        """.trimIndent()
        catalogFile.writeText(initialContent)

        // 実行
        Kargo().subcommands(Remove()).parse(listOf("-c", catalogFile.absolutePath, "remove", "junit-jupiter"))

        val content = catalogFile.readText()
        assertFalse(content.contains("junit-jupiter"), "Library should be removed")
    }

    @Test
    fun `remove library and unused version reference`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val initialContent = """
            [versions]
            junit = "5.10.0"
            
            [libraries]
            junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
            
            [plugins]
        """.trimIndent()
        catalogFile.writeText(initialContent)

        // バージョン削除のプロンプトに "yes" と答えるシミュレーション
        recorder.inputLines.add("y")

        // 実行
        Kargo().subcommands(Remove()).parse(listOf("-c", catalogFile.absolutePath, "remove", "junit-jupiter"))

        val content = catalogFile.readText()
        assertFalse(content.contains("junit-jupiter"), "Library should be removed")
        assertFalse(content.contains("junit = \"5.10.0\""), "Unused version reference should be removed")
    }
}
