/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.server.rest.user

import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test

/**
 * Test suite for [UserResource].
 */
@QuarkusTest
@TestHTTPEndpoint(UserResource::class)
class UserResourceTest {
    /**
     * Test that tries to obtain the profile of the active user.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testMe() {
        When {
            get("me")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)

            body("userId", Matchers.equalTo("testUser"))
            body("accounting.simulationTime", Matchers.equalTo(0))
            body("accounting.simulationTimeBudget", Matchers.greaterThan(0))
        }
    }

    /**
     * Test that tries to obtain the profile of the active user without authorization.
     */
    @Test
    fun testMeUnauthorized() {
        When {
            get("me")
        } Then {
            statusCode(401)
        }
    }
}
