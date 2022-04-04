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

package org.opendc.web.api.rest.runner

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
import org.opendc.web.api.service.JobService
import org.opendc.web.proto.*
import org.opendc.web.proto.Targets
import org.opendc.web.proto.runner.Job
import org.opendc.web.proto.runner.Portfolio
import org.opendc.web.proto.runner.Scenario
import org.opendc.web.proto.runner.Topology
import java.time.Instant

/**
 * Test suite for [JobResource].
 */
@QuarkusTest
@TestHTTPEndpoint(JobResource::class)
class JobResourceTest {
    @InjectMock
    private lateinit var jobService: JobService

    /**
     * Dummy values
     */
    private val dummyPortfolio = Portfolio(1, 1, "test", Targets(emptySet()))
    private val dummyTopology = Topology(1, 1, "test", emptyList(), Instant.now(), Instant.now())
    private val dummyTrace = Trace("bitbrains", "Bitbrains", "vm")
    private val dummyScenario = Scenario(1, 1, dummyPortfolio, "test", Workload(dummyTrace, 1.0), dummyTopology, OperationalPhenomena(false, false), "test",)
    private val dummyJob = Job(1, dummyScenario, JobState.PENDING, Instant.now(), Instant.now())

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(jobService, JobService::class.java)
    }

    /**
     * Test that tries to query the pending jobs without token.
     */
    @Test
    fun testQueryWithoutToken() {
        When {
            get()
        } Then {
            statusCode(401)
        }
    }

    /**
     * Test that tries to query the pending jobs for a user.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["openid"])
    fun testQueryInvalidScope() {
        When {
            get()
        } Then {
            statusCode(403)
        }
    }

    /**
     * Test that tries to query the pending jobs for a runner.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testQuery() {
        every { jobService.queryPending() } returns listOf(dummyJob)

        When {
            get()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("get(0).id", equalTo(1))
        }
    }

    /**
     * Test that tries to obtain a non-existent job.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testGetNonExisting() {
        every { jobService.findById(1) } returns null

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
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testGetExisting() {
        every { jobService.findById(1) } returns dummyJob

        When {
            get("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("id", equalTo(1))
        }
    }

    /**
     * Test that tries to update a non-existent job.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testUpdateNonExistent() {
        every { jobService.updateState(1, any(), any()) } returns null

        Given {
            body(Job.Update(JobState.PENDING))
            contentType(ContentType.JSON)
        } When {
            post("/1")
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to update a job.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testUpdateState() {
        every { jobService.updateState(1, any(), any()) } returns dummyJob.copy(state = JobState.CLAIMED)

        Given {
            body(Job.Update(JobState.CLAIMED))
            contentType(ContentType.JSON)
        } When {
            post("/1")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("state", equalTo(JobState.CLAIMED.toString()))
        }
    }

    /**
     * Test that tries to update a job with invalid input.
     */
    @Test
    @TestSecurity(user = "testUser", roles = ["runner"])
    fun testUpdateInvalidInput() {
        Given {
            body("""{ "test": "test" }""")
            contentType(ContentType.JSON)
        } When {
            post("/1")
        } Then {
            statusCode(400)
            contentType(ContentType.JSON)
        }
    }
}
