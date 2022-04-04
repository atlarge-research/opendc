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

package org.opendc.web.api.rest

import io.mockk.every
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.web.api.service.TraceService
import org.opendc.web.proto.Trace

/**
 * Test suite for [TraceResource].
 */
@QuarkusTest
@TestHTTPEndpoint(TraceResource::class)
class TraceResourceTest {
    @InjectMock
    private lateinit var traceService: TraceService

    @BeforeEach
    fun setUp() {
        QuarkusMock.installMockForType(traceService, TraceService::class.java)
    }

    /**
     * Test that tries to obtain all traces (empty response).
     */
    @Test
    fun testGetAllEmpy() {
        every { traceService.findAll() } returns emptyList()

        When {
            get()
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("", Matchers.empty<String>())
        }
    }

    /**
     * Test that tries to obtain a non-existent trace.
     */
    @Test
    fun testGetNonExisting() {
        every { traceService.findById("bitbrains") } returns null

        When {
            get("/bitbrains")
        } Then {
            statusCode(404)
            contentType(ContentType.JSON)
        }
    }

    /**
     * Test that tries to obtain an existing trace.
     */
    @Test
    fun testGetExisting() {
        every { traceService.findById("bitbrains") } returns Trace("bitbrains", "Bitbrains", "VM")

        When {
            get("/bitbrains")
        } Then {
            statusCode(200)
            contentType(ContentType.JSON)
            body("name", equalTo("Bitbrains"))
        }
    }
}
