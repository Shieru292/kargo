package net.shieru.kargo.commands

import com.github.ajalt.clikt.command.parse
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlinx.coroutines.runBlocking
import net.shieru.kargo.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class AddTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var recorder: TerminalRecorder

    @BeforeEach
    fun setup() {
        recorder = TerminalRecorder()
        t = Terminal(terminalInterface = recorder)
    }

    @Test
    fun `add library with explicit version and reference`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        val initialContent = """
            [versions]
            junit = "5.10.0"
            
            [libraries]
            
            [plugins]
        """.trimIndent()
        catalogFile.writeText(initialContent)

        // 実行: g:a:v 指定、リファレンスは既存の junit を使用
        Kargo().subcommands(Add()).parse(listOf("-c", catalogFile.absolutePath, "add", "org.junit.jupiter:junit-jupiter:5.10.1", "--ref", "junit"))

        val content = catalogFile.readText()
        assertTrue(content.contains("junit-jupiter"), "Library should be added")
        // addCatalogEntry の実装上、既存の ref がある場合はバージョンが上書きされるか確認が必要だが、
        // 現状の実装（Add.kt:207）では version が渡されれば上書きされる。
        assertTrue(content.contains("junit = \"5.10.1\""), "Version reference should be updated")
    }

    @Test
    fun `add library with latest version`() = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        catalogFile.writeText("[versions]\n[libraries]\n[plugins]")

        // 実行: g:a:@latest 指定
        Kargo().subcommands(Add()).parse(listOf("-c", catalogFile.absolutePath, "add", "org.junit.jupiter:junit-jupiter:@latest", "--ref", "junit"))

        val content = catalogFile.readText()
        assertTrue(content.contains("junit-jupiter"), "Library should be added")
        assertTrue(content.contains("junit = \""), "Version reference should be added with some version")
    }

    @Test
    fun `add library with invalid alias should fail`(): Unit = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        catalogFile.writeText("[versions]\n[libraries]\n[plugins]")

        assertThrows(Abort::class.java) {
            runBlocking {
                Kargo().subcommands(Add()).parse(listOf("-c", catalogFile.absolutePath, "add", "org.example:invalid..alias:1.0", "--ref", "ref"))
            }
        }
    }

    @Test
    fun `add library with invalid ref should fail`(): Unit = runBlocking {
        val catalogFile = File(tempDir, "kargo.versions.toml")
        catalogFile.writeText("[versions]\n[libraries]\n[plugins]")

        assertThrows(Abort::class.java) {
            runBlocking {
                Kargo().subcommands(Add()).parse(listOf("-c", catalogFile.absolutePath, "add", "org.example:lib:1.0", "--ref", "invalid..ref"))
            }
        }
    }

    @Test
    fun `init should create parent directory if not exists`() = runBlocking {
        val subDir = File(tempDir, "new_gradle")
        val catalogFile = File(subDir, "kargo.versions.toml")

        Kargo().subcommands(Init()).parse(listOf("-c", catalogFile.absolutePath, "init", "--no-check"))

        assertTrue(catalogFile.exists(), "Catalog file should be created in the new directory")
    }
}
