/*
 * Copyright (c) 2026 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.cli.run

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.opendc.sdk.model.experiment.ExperimentSpec
import org.opendc.sdk.model.serialization.SdkJson
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)

/** A request the server answered with a problem: its status, title and the issues it named. */
internal class ApiFailure(
    val status: Int,
    val title: String,
    val issues: List<String>,
) : RuntimeException(title)

/** How a request proves who sends it: not at all, against a deployment that signs nobody in, or with a token. */
internal sealed interface Credentials {
    data object Anonymous : Credentials

    data class Bearer(val token: String) : Credentials
}

/** Where a remote experiment is, as the server reports it. */
internal enum class RemoteState(val wire: String, val isTerminal: Boolean) {
    DRAFT("draft", false),
    QUEUED("queued", false),
    RUNNING("running", false),
    SUCCEEDED("succeeded", true),
    PARTIAL("partial", true),
    FAILED("failed", true),
    CANCELLED("cancelled", true),
    ;

    companion object {
        fun of(wire: String): RemoteState = entries.first { it.wire == wire }
    }
}

/** How far a remote experiment has got, and why each scenario that failed did. */
internal data class RemoteStatus(
    val state: RemoteState,
    val completedTasks: Long,
    val totalTasks: Long,
    val failures: List<String>,
)

/** The parts of the OpenDC API a remote run uses: projects, experiments, their status and their results. */
internal class OpendcApi(
    apiUrl: String,
    private val credentials: Credentials,
) {
    private val base = URI(apiUrl.trimEnd('/') + "/")
    private val client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()

    /** The id of the caller's project called [name], created if they have none. */
    fun projectNamed(name: String): String {
        val existing = get("api/v1/projects").jsonArray.map { it.jsonObject }.firstOrNull { it.string("name") == name }
        return existing?.string("id") ?: post("api/v1/projects", buildJsonObject { put("name", name) }).jsonObject.string("id")
    }

    /** Creates a draft of [spec] in [projectId] and returns its id. */
    fun createExperiment(
        projectId: String,
        name: String,
        spec: ExperimentSpec,
    ): String {
        val body =
            buildJsonObject {
                put("projectId", projectId)
                put("name", name)
                put("spec", SdkJson.json.encodeToJsonElement(ExperimentSpec.serializer(), spec))
            }
        return post("api/v1/experiments", body).jsonObject.string("id")
    }

    fun submit(experimentId: String) {
        post("api/v1/experiments/$experimentId/submit")
    }

    fun cancel(experimentId: String) {
        post("api/v1/experiments/$experimentId/cancel")
    }

    fun delete(experimentId: String) {
        send(request(base.resolve("api/v1/experiments/$experimentId")).DELETE().build())
    }

    fun status(experimentId: String): RemoteStatus {
        val status = get("api/v1/experiments/$experimentId/status").jsonObject
        val failures =
            status.getValue("scenarios").jsonArray.map { it.jsonObject }.mapNotNull { scenario ->
                scenario["exitInfo"]?.jsonObject?.let { exit ->
                    val message = exit["message"]?.jsonPrimitive?.content.orEmpty()
                    "scenario ${scenario.getValue("scenarioIndex").jsonPrimitive.int}: ${exit.string("reason")}" +
                        if (message.isEmpty()) "" else " ($message)"
                }
            }
        return RemoteStatus(
            state = RemoteState.of(status.string("state")),
            completedTasks = status.getValue("completedTasks").jsonPrimitive.long,
            totalTasks = status.getValue("totalTasks").jsonPrimitive.long,
            failures = failures,
        )
    }

    /** Streams the experiment's results archive, through the short-lived link the server signs for it. */
    fun <T> archive(
        experimentId: String,
        consume: (InputStream) -> T,
    ): T {
        val link = post("api/v1/experiments/$experimentId/archive/link").jsonObject.string("url")
        val response = client.send(request(base.resolve(link), ZIP).GET().build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != HTTP_OK) {
            throw failureOf(response.statusCode(), response.body().use { it.readBytes().decodeToString() })
        }
        return response.body().use(consume)
    }

    private fun get(path: String): JsonElement = send(request(base.resolve(path)).GET().build())

    // No content type without a body: the server matches a bodyless request that declares one
    // against what JSON endpoints expect and refuses it.
    private fun post(path: String): JsonElement = send(request(base.resolve(path)).POST(HttpRequest.BodyPublishers.noBody()).build())

    private fun post(
        path: String,
        body: JsonObject,
    ): JsonElement =
        send(
            request(base.resolve(path))
                .header("Content-Type", JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build(),
        )

    private fun send(request: HttpRequest): JsonElement {
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in HTTP_SUCCESS) {
            throw failureOf(response.statusCode(), response.body())
        }
        return if (response.body().isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(response.body())
    }

    private fun request(
        uri: URI,
        accept: String = JSON,
    ): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder(uri).header("Accept", accept)
        return when (credentials) {
            Credentials.Anonymous -> builder
            is Credentials.Bearer -> builder.header("Authorization", "Bearer ${credentials.token}")
        }
    }

    /** The server's problem document when it sent one, and the bare status otherwise. */
    private fun failureOf(
        status: Int,
        body: String,
    ): ApiFailure {
        val problem = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()
        val title = problem?.get("title")?.jsonPrimitive?.content ?: "The server answered HTTP $status"
        val issues =
            problem?.get("issues")?.jsonArray.orEmpty().map { issue ->
                val fields = issue.jsonObject
                val path = fields["path"]?.jsonPrimitive?.content.orEmpty()
                val message = fields["message"]?.jsonPrimitive?.content.orEmpty()
                if (path.isEmpty()) message else "$path: $message"
            }
        return ApiFailure(status, title, issues)
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private companion object {
        const val HTTP_OK = 200
        val HTTP_SUCCESS = 200..299
        const val JSON = "application/json"
        const val ZIP = "application/zip"
    }
}
