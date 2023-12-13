/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.rest.user;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import io.restassured.http.ContentType;

/**
 * Test suite for [UserResource].
 */
// @QuarkusTest
// @TestHTTPEndpoint(UserResource.class)
public final class UserResourceTest {
    /**
     * Test that tries to obtain the profile of the active user.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "testUser",
    //            roles = {"openid"})
    public void testMe() {
        when().get("me")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("userId", equalTo("testUser"))
                .body("accounting.simulationTime", equalTo(0))
                .body("accounting.simulationTimeBudget", greaterThan(0));
    }

    /**
     * Test that tries to obtain the profile of the active user without authorization.
     */
    //    @Test
    public void testMeUnauthorized() {
        when().get("me").then().statusCode(401);
    }
}
