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
import org.opendc.web.api.service.ScenarioService
import org.opendc.web.proto.*
import org.opendc.web.proto.user.*
import java.time.Instant

/**
 * Test suite for [PortfolioScenarioResource].
 */
@QuarkusTest
@TestHTTPEndpoint(PortfolioScenarioResource::class)
class PortfolioScenarioResourceTest {
    @InjectMock
    private lateinit var scenarioService: ScenarioService

    /**
     * Dummy values
     */
    private val dummyProject = Project(0, "test", Instant.now(), Instant.now(), ProjectRole.OWNER)
    private val dummyPortfolio = Portfolio.Summary(1, 1, "test", Targets(emptySet()))
    private val dummyJob = Job(1, JobState.PENDING, Instant.now(), Instant.now(), null)
    private val dummyTrace = Trace("bitbrains", "Bitbrains", "vm")
    private val dummyTopology = Topology.Summary(1, 1, "test", Instant.now(), Instant.now())
    private val dummyScenario = Scenario(
        1,
        1,
        dummyProject,
        dummyPortfolio,
        "test",
        Workload(dummyTrace, 1.0),
        dummyTopology,
        OperationalPhenomena(false, false),
        "test",
        dummyJob
    )

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(scenarioService, ScenarioService::class.java)
    }

    /**
     * Test that tries to obtain a portfolio without token.
     */
    @Test
    fun testGetWithoutToken() {
        Given {
            pathParam("project", "1")
            pathParam("portfolio", "1")
        } When {
            get()
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
            pathParam("portfolio", "1")
        } When {
            get()
        } Then {
            statusCode(403)
        }
    }

    /**
     * Test that tries to obtain a non-existent portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGet() {
        every { scenarioService.findAll("testUser", 1, 1) } returns emptyList()

        Given {
            pathParam("project", "1")
            pathParam("portfolio", "1")
        } When {
            get()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to create a scenario for a portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateNonExistent() {
        every { scenarioService.create("testUser", 1, any(), any()) } returns null

        Given {
            pathParam("project", "1")
            pathParam("portfolio", "1")

            body(Scenario.Create("test", Workload.Spec("test", 1.0), 1, OperationalPhenomena(false, false), "test"))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to create a scenario for a portfolio.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreate() {
        every { scenarioService.create("testUser", 1, 1, any()) } returns dummyScenario

        Given {
            pathParam("project", "1")
            pathParam("portfolio", "1")

            body(Scenario.Create("test", Workload.Spec("test", 1.0), 1, OperationalPhenomena(false, false), "test"))
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
     * Test to create a project with an empty body.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testCreateEmpty() {
        Given {
            pathParam("project", "1")
            pathParam("portfolio", "1")

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
            pathParam("project", "1")
            pathParam("portfolio", "1")

            body(Scenario.Create("", Workload.Spec("test", 1.0), 1, OperationalPhenomena(false, false), "test"))
            contentType(ContentType.JSON)
        } When {
            post()
        } Then {
            statusCode(400)
            contentType(ContentType.JSON)
        }
    }
}
