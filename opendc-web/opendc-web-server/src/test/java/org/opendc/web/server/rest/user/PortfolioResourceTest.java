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
import org.opendc.web.proto.Targets;
import org.opendc.web.proto.user.Portfolio;
import org.opendc.web.proto.user.Project;
import org.opendc.web.proto.user.ProjectRole;
import org.opendc.web.server.service.PortfolioService;

/**
 * Test suite for {@link PortfolioResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(PortfolioResource.class)
public final class PortfolioResourceTest {
    @InjectMock
    private PortfolioService portfolioService;

    /**
     * Dummy project and portfolio
     */
    private final Project dummyProject = new Project(1, "test", Instant.now(), Instant.now(), ProjectRole.OWNER);

    private final Portfolio dummyPortfolio =
            new Portfolio(1, 1, dummyProject, "test", new Targets(Set.of(), 1), List.of());

    /**
     * Test that tries to obtain the list of portfolios belonging to a project.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGetForProject() {
        Mockito.when(portfolioService.findByUser("testUser", 1)).thenReturn(List.of());

        given().pathParam("project", 1).when().get().then().statusCode(200).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a topology for a project.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreateNonExistent() {
        Mockito.when(portfolioService.create(eq("testUser"), eq(1), any())).thenReturn(null);

        given().pathParam("project", "1")
                .body(new Portfolio.Create("test", new Targets(Set.of(), 1)))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to create a portfolio for a scenario.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreate() {
        Mockito.when(portfolioService.create(eq("testUser"), eq(1L), any())).thenReturn(dummyPortfolio);

        given().pathParam("project", "1")
                .body(new Portfolio.Create("test", new Targets(Set.of(), 1)))
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
     * Test to create a portfolio with an empty body.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreateEmpty() {
        given().pathParam("project", "1")
                .body("{}")
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test to create a portfolio with a blank name.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testCreateBlankName() {
        given().pathParam("project", "1")
                .body(new Portfolio.Create("", new Targets(Set.of(), 1)))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a portfolio without token.
     */
    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(401);
    }

    /**
     * Test that tries to obtain a portfolio with an invalid scope.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(403);
    }

    /**
     * Test that tries to obtain a non-existent portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGetNonExisting() {
        Mockito.when(portfolioService.findByUser("testUser", 1, 1)).thenReturn(null);

        given().pathParam("project", "1")
                .when()
                .get("/1")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testGetExisting() {
        Mockito.when(portfolioService.findByUser("testUser", 1, 1)).thenReturn(dummyPortfolio);

        given().pathParam("project", "1")
                .when()
                .get("/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(1));
    }

    /**
     * Test to delete a non-existent portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testDeleteNonExistent() {
        Mockito.when(portfolioService.delete("testUser", 1, 1)).thenReturn(null);

        given().pathParam("project", "1").when().delete("/1").then().statusCode(404);
    }

    /**
     * Test to delete a portfolio.
     */
    @Test
    @TestSecurity(
            user = "testUser",
            roles = {"openid"})
    public void testDelete() {
        Mockito.when(portfolioService.delete("testUser", 1, 1)).thenReturn(dummyPortfolio);

        given().pathParam("project", "1")
                .when()
                .delete("/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
