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

package org.opendc.web.server.metrics

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.ApiTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/** Where the dashboards are kept, from this module's directory, which is where tests run. */
private val DASHBOARDS: Path = Path.of("../../deploy/grafana")

/** Management port in the test profile. */
private const val MANAGEMENT_PORT = 9001

/** Labels Prometheus attaches when it scrapes, which the scrape itself therefore never carries. */
private val TARGET_LABELS = setOf("instance", "job")

/** Words in a PromQL expression that are functions, aggregations or keywords rather than metrics. */
private val PROMQL_WORDS =
    setOf(
        "sum", "max", "min", "avg", "count", "topk", "bottomk", "by", "without", "rate", "increase",
        "irate", "histogram_quantile", "le", "on", "ignoring", "group_left", "group_right", "and", "or", "unless",
    )

/**
 * The dashboards shipped under deploy/grafana, checked against what this server actually exports:
 * a panel querying a metric that does not exist draws an empty chart and nobody notices until it is
 * needed. Labels a query selects or groups by have to exist on the metric too.
 */
@QuarkusTest
class DashboardsTest {
    @Test
    fun `every dashboard queries only metrics and labels the server exports`() {
        // Some API traffic first, so the request timer has series to show.
        ApiTest.requestJson().get("/api/v1/config").then().statusCode(200)
        val exported = exportedSeries(given().port(MANAGEMENT_PORT).get("/q/metrics").then().statusCode(200).extract().asString())

        val dashboards = Files.list(DASHBOARDS).use { files -> files.filter { it.extension == "json" }.toList() }
        assertTrue(dashboards.isNotEmpty(), "no dashboards under $DASHBOARDS")
        for (dashboard in dashboards) {
            val expressions = expressionsOf(Json.parseToJsonElement(dashboard.readText()))
            assertTrue(expressions.isNotEmpty(), "${dashboard.fileName} has no queries")
            for (expression in expressions) {
                for ((metric, labels) in referencesOf(expression)) {
                    val known = exported[metric]
                    assertTrue(known != null) {
                        "${dashboard.fileName} queries $metric, which the server does not export: $expression\n" +
                            "it does export ${exported.keys.filter { it.substringBefore('_') == metric.substringBefore('_') }.sorted()}"
                    }
                    assertEquals(
                        emptySet<String>(),
                        labels - known.orEmpty() - TARGET_LABELS,
                        "${dashboard.fileName}: labels missing on $metric",
                    )
                }
            }
        }
    }

    /** Every series name in a Prometheus scrape, with the labels it carries. */
    private fun exportedSeries(scrape: String): Map<String, Set<String>> =
        scrape
            .lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val name = line.substringBefore('{').substringBefore(' ')
                val labels =
                    Regex(
                        """([a-zA-Z_]+)="""",
                    ).findAll(line.substringAfter('{', "").substringBefore('}')).map { it.groupValues[1] }
                name to labels.toSet()
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, labels) -> labels.flatten().toSet() }

    private fun expressionsOf(element: JsonElement): List<String> =
        when (element) {
            is JsonObject ->
                element.flatMap { (key, value) ->
                    if (key == "expr" && value is JsonPrimitive) listOf(value.content) else expressionsOf(value)
                }
            is JsonArray -> element.flatMap(::expressionsOf)
            else -> emptyList()
        }

    /**
     * The metrics an expression reads and the labels it selects or groups each by. Grouping labels
     * are attributed to every metric in the expression, which is what a query that divides two of
     * them needs anyway.
     */
    private fun referencesOf(expression: String): Map<String, Set<String>> {
        val withoutStrings = expression.replace(Regex(""""[^"]*""""), "\"\"")
        val grouping =
            Regex("""(?:by|without)\s*\(([^)]*)\)""").findAll(withoutStrings).flatMap { match ->
                match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }.toSet()
        val selectors = Regex("""([a-zA-Z_:][a-zA-Z0-9_:]*)\s*(\{([^}]*)\})?""").findAll(withoutStrings)
        return selectors
            .map { it.groupValues[1] to it.groupValues[3] }
            .filter { (name, _) -> name !in PROMQL_WORDS && name !in grouping && !name.startsWith("__") && name.contains('_') }
            .groupBy(
                { it.first },
                { (_, selector) -> Regex("""([a-zA-Z_]+)\s*[=!~]""").findAll(selector).map { it.groupValues[1] }.toSet() },
            )
            .mapValues { (_, labels) -> labels.flatten().toSet() + grouping }
    }
}
