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
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.web.server.TestAccounts

/**
 * Personal access tokens act as their owner wherever they are presented, and stop the moment they
 * are revoked. A token that resolves to nobody must never fall back to the local account.
 */
@QuarkusTest
class AccessTokensResourceTest {
    @Test
    fun `a minted token acts as its owner, and its secret is shown once`() {
        val person = TestAccounts.person()

        val minted = person.request().body("""{"name":"laptop"}""").post("/api/v1/me/tokens").then().statusCode(201).extract()
        val secret = minted.path<String>("secret")
        assertTrue(secret.startsWith(minted.path<String>("token.prefix")))
        assertTrue(secret.startsWith("odc_pat_"))

        given().header(
            "Authorization",
            "Bearer $secret",
        ).get("/api/v1/me").then().statusCode(200).body("handle.name", equalTo(person.handle))
        person.request().get("/api/v1/me/tokens").then().statusCode(200).body("$", everyItem(not(hasKey("secret"))))
    }

    @Test
    fun `records that a token was used`() {
        val person = TestAccounts.person()
        val secret = person.request().body("""{"name":"ci"}""").post("/api/v1/me/tokens").then().extract().path<String>("secret")
        person.request().get("/api/v1/me/tokens").then().body("find { it.name == 'ci' }.lastUse.type", equalTo("unused"))

        given().header("Authorization", "Bearer $secret").get("/api/v1/projects").then().statusCode(200)

        person.request().get("/api/v1/me/tokens").then().body("find { it.name == 'ci' }.lastUse.type", equalTo("used"))
    }

    @Test
    fun `a revoked, unknown or foreign token is refused rather than read as the local account`() {
        val person = TestAccounts.person()
        val minted = person.request().body("""{"name":"old"}""").post("/api/v1/me/tokens").then().extract()

        TestAccounts.person().request().delete("/api/v1/me/tokens/${minted.path<String>("token.id")}").then().statusCode(404)
        person.request().delete("/api/v1/me/tokens/${minted.path<String>("token.id")}").then().statusCode(204)

        given().header("Authorization", "Bearer ${minted.path<String>("secret")}").get("/api/v1/me").then().statusCode(401)
        given().header("Authorization", "Bearer odc_pat_invented").get("/api/v1/me").then().statusCode(401)
    }

    @Test
    fun `an account still choosing its handle cannot mint tokens`() {
        TestAccounts.newcomer().request().body("""{"name":"early"}""").post("/api/v1/me/tokens").then().statusCode(403)
    }
}
