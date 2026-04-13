package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.runBlocking
import net.shieru.kargo.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

class InitTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var recorder: TerminalRecorder

    @BeforeEach
    fun setup() {
        recorder = TerminalRecorder()
        t = Terminal(terminalInterface = recorder)
    }

    @Test
    fun `init creates kargo versions toml`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        
        // 実行
        Kargo().subcommands(Init()).parse(listOf("-c", catalogFile.absolutePath, "init", "--no-check"))
        
        assertTrue(catalogFile.exists(), "Catalog file should be created")
        val content = catalogFile.readText()
        assertTrue(content.contains("versions"))
        assertTrue(content.contains("libraries"))
        assertTrue(content.contains("plugins"))
    }
}
