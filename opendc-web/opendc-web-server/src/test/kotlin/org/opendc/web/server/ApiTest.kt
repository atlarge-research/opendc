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

package org.opendc.web.server

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.opendc.sdk.model.serialization.SdkJson
import java.nio.file.Files
import java.nio.file.Path

/** Shared helpers for the REST test suite: one Json boundary and fixture loading. */
object ApiTest {
    val json: Json = Json(from = SdkJson.json) { prettyPrint = false }

    fun requestJson(): RequestSpecification = RestAssured.given().contentType(ContentType.JSON)

    fun fixture(name: String): String =
        checkNotNull(ApiTest::class.java.getResourceAsStream("/fixtures/$name")) {
            "missing test fixture $name"
        }.readBytes().decodeToString()

    fun fixtureElement(name: String): JsonElement = json.parseToJsonElement(fixture(name))

    /** A fixture as a file on disk, for the ones that are not text. */
    fun fixturePath(name: String): Path =
        Path.of(checkNotNull(ApiTest::class.java.getResource("/fixtures/$name")) { "missing test fixture $name" }.toURI())

    fun fixtureBytes(name: String): ByteArray = Files.readAllBytes(fixturePath(name))
}
