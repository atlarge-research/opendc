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

package org.opendc.web.server.rest.runner;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import org.opendc.web.proto.JobState;

/**
 * Test suite for {@link JobResource}.
 */
// @QuarkusTest
// @TestHTTPEndpoint(JobResource.class)
public final class JobResourceTest {
    /**
     * Test that tries to query the pending jobs without token.
     */
    //    @Test
    public void testQueryWithoutToken() {
        when().get().then().statusCode(401);
    }

    /**
     * Test that tries to query the pending jobs for a user.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"openid"})
    public void testQueryInvalidScope() {
        when().get().then().statusCode(403);
    }

    /**
     * Test that tries to query the pending jobs for a runner.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testQuery() {
        when().get().then().statusCode(200).contentType(ContentType.JSON).body("get(0).state", equalTo("PENDING"));
    }

    /**
     * Test that tries to obtain a non-existent job.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testGetNonExisting() {
        when().get("/0").then().statusCode(404).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a job.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testGetExisting() {
        when().get("/1").then().statusCode(200).contentType(ContentType.JSON).body("id", equalTo(1));
    }

    /**
     * Test that tries to update a non-existent job.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testUpdateNonExistent() {
        given().body(new org.opendc.web.proto.runner.Job.Update(JobState.PENDING, 0, null))
                .contentType(ContentType.JSON)
                .when()
                .post("/0")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to update a job.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testUpdateState() {
        given().body(new org.opendc.web.proto.runner.Job.Update(JobState.CLAIMED, 0, null))
                .contentType(ContentType.JSON)
                .when()
                .post("/2")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("state", equalTo(JobState.CLAIMED.toString()));
    }

    /**
     * Test that tries to update a job with invalid input.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "test",
    //            roles = {"runner"})
    public void testUpdateInvalidInput() {
        given().body("{ \"test\": \"test\" }")
                .contentType(ContentType.JSON)
                .when()
                .post("/1")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }
}
