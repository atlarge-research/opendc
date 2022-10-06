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

package org.opendc.web.runner.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import java.time.Duration;

/**
 * Configuration for the OpenDC web runner.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "opendc-runner")
public class OpenDCRunnerRuntimeConfig {
    /**
     * Flag to indicate whether the runner should be enabled.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enable;

    /**
     * The path where the workload traces are located.
     */
    @ConfigItem(defaultValue = "traces")
    public String tracePath;

    /**
     * The number of concurrent simulations
     */
    @ConfigItem(defaultValue = "1")
    public int parallelism;

    /**
     * The maximum duration of a job.
     */
    @ConfigItem(defaultValue = "10m")
    public Duration jobTimeout;

    /**
     * The interval between successive polls to the API.
     */
    @ConfigItem(defaultValue = "30s")
    public Duration pollInterval;

    /**
     * The interval between successive heartbeats to the API.
     */
    @ConfigItem(defaultValue = "1m")
    public Duration heartbeatInterval;
}
