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

package org.opendc.web.ui.runtime;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for serving the OpenDC web interface.
 */
@Recorder
public class OpenDCUiRecorder {
    /**
     * Construct a {@link Handler} for serving a page of the OpenDC web interface.
     */
    public Handler<RoutingContext> pageHandler(
        String finalDestination,
        String page,
        OpenDCUiRuntimeConfig runtimeConfig
    ) {
        if (runtimeConfig.enable) {
            String pageDirectory = finalDestination + "/pages";
            return (event) -> {
                event.response()
                    .setStatusCode(200)
                    .sendFile(pageDirectory + page + ".html");
            };
        }

        return new WebJarNotFoundHandler();
    }

    /**
     * Construct a {@link Handler} for handling redirects in the OpenDC web interface.
     */
    public Handler<RoutingContext> redirectHandler(
        String destination,
        int statusCode,
        OpenDCUiRuntimeConfig runtimeConfig
    ) {
        if (runtimeConfig.enable) {
            return (event) -> {
                String query = event.request().query();
                String fullDestination = query != null ? destination + "?" + query : destination;

                event.response()
                    .setStatusCode(statusCode)
                    .putHeader("Location", fullDestination)
                    .end();
            };
        }

        return new WebJarNotFoundHandler();
    }

    /**
     * Construct a {@link Handler} for serving the static files of the OpenDC web interface.
     */
    public Handler<RoutingContext> staticHandler(
        String finalDestination,
        String path,
        List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
        OpenDCUiRuntimeConfig runtimeConfig,
        ShutdownContext shutdownContext
    ) {
        if (runtimeConfig.enable) {
            var augmentedWebRootConfigurations = webRootConfigurations
                .stream()
                .map(c -> new FileSystemStaticHandler.StaticWebRootConfiguration(c.getFileSystem(), c.getWebRoot().isEmpty() ? "static" : c.getWebRoot() + "/static"))
                .collect(Collectors.toList());

            WebJarStaticHandler handler = new WebJarStaticHandler(finalDestination + "/static", path, augmentedWebRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        }

        return new WebJarNotFoundHandler();
    }
}
