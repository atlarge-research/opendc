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

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link TraceResource}.
 */
@QuarkusTest
@TestHTTPEndpoint(TraceResource.class)
public final class TraceResourceTest {
    /**
     * Test that tries to obtain all traces.
     */
    @Test
    public void testGetAllEmpty() {
        when().get().then().statusCode(200);
    }

    /**
     * Test that tries to obtain a non-existent trace.
     */
    @Test
    public void testGetNonExisting() {
        when().get("/unknown").then().statusCode(404);
    }

    /**
     * Test that tries to obtain an existing trace.
     */
    @Test
    public void testGetExisting() {
        when().get("/bitbrains-small")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Bitbrains Small"));
    }
}
