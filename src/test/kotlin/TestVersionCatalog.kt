import net.shieru.kargo.Resolver
import net.shieru.kargo.VersionCatalog
import kotlin.test.Test
import kotlin.test.assertEquals

class TestVersionCatalog {
    val catalogToml = """
        [versions]
        mylib = "1.3.5"
        lib1 = "1.0.0"
        lib2 = "2.0.0"
        
        [libraries]
        lib1 = { group = "org.example", name = "lib1", version.ref = "lib1" }
        lib2 = { group = "org.example", name = "lib2", version.ref = "lib2" }
        mylib = { group = "org.example", name = "mylib", version.ref = "mylib" }
        mylib2 = { group = "org.example", name = "mylib2", version.ref = "mylib" }
        
        [plugins]
        my-plugin = { id = "org.example.myplugin", version.ref = "mylib" }
    """.trimIndent()

    @Test
    fun test() {
        val catalog = VersionCatalog.parse(catalogToml)
        assert(catalog.versions.size == 3)
        assert(catalog.libraries.size == 4)
        assert(catalog.plugins.size == 1)
    }

    @Test
    fun testModuleNameSuggestions() {
        val resolver = Resolver(VersionCatalog.parse(catalogToml))
        val names = resolver.suggestVersionRefs("org.example.myproject", "myproject-core-library")
        val expectedNames = setOf(
            "myproject", "myproject-core", "myproject-core-library"
        )

        assertEquals(expectedNames, names)
    }
}
