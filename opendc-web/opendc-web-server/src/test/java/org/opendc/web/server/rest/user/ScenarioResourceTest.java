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

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.opendc.web.proto.OperationalPhenomena;
import org.opendc.web.proto.Workload;
import org.opendc.web.proto.user.Scenario;

/**
 * Test suite for {@link ScenarioResource}.
 */
// @QuarkusTest
// @TestHTTPEndpoint(ScenarioResource.class)
public final class ScenarioResourceTest {
    /**
     * Test that tries to obtain all scenarios belonging to a project without authorization.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "unknown",
    //            roles = {"openid"})
    public void testGetAllUnauthorized() {
        given().pathParam("project", "1").when().get().then().statusCode(404).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain all scenarios belonging to a project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetAll() {
        given().pathParam("project", "1").when().get().then().statusCode(200).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a scenario without token.
     */
    //    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(401);
    }

    /**
     * Test that tries to obtain a scenario with an invalid scope.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(403);
    }

    /**
     * Test that tries to obtain a non-existent scenario.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetNonExisting() {
        given().pathParam("project", "1")
                .when()
                .get("/0")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a scenario.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "unknown",
    //            roles = {"openid"})
    public void testGetExistingUnauthorized() {
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
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetExisting() {
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
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testDeleteNonExistent() {
        given().pathParam("project", "1").when().delete("/0").then().statusCode(404);
    }

    /**
     * Test to delete a scenario without authorization.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "unknown",
    //            roles = {"openid"})
    public void testDeleteUnauthorized() {
        given().pathParam("project", "1").when().delete("/1").then().statusCode(404);
    }

    /**
     * Test to delete a scenario as a viewer.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "viewer",
    //            roles = {"openid"})
    public void testDeleteAsViewer() {
        given().pathParam("project", "1").when().delete("/1").then().statusCode(403);
    }

    /**
     * Test to delete a scenario.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testDelete() {
        RequestSpecification spec = new RequestSpecBuilder()
                .setBasePath("/projects/1/portfolios/1/scenarios")
                .build();

        int number = given(spec)
                .body(new Scenario.Create(
                        "test",
                        new Workload.Spec("bitbrains-small", 1.0),
                        1,
                        new OperationalPhenomena(false, false),
                        "test"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("number");

        given().pathParam("project", "1")
                .when()
                .delete("/" + number)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
