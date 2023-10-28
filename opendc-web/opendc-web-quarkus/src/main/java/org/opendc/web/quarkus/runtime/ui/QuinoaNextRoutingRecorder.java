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

package org.opendc.web.quarkus.runtime.ui;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Recorder class for building route handlers for Next.js pages and redirects.
 */
@Recorder
public class QuinoaNextRoutingRecorder {
    /**
     * Construct a {@link Handler} for serving a dynamic route of a Next.js application.
     */
    public Handler<RoutingContext> pageHandler(String basePath, String page) {
        return (event) -> event.reroute(basePath + page + ".html");
    }

    /**
     * Construct a {@link Handler} for handling redirects of a Next.js application.
     */
    public Handler<RoutingContext> redirectHandler(String destination, int statusCode) {
        return (event) -> {
            String query = event.request().query();
            String fullDestination = query != null ? destination + "?" + query : destination;

            event.response()
                    .setStatusCode(statusCode)
                    .putHeader("Location", fullDestination)
                    .end();
        };
    }
}
