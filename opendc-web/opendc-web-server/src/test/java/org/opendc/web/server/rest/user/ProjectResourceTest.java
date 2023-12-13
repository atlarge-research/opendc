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
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;

/**
 * Test suite for [ProjectResource].
 */
// @QuarkusTest
// @TestHTTPEndpoint(ProjectResource.class)
public final class ProjectResourceTest {
    /**
     * Test that tries to obtain all projects without token.
     */
    //    @Test
    public void testGetAllWithoutToken() {
        when().get().then().statusCode(401);
    }

    /**
     * Test that tries to obtain all projects with an invalid scope.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"runner"})
    public void testGetAllWithInvalidScope() {
        when().get().then().statusCode(403);
    }

    /**
     * Test that tries to obtain all project for a user.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetAll() {
        when().get().then().statusCode(200).contentType(ContentType.JSON).body("get(0).name", equalTo("Test Project"));
    }

    /**
     * Test that tries to obtain a non-existent project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetNonExisting() {
        when().get("/0").then().statusCode(404).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain a project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testGetExisting() {
        when().get("/1").then().statusCode(200).contentType(ContentType.JSON).body("id", equalTo(1));
    }

    /**
     * Test that tries to create a project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testCreate() {
        given().body(new org.opendc.web.proto.user.Project.Create("test"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("test"));
    }

    /**
     * Test to create a project with an empty body.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testCreateEmpty() {
        given().body("{}")
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
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testCreateBlankName() {
        given().body(new org.opendc.web.proto.user.Project.Create(""))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON);
    }

    /**
     * Test to delete a non-existent project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testDeleteNonExistent() {
        when().delete("/0").then().statusCode(404).contentType(ContentType.JSON);
    }

    /**
     * Test to delete a project.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "owner",
    //            roles = {"openid"})
    public void testDelete() {
        int id = given().body(new org.opendc.web.proto.user.Project.Create("Delete Project"))
                .contentType(ContentType.JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("id");

        when().delete("/" + id).then().statusCode(200).contentType(ContentType.JSON);
    }

    /**
     * Test to delete a project which the user does not own.
     */
    //    @Test
    //    @TestSecurity(
    //            user = "viewer",
    //            roles = {"openid"})
    public void testDeleteNonOwner() {
        when().delete("/1").then().statusCode(403).contentType(ContentType.JSON);
    }
}
