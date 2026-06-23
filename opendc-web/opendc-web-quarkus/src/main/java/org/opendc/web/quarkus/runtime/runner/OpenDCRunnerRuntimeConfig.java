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

package org.opendc.web.quarkus.runtime.runner;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.time.Duration;

/**
 * Configuration for the OpenDC web runner.
 */
@ConfigMapping(prefix = "quarkus.opendc-runner")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OpenDCRunnerRuntimeConfig {
    /**
     * Flag to indicate whether the runner should be enabled.
     */
    @WithDefault("true")
    boolean enable();

    /**
     * The path where the workload traces are located.
     */
    @WithDefault("traces")
    String tracePath();

    /**
     * The number of concurrent simulations
     */
    @WithDefault("1")
    int parallelism();

    /**
     * The maximum duration of a job.
     */
    @WithDefault("10m")
    Duration jobTimeout();

    /**
     * The interval between successive polls to the API.
     */
    @WithDefault("30s")
    Duration pollInterval();

    /**
     * The interval between successive heartbeats to the API.
     */
    @WithDefault("1m")
    Duration heartbeatInterval();
}
