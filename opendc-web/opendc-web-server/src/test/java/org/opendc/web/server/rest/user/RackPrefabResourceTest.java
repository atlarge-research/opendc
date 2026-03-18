/*
 * Copyright (c) 2024 AtLarge Research
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
import org.opendc.web.proto.topology.Rack;
import org.opendc.web.proto.user.RackPrefab;

/**
 * Test suite for the {@link RackPrefabResource} endpoint.
 */
@QuarkusTest
@TestHTTPEndpoint(RackPrefabResource.class)
public class RackPrefabResourceTest {
    /**
     * Test that tries to create a rack prefab for a project.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testCreate() {
        Rack rack = new Rack("1", "Rack 1", 42, 1000.0, List.of());
        given().pathParam("project", "1")
                .body(new RackPrefab.Create("Test Rack Prefab", rack))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Test Rack Prefab"))
                .body("project.id", equalTo(1));
    }

    /**
     * Test that tries to get all rack prefabs for a project.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testGetAll() {
        given().pathParam("project", "1")
                .when()
                .get()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    /**
     * Test that tries to delete a rack prefab.
     */
    @Test
    @TestSecurity(
            user = "test_user_1",
            roles = {"openid"})
    public void testDelete() {
        Rack rack = new Rack("1", "Rack 1", 42, 1000.0, List.of());
        int number = given().pathParam("project", "1")
                .body(new RackPrefab.Create("Test Rack Prefab to Delete", rack))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .path("number");

        given().pathParam("project", "1")
                .pathParam("number", number)
                .when()
                .delete("/{number}")
                .then()
                .statusCode(200);
    }
}
