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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.List;
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
 * Test suite for {@link PortfolioScenarioResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(PortfolioScenarioResource.class)
public final class PortfolioScenarioResourceTest {
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
     * Test that tries to obtain a portfolio without token.
     */
    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .when()
                .get()
                .then()
                .statusCode(401);
    }

    /**
     * Test that tries to obtain a portfolio with an invalid scope.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .when()
                .get()
                .then()
                .statusCode(403);
    }

    /**
     * Test that tries to obtain a non-existent portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGet() {
        Mockito.when(scenarioService.findAll("testUser", 1, 1)).thenReturn(List.of());

        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .when()
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreateNonExistent() {
        Mockito.when(scenarioService.create(eq("testUser"), eq(1L), anyInt(), any()))
                .thenReturn(null);

        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .body(new Scenario.Create(
                        "test", new Workload.Spec("test", 1.0), 1, new OperationalPhenomena(false, false), "test"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a scenario for a portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreate() {
        Mockito.when(scenarioService.create(eq("testUser"), eq(1L), eq(1), any()))
                .thenReturn(dummyScenario);

        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .body(new Scenario.Create(
                        "test", new Workload.Spec("test", 1.0), 1, new OperationalPhenomena(false, false), "test"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1))
                .body("name", equalTo("test"));
    }

    /**
     * Test to create a project with an empty body.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreateEmpty() {
        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
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
            user = "testUser",
            roles = {"openid"})
    public void testCreateBlankName() {
        given().pathParam("project", "1")
                .pathParam("portfolio", "1")
                .body(new Scenario.Create(
                        "", new Workload.Spec("test", 1.0), 1, new OperationalPhenomena(false, false), "test"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }
}
