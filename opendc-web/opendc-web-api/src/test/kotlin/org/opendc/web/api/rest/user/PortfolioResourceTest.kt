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
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.api.service.PortfolioService
import org.opendc.web.proto.Targets
import org.opendc.web.proto.user.Portfolio
import org.opendc.web.proto.user.Project
import org.opendc.web.proto.user.ProjectRole
import java.time.Instant

/**
 * Test suite for [PortfolioResource].
 */
@QuarkusTest
@TestHTTPEndpoint(PortfolioResource::class)
class PortfolioResourceTest {
    @InjectMock
    private lateinit var portfolioService: PortfolioService

    /**
     * Dummy project and portfolio
     */
    private val dummyProject = Project(1, "test", Instant.now(), Instant.now(), ProjectRole.OWNER)
    private val dummyPortfolio = Portfolio(1, 1, dummyProject, "test", Targets(emptySet(), 1), emptyList())

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(portfolioService, PortfolioService::class.java)
    }

    /**
     * Test that tries to obtain the list of portfolios belonging to a project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetForProject() {
        every { portfolioService.findAll("testUser", 1) } returns emptyList()

        Given {
            pathParam("project", "1")
        } When {
            get()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to create a topology for a project.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateNonExistent() {
        every { portfolioService.create("testUser", 1, any()) } returns null

        Given {
            pathParam("project", "1")

            body(Portfolio.Create("test", Targets(emptySet(), 1)))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to create a portfolio for a scenario.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreate() {
        every { portfolioService.create("testUser", 1, any()) } returns dummyPortfolio

        Given {
            pathParam("project", "1")

            body(Portfolio.Create("test", Targets(emptySet(), 1)))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", Matchers.equalTo(1))
            body("name", Matchers.equalTo("test"))
        }
    }

    /**
     * Test to create a portfolio with an empty body.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateEmpty() {
        Given {
            pathParam("project", "1")

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
     * Test to create a portfolio with a blank name.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateBlankName() {
        Given {
            pathParam("project", "1")

            body(Portfolio.Create("", Targets(emptySet(), 1)))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(400)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to obtain a portfolio without token.
     */
    @Test
    fun testGetWithoutToken() {
        Given {
            pathParam("project", "1")
        } When {
            get("/1")
        } Then {
            statusCode(401)
        }
    }

    /**
     * Test that tries to obtain a portfolio with an invalid scope.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testGetInvalidToken() {
        Given {
            pathParam("project", "1")
        } When {
            get("/1")
        } Then {
            statusCode(403)
        }
    }

    /**
     * Test that tries to obtain a non-existent portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetNonExisting() {
        every { portfolioService.findOne("testUser", 1, 1) } returns null

        Given {
            pathParam("project", "1")
        } When {
            get("/1")
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to obtain a portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetExisting() {
        every { portfolioService.findOne("testUser", 1, 1) } returns dummyPortfolio

        Given {
            pathParam("project", "1")
        } When {
            get("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", Matchers.equalTo(1))
        }
    }

    /**
     * Test to delete a non-existent portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDeleteNonExistent() {
        every { portfolioService.delete("testUser", 1, 1) } returns null

        Given {
            pathParam("project", "1")
        } When {
            delete("/1")
        } Then {
            statusCode(404)
        }
    }

    /**
     * Test to delete a portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDelete() {
        every { portfolioService.delete("testUser", 1, 1) } returns dummyPortfolio

        Given {
            pathParam("project", "1")
        } When {
            delete("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
        }
    }
}
