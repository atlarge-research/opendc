/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.api.rest.user

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.api.service.ProjectService
import org.opendc.web.proto.user.Project
import org.opendc.web.proto.user.ProjectRole
import java.time.Instant

/**
 * Test suite for [ProjectResource].
 */
@QuarkusTest
@TestHTTPEndpoint(ProjectResource::class)
class ProjectResourceTest {
    @InjectMock
    private lateinit var projectService: ProjectService

    /**
     * Dummy values.
     */
    private val dummyProject = Project(0, "test", Instant.now(), Instant.now(), ProjectRole.OWNER)

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(projectService, ProjectService::class.java)
    }

    /**
     * Test that tries to obtain all projects without token.
     */
    @Test
    fun testGetAllWithoutToken() {
        When {
            get()
        } Then {
            statusCode(401)
        }
    }

    /**
     * Test that tries to obtain all projects with an invalid scope.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testGetAllWithInvalidScope() {
        When {
            get()
        } Then {
            statusCode(403)
        }
    }

    /**
     * Test that tries to obtain all project for a user.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetAll() {
        val projects = listOf(dummyProject)
        every { projectService.findWithUser("testUser") } returns projects

        When {
            get()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("get(0).name", equalTo("test"))
        }
    }

    /**
     * Test that tries to obtain a non-existent project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetNonExisting() {
        every { projectService.findWithUser("testUser", 1) } returns null

        When {
            get("/1")
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to obtain a job.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetExisting() {
        every { projectService.findWithUser("testUser", 1) } returns dummyProject

        When {
            get("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", equalTo(0))
        }
    }

    /**
     * Test that tries to create a project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreate() {
        every { projectService.createForUser("testUser", "test") } returns dummyProject

        Given {
            body(Project.Create("test"))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", equalTo(0))
            body("name", equalTo("test"))
        }
    }

    /**
     * Test to create a project with an empty body.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateEmpty() {
        Given {
            body("{}")
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(400)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test to create a project with a blank name.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateBlankName() {
        Given {
            body(Project.Create(""))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(400)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test to delete a non-existent project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDeleteNonExistent() {
        every { projectService.deleteWithUser("testUser", 1) } returns null

        When {
            delete("/1")
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test to delete a project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDelete() {
        every { projectService.deleteWithUser("testUser", 1) } returns dummyProject

        When {
            delete("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test to delete a project which the user does not own.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDeleteNonOwner() {
        every { projectService.deleteWithUser("testUser", 1) } throws IllegalArgumentException("User does not own project")

        When {
            delete("/1")
        } Then {
            statusCode(403)
            contentType(ContentType.JSON)
        }
    }
}
