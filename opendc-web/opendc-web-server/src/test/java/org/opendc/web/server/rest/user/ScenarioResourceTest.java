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
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendc.web.proto.JobState;
import org.opendc.web.proto.OperationalPhenomena;
import org.opendc.web.proto.Targets;
import org.opendc.web.proto.Trace;
import org.opendc.web.proto.Workload;
import org.opendc.web.proto.user.Job;
import org.opendc.web.proto.user.Portfolio;
import org.opendc.web.proto.user.Project;
import org.opendc.web.proto.user.ProjectRole;
import org.opendc.web.proto.user.Scenario;
import org.opendc.web.proto.user.Topology;
import org.opendc.web.server.service.ScenarioService;

/**
 * Test suite for [ScenarioResource].
 */
@QuarkusTest
@TestHTTPEndpoint(ScenarioResource.class)
public final class ScenarioResourceTest {
    @InjectMock
    private ScenarioService scenarioService;

    /**
     * Dummy values
     */
    private final Project dummyProject = new Project(0, "test", Instant.now(), Instant.now(), ProjectRole.OWNER);

    private final Portfolio.Summary dummyPortfolio = new Portfolio.Summary(1, 1, "test", new Targets(Set.of(), 1));
    private final Job dummyJob = new Job(1, JobState.PENDING, Instant.now(), Instant.now(), null);
    private final Trace dummyTrace = new Trace("bitbrains", "Bitbrains", "vm");
    private final Topology.Summary dummyTopology = new Topology.Summary(1, 1, "test", Instant.now(), Instant.now());
    private final Scenario dummyScenario = new Scenario(
            1,
            1,
            dummyProject,
            dummyPortfolio,
            "test",
            new Workload(dummyTrace, 1.0),
            dummyTopology,
            new OperationalPhenomena(false, false),
            "test",
            dummyJob);

    /**
     * Test that tries to obtain a scenario without token.
     */
    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(401);
    }

    /**
     * Test that tries to obtain a scenario with an invalid scope.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(403);
    }

    /**
     * Test that tries to obtain a non-existent scenario.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGetNonExisting() {
        Mockito.when(scenarioService.findOne("testUser", 1, 1)).thenReturn(null);

        given().pathParam("project", "1")
                .when()
                .get("/1")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a scenario.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGetExisting() {
        Mockito.when(scenarioService.findOne("testUser", 1, 1)).thenReturn(dummyScenario);

        given().pathParam("project", "1")
                .when()
                .get("/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1));
    }

    /**
     * Test to delete a non-existent scenario.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testDeleteNonExistent() {
        Mockito.when(scenarioService.delete("testUser", 1, 1)).thenReturn(null);

        given().pathParam("project", "1").when().delete("/1").then().statusCode(404);
    }

    /**
     * Test to delete a scenario.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testDelete() {
        Mockito.when(scenarioService.delete("testUser", 1, 1)).thenReturn(dummyScenario);

        given().pathParam("project", "1")
                .when()
                .delete("/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
