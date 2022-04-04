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
 * Test suite for [ScenarioResource].
 */
@QuarkusTest
@TestHTTPEndpoint(ScenarioResource::class)
class ScenarioResourceTest {
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
     * Test that tries to obtain a scenario without token.
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
     * Test that tries to obtain a scenario with an invalid scope.
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
     * Test that tries to obtain a non-existent scenario.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetNonExisting() {
        every { scenarioService.findOne("testUser", 1, 1) } returns null

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
     * Test that tries to obtain a scenario.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testGetExisting() {
        every { scenarioService.findOne("testUser", 1, 1) } returns dummyScenario

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
     * Test to delete a non-existent scenario.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDeleteNonExistent() {
        every { scenarioService.delete("testUser", 1, 1) } returns null

        Given {
            pathParam("project", "1")
        } When {
            delete("/1")
        } Then {
            statusCode(404)
        }
    }

    /**
     * Test to delete a scenario.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testDelete() {
        every { scenarioService.delete("testUser", 1, 1) } returns dummyScenario

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
