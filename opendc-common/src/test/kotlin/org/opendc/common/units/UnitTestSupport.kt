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

package org.opendc.common.units

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.assertAll
import kotlin.math.abs
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

/**
 * One way of building a unit ([of]) and reading it back ([to]), with how many reference units one of it is.
 * The reference values are written out by hand, so the conversion chains are checked against numbers that do not come
 * from the code under test.
 */
internal class Scale<T>(
    val name: String,
    val inReference: Double,
    val of: (Double) -> T,
    val to: (T) -> Double,
)

/** Zero, fractions that are not exact in binary, a negative, and large values. */
internal val SAMPLE_VALUES = listOf(0.0, 1.0, 0.1, 1.0 / 3, -2.5, 123_456.789, 1e9)

/**
 * Asserts that [actual] equals [expected] up to a relative error of [relTol]. The default leaves room for the few ulps
 * of rounding that a chain of conversions adds, and nothing more.
 */
internal fun assertApprox(
    expected: Double,
    actual: Double,
    message: String? = null,
    relTol: Double = 1e-12,
) {
    if (abs(expected - actual) > relTol * maxOf(abs(expected), abs(actual))) {
        fail("${message?.let { "$it: " }.orEmpty()}expected <$expected> but was <$actual>")
    }
}

/**
 * Asserts that one of every scale reads back as the right amount of every other scale, and that every scale returns the
 * value it was built from.
 */
internal fun <T> assertScales(scales: List<Scale<T>>) {
    val checks =
        scales.flatMap { from ->
            scales.map { to -> { assertApprox(from.inReference / to.inReference, to.to(from.of(1.0)), "1 ${from.name} in ${to.name}") } } +
                SAMPLE_VALUES.map { v -> { assertApprox(v, from.to(from.of(v)), "$v ${from.name} back to ${from.name}") } }
        }
    assertAll(checks)
}

/** Decodes [str] as the JSON string a config file would hold. */
internal inline fun <reified T> parse(str: String): T = Json.decodeFromString<T>(JsonPrimitive(str).toString())

/** Asserts that every string fails to parse, with an error message that contains [hint]. */
internal inline fun <reified T> assertRejects(
    hint: String,
    vararg strs: String,
) {
    assertAll(
        strs.map { str ->
            {
                val e = assertFailsWith<IllegalArgumentException>("parsing \"$str\"") { parse<T>(str) }
                assertContains(e.message.orEmpty(), hint)
            }
        },
    )
}

/** Encodes [value] the way it is written to a file, without the JSON quotes. */
internal inline fun <reified T> write(value: T): String = Json.encodeToJsonElement(value).jsonPrimitive.content

/** Asserts that every string parses to the unit it is paired with. */
internal inline fun <reified T> assertParses(vararg cases: Pair<String, T>) {
    assertAll(cases.map { (str, expected) -> { assertEquals(expected, parse<T>(str), "parsing \"$str\"") } })
}

/**
 * Asserts that every value is formatted (without decimals, which keeps it independent of the locale) as the string it is
 * paired with, and that the string parses back to the value, so every label [Unit.fmtValue] prints is one we can read.
 */
internal inline fun <reified T : Unit<T>> assertFormats(vararg cases: Pair<T, String>) {
    assertAll(
        cases.map { (value, expected) ->
            {
                assertEquals(expected, value.fmtValue("%.0f"))
                assertEquals(value, parse<T>(expected), "parsing back \"$expected\"")
            }
        },
    )
}

/** Asserts that every value is written as a string that reads back as exactly the same value. */
internal inline fun <reified T> assertRoundTrips(vararg values: T) {
    assertAll(
        values.map { value ->
            {
                val written = Json.encodeToString(value)
                assertEquals(value, Json.decodeFromString<T>(written), "reading back $written")
            }
        },
    )
}
