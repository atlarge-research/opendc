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

package org.opendc.web.server.rest

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

/**
 * A distribution serves the frontend from the API's own origin. The export writes `project.html`
 * while the app links to `/project?id=...`, so a reloaded or shared deep link has to find the page,
 * and none of it may shadow the API.
 */
@QuarkusTest
@TestProfile(FrontendPagesTest.WithExport::class)
class FrontendPagesTest {
    class WithExport : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> = mapOf("opendc.frontend.directory" to "src/test/resources/frontend-export")
    }

    @Test
    fun aDeepLinkFindsThePageItNames() {
        given().get("/project?id=3f5b1f4a").then().statusCode(200).body(containsString("project page"))
    }

    @Test
    fun theRootIsTheIndexPageAndAssetsAreServedAsFiles() {
        given().get("/").then().statusCode(200).body(containsString("projects page"))
        given().get("/_next/static/app.js").then().statusCode(200).body(containsString("console.log"))
    }

    @Test
    fun theApiIsNeverShadowed() {
        given().get("/api/v1/config").then().statusCode(200).body("auth.type", equalTo("anonymous"))
    }

    @Test
    fun aPathThatNamesNothingIsNotFound() {
        given().get("/nowhere").then().statusCode(404)
        given().urlEncodingEnabled(false).get("/%2e%2e/%2e%2e/build.gradle.kts").then().statusCode(404)
    }
}
