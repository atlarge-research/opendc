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

import static io.vertx.ext.web.handler.StaticHandler.DEFAULT_MAX_AGE_SECONDS;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Recorder class for the OpenDC web UI.
 */
@Recorder
public class OpenDCUiRecorder {
    /**
     * Construct a {@link Handler} for serving the configuration of the OpenDC web UI.
     */
    public Handler<RoutingContext> configHandler() {
        return (event) -> {
            HttpServerRequest request = event.request();
            if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
                event.next();
                return;
            }

            OpenDCUiConfig config = ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getConfigMapping(OpenDCUiConfig.class);
            String dateHeader = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneOffset.UTC));

            event.response()
                    .setStatusCode(200)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
                    .putHeader(HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=" + DEFAULT_MAX_AGE_SECONDS)
                    .putHeader(HttpHeaders.DATE, dateHeader)
                    .end(OpenDCUiRecorder.serializeConfig(config));
        };
    }

    private static String serializeConfig(OpenDCUiConfig config) {
        JsonObject configJson = JsonObject.of("NEXT_PUBLIC_API_BASE_URL", config.apiBaseUrl());
        config.auth().domain().ifPresent(s -> configJson.put("NEXT_PUBLIC_AUTH0_DOMAIN", s));
        config.auth().clientId().ifPresent(s -> configJson.put("NEXT_PUBLIC_AUTH0_CLIENT_ID", s));
        config.auth().audience().ifPresent(s -> configJson.put("NEXT_PUBLIC_AUTH0_AUDIENCE", s));
        config.sentryDsn().ifPresent(s -> configJson.put("NEXT_PUBLIC_SENTRY_DSN", s));

        return "window.__ENV = " + configJson.encode() + ";";
    }
}
