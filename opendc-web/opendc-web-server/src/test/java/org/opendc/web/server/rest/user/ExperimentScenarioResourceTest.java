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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.opendc.web.proto.OperationalPhenomena;
import org.opendc.web.proto.Workload;
import org.opendc.web.proto.user.Scenario;

/**
 * Test suite for {@link ExperimentScenarioResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(ExperimentScenarioResource.class)
public final class ExperimentScenarioResourceTest {
    /**
     * Test that tries to obtain a experiment without token.
     */
    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .when()
                .get()
                .then()
                .statusCode(401);
    }

    /**
     * Test that tries to obtain a experiment with an invalid scope.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .when()
                .get()
                .then()
                .statusCode(403);
    }

    /**
     * Test that tries to obtain a scenario without authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGetUnauthorized() {
        given().pathParam("project", "2")
                .pathParam("experiment", "1")
                .when()
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a scenario.
     * TODO: shouldn't this be all scenarios?
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGet() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .when()
                .get()
                .then()
                .statusCode(200);
    }

    /**
     * Test that tries to create a scenario for a experiment that does not exist in a project that can be accessed.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateNonExistent() {
        given().pathParam("project", "1")
                .pathParam("experiment", "0")
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("test", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a experiment without authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateUnauthorized() {
        given().pathParam("project", "2")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("test", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a experiment as a viewer.
     */
    @Test
    @TestSecurity(
            user = "test_user_2",
            roles = {"openid"})
    public void testCreateAsViewer() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("test", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a experiment.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreate() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "Test Scenario New",
                        new Workload.Spec("bitbrains-small", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Test Scenario New"));
    }

    /**
     * Test to create a project with an empty body.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateEmpty() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body("{}")
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test to create a project with a blank name.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateBlankName() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "", new Workload.Spec("test", 1.0), 1, new OperationalPhenomena(false, false), "test", null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a experiment with an unknown Topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateUnknownTopology() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("bitbrains-small", 1.0),
                        -1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a experiment with an unknown Trace.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateUnknownTrace() {
        given().pathParam("project", "1")
                .pathParam("experiment", "1")
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("unknown", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test",
                        null))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }
}
