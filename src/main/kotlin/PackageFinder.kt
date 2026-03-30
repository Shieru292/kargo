package net.shieru.kargo

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
data class MavenSearchResponse(val response: MavenResponse)

@Serializable
data class MavenResponse(val numFound: Int, val docs: List<MavenDocument>)

@Serializable
data class MavenDocument(
    val id: String,
    val g: String,
    val a: String,
    val latestVersion: String,
    val repositoryId: String,
    val timestamp: Long,
    val versionCount: Int
) {
    fun toInstant(): Instant = Instant.fromEpochMilliseconds(timestamp)
}

@Serializable
data class MavenGavAPIResponse(val response: MavenGavResponse)

@Serializable
data class MavenGavResponse(val numFound: Int, val docs: List<MavenGavDocument>)

@Serializable
data class MavenGavDocument(
    val id: String, val g: String, val a: String, val v: String, val timestamp: Long
) {
    fun toInstant(): Instant = Instant.fromEpochMilliseconds(timestamp)
}


class MavenCentralClient {
    val client = HttpClient(Java)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val baseUrl = "https://central.sonatype.com"

    suspend fun searchMavenByText(query: String): MavenSearchResponse {
        val responseText = client.get("$baseUrl/solrsearch/select") {
            parameter("q", query)
            parameter("rows", 10)
            parameter("wt", "json")
        }.bodyAsText()
        return json.decodeFromString(MavenSearchResponse.serializer(), responseText)
    }

    suspend fun searchVersionsGav(g: String, a: String): MavenGavAPIResponse {
        val responseText = client.get("$baseUrl/solrsearch/select") {
            parameter("q", "g:$g AND a:$a")
            parameter("core", "gav")
            parameter("rows", 15)
            parameter("wt", "json")
            parameter("sort", "v desc")
        }.bodyAsText()
        return json.decodeFromString(MavenGavAPIResponse.serializer(), responseText)
    }

    fun close() {
        client.close()
    }
}
