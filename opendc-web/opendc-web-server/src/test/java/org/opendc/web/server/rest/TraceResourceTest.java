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

package org.opendc.web.server.rest;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendc.web.server.model.Trace;

/**
 * Test suite for {@link TraceResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(TraceResource.class)
public final class TraceResourceTest {
    /**
     * Set up the test environment.
     */
    @BeforeEach
    public void setUp() {
        PanacheMock.mock(Trace.class);
    }

    /**
     * Test that tries to obtain all traces (empty response).
     */
    @Test
    public void testGetAllEmpty() {
        Mockito.when(Trace.streamAll()).thenReturn(Stream.of());

        when().get().then().statusCode(200).contentType(ContentType.JSON).body("", Matchers.empty());
    }

    /**
     * Test that tries to obtain a non-existent trace.
     */
    @Test
    public void testGetNonExisting() {
        Mockito.when(Trace.findById("bitbrains")).thenReturn(null);

        when().get("/bitbrains").then().statusCode(404).contentType(ContentType.JSON);
    }

    /**
     * Test that tries to obtain an existing trace.
     */
    @Test
    public void testGetExisting() {
        Mockito.when(Trace.findById("bitbrains")).thenReturn(new Trace("bitbrains", "Bitbrains", "VM"));

        when().get("/bitbrains")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Bitbrains"));
    }
}
