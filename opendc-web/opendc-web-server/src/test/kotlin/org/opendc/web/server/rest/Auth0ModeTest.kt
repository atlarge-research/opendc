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

import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.quarkus.test.oidc.server.OidcWiremockTestResource
import io.restassured.RestAssured.given
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.web.server.model.UserAccount
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val AUDIENCE = "https://api.opendc.test"
private const val ADMIN_SUBJECT = "auth0|root"
private const val OWN_DATABASE = "jdbc:h2:mem:opendc-auth0;DB_CLOSE_DELAY=-1;INIT=CREATE TYPE IF NOT EXISTS \"JSONB\" AS json"

/**
 * Auth0 mode against an OIDC provider on WireMock, in process: tokens are verified, a first sight
 * makes an account that must choose its handle, and nothing falls back to an implicit account.
 */
@QuarkusTest
@TestProfile(Auth0ModeTest.Auth0Mode::class)
@QuarkusTestResource(OidcWiremockTestResource::class, restrictToAnnotatedClass = true)
class Auth0ModeTest {
    class Auth0Mode : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> =
            mapOf(
                // A database of its own: the in-memory one the other applications of this JVM share
                // may be closed under it when one of them stops.
                "quarkus.datasource.jdbc.url" to OWN_DATABASE,
                "opendc.auth.mode" to "auth0",
                "quarkus.oidc.tenant-enabled" to "true",
                "quarkus.oidc.auth-server-url" to "\${keycloak.url}/realms/quarkus/",
                "quarkus.oidc.token.issuer" to "https://server.example.com",
                "quarkus.oidc.token.audience" to AUDIENCE,
                "opendc.auth.auth0.client-id" to "test-spa",
                "opendc.auth.admins" to ADMIN_SUBJECT,
            )
    }

    @Test
    fun `config names the tenant the frontend signs in through`() {
        given()
            .get("/api/v1/config")
            .then()
            .statusCode(200)
            .body("auth.type", equalTo("auth0"))
            .body("auth.clientId", equalTo("test-spa"))
            .body("auth.audience", equalTo(AUDIENCE))
    }

    @Test
    fun `a request without a token is asked to sign in`() {
        given().get("/api/v1/projects").then().statusCode(401).body("title", equalTo("Sign in to continue"))
    }

    // A download link is followed by a browser that cannot attach a token, so it is checked on its
    // own: a bad one is a missing download, never a demand to sign in.
    @Test
    fun `a download link needs no token`() {
        given()
            .get("/api/v1/downloads/not.signed")
            .then()
            .statusCode(404)
            .body("title", equalTo("This download link is invalid or has expired"))
    }

    @Test
    fun `a token for another audience is refused`() {
        given().auth().oauth2(token("auth0|${UUID.randomUUID()}", audience = "https://elsewhere")).get("/api/v1/me").then().statusCode(401)
    }

    @Test
    fun `a first sign-in makes an account that chooses its handle before anything else`() {
        val subject = "auth0|${UUID.randomUUID()}"

        signedIn(subject).get("/api/v1/me").then().statusCode(200).body("handle.type", equalTo("provisional"))
        signedIn(subject).get("/api/v1/projects").then().statusCode(403)

        val handle = "auth-${UUID.randomUUID().toString().take(8)}"
        signedIn(subject).body("""{"handle":"$handle","displayName":"Signed In"}""").put("/api/v1/me/profile").then().statusCode(200)
        signedIn(subject).get("/api/v1/projects").then().statusCode(200)
    }

    // A browser opening the app fires several requests at once, all of them a first sight.
    @Test
    fun `parallel first requests make one account`() {
        val subject = "auth0|${UUID.randomUUID()}"
        val pool = Executors.newFixedThreadPool(4)
        val statuses = (1..4).map { pool.submit<Int> { signedIn(subject).get("/api/v1/me").statusCode } }.map { it.get() }
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(listOf(200, 200, 200, 200), statuses)
        QuarkusTransaction.requiringNew().run { assertEquals(1L, UserAccount.count("subject = ?1", subject)) }
    }

    @Test
    fun `a subject named as an administrator is one from its first sign-in`() {
        signedIn(ADMIN_SUBJECT).get("/api/v1/me").then().statusCode(200).body("isAdmin", equalTo(true))
        signedIn("auth0|${UUID.randomUUID()}").get("/api/v1/me").then().statusCode(200).body("isAdmin", equalTo(false))
    }

    @Test
    fun `a deactivated account is told so rather than signed out`() {
        val subject = "auth0|${UUID.randomUUID()}"
        signedIn(subject).get("/api/v1/me").then().statusCode(200)
        QuarkusTransaction.requiringNew().run { checkNotNull(UserAccount.findBySubject(subject)).deactivate(Instant.now()) }

        signedIn(subject).get("/api/v1/me").then().statusCode(403).body("title", equalTo("This account has been deactivated"))
    }

    private fun signedIn(subject: String): RequestSpecification = given().auth().oauth2(token(subject)).contentType("application/json")

    private fun token(
        subject: String,
        audience: String = AUDIENCE,
    ): String = OidcWiremockTestResource.generateJwtToken("someone", setOf("user"), subject, "Bearer", setOf(audience))
}
