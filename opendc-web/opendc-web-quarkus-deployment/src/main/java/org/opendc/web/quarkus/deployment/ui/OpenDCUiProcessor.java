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

package org.opendc.web.quarkus.deployment.ui;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import org.opendc.web.quarkus.runtime.ui.OpenDCUiConfig;
import org.opendc.web.quarkus.runtime.ui.OpenDCUiRecorder;

/**
 * Quarkus build processor for the OpenDC web UI.
 */
public class OpenDCUiProcessor {
    /**
     * Register a route handler for serving the config of the OpenDC web UI.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RouteBuildItem registerConfigHandler(
            OpenDCUiRecorder openDCUiRecorder,
            BuildProducer<RouteBuildItem> routes,
            HttpRootPathBuildItem httpRootPathBuildItem,
            OpenDCUiConfig openDCUiConfig) {

        String basePath = httpRootPathBuildItem.getRootPath();

        return httpRootPathBuildItem
                .routeBuilder()
                .route(basePath + "/__ENV.js")
                .handler(openDCUiRecorder.configHandler(openDCUiConfig))
                .build();
    }
}
