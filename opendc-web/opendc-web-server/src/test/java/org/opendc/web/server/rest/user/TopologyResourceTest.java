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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendc.web.proto.user.Topology;

/**
 * Test suite for {@link TopologyResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(TopologyResource.class)
public final class TopologyResourceTest {
    /**
     * Test that tries to obtain the list of topologies of a project without proper authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_4",
            roles = {"openid"})
    public void testGetAllWithoutAuth() {
        given().pathParam("project", "1")
                .when()
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(equalTo("[]"));
    }

    /**
     * Test that tries to obtain the list of topologies belonging to a project.
     * TODO: check if any topology comes back
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGetAll() {
        given().pathParam("project", "1").when().get().then().statusCode(200);
    }

    /**
     * Test that tries to create a topology for a project that does not exist.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateNonExistent() {
        given().pathParam("project", "0")
                .body(new Topology.Create("test", List.of()))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404);
    }

    /**
     * Test that tries to create a topology for a project while not authorized.
     * TODO: should probably return 403, but this does not work in the current system
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateUnauthorized() {
        given().pathParam("project", "2")
                .body(new Topology.Create("test", List.of()))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(404);
    }

    /**
     * Test that tries to create a topology for a project.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreate() {
        given().pathParam("project", "1")
                .body(new Topology.Create("Test Topology New", List.of()))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Test Topology New"));
    }

    /**
     * Test to create a topology with an empty body.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateEmpty() {
        given().pathParam("project", "1")
                .body("{}")
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    /**
     * Test to create a topology with a blank name.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreateBlankName() {
        given().pathParam("project", "1")
                .body(new Topology.Create("", List.of()))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    /**
     * Test that tries to obtain a topology without token.
     */
    @Test
    public void testGetWithoutToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(401);
    }

    /**
     * Test that tries to obtain a topology with an invalid scope.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"runner"})
    public void testGetInvalidToken() {
        given().pathParam("project", "1").when().get("/1").then().statusCode(403);
    }

    /**
     * Test that tries to obtain a non-existent topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGetNonExisting() {
        given().pathParam("project", "1").when().get("/0").then().statusCode(404);
    }

    /**
     * Test that tries to obtain a topology without authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGetUnauthorized() {
        given().pathParam("project", "2").when().get("/1").then().statusCode(404);
    }

    /**
     * Test that tries to obtain a topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
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
     * Test to delete a non-existent topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testUpdateNonExistent() {
        given().pathParam("project", "1")
                .body(new Topology.Update(List.of()))
                .contentType(ContentType.JSON)
                .when()
                .put("/0")
                .then()
                .statusCode(404);
    }

    /**
     * Test to delete a topology without authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testUpdateUnauthorized() {
        given().pathParam("project", "2")
                .body(new Topology.Update(List.of()))
                .contentType(ContentType.JSON)
                .when()
                .put("/1")
                .then()
                .statusCode(404);
    }

    /**
     * Test to update a topology as a viewer.
     * TODO: should return 403, but currently returns 404
     */
    @Test
    @TestSecurity(
            user = "test_user_2",
            roles = {"openid"})
    public void testUpdateAsViewer() {
        given().pathParam("project", "1")
                .body(new Topology.Update(List.of()))
                .contentType(ContentType.JSON)
                .when()
                .put("/1")
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON);
    }

    /**
     * Test to update a topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testUpdate() {
        given().pathParam("project", "1")
                .body(new Topology.Update(List.of()))
                .contentType(ContentType.JSON)
                .when()
                .put("/1")
                .then()
                .statusCode(200);
    }

    /**
     * Test to delete a non-existent topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testDeleteNonExistent() {
        given().pathParam("project", "1").when().delete("/0").then().statusCode(404);
    }

    /**
     * Test to delete a topology without authorization.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testDeleteUnauthorized() {
        given().pathParam("project", "2").when().delete("/1").then().statusCode(404);
    }

    /**
     * Test to delete a topology as a viewer.
     */
    @Test
    @TestSecurity(
            user = "test_user_2",
            roles = {"openid"})
    public void testDeleteAsViewer() {
        given().pathParam("project", "1").when().delete("/1").then().statusCode(403);
    }

    /**
     * Test to delete a topology as a viewer.
     */
    @Test
    @TestSecurity(
            user = "test_user_3",
            roles = {"openid"})
    public void testDeleteAsEditor() {
        given().pathParam("project", "1").when().delete("/2").then().statusCode(200);
    }

    /**
     * Test to delete a topology.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testDelete() {
        given().pathParam("project", "1").when().delete("/3").then().statusCode(200);
    }

    /**
     * Test to delete a topology that is still being used by a scenario.
     * TODO: fix later
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testDeleteUsed() {
        given().pathParam("project", "1")
                .when()
                .delete("/4") // Topology 1 is still used by scenario 1 and 2
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON);
    }
}
