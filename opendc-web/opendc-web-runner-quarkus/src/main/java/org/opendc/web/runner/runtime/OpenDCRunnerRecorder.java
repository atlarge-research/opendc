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

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import java.io.File;
import javax.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;
import org.opendc.web.runner.JobManager;
import org.opendc.web.runner.OpenDCRunner;

/**
 * Helper class for starting the OpenDC web runner.
 */
@Recorder
public class OpenDCRunnerRecorder {
    private static final Logger LOGGER = Logger.getLogger(OpenDCRunnerRecorder.class.getName());

    /**
     * Helper method to create an {@link OpenDCRunner} instance.
     */
    public RuntimeValue<OpenDCRunner> createRunner(OpenDCRunnerRuntimeConfig config) {
        int parallelism = config.parallelism;
        if (parallelism < 0) {
            throw new IllegalArgumentException("Parallelism must be non-negative");
        } else if (parallelism == 0) {
            parallelism = Math.min(1, Runtime.getRuntime().availableProcessors() - 1);
        }

        JobManager manager = CDI.current().select(JobManager.class).get();
        OpenDCRunner runner = new OpenDCRunner(
                manager,
                new File(config.tracePath),
                parallelism,
                config.jobTimeout,
                config.pollInterval,
                config.heartbeatInterval);

        return new RuntimeValue<>(runner);
    }

    /**
     * Helper method to start the OpenDC runner service.
     */
    public void startRunner(
            RuntimeValue<OpenDCRunner> runner, OpenDCRunnerRuntimeConfig config, ShutdownContext shutdownContext) {
        if (config.enable) {
            LOGGER.info("Starting OpenDC Runner in background (polling every " + config.pollInterval + ")");

            Thread thread = new Thread(runner.getValue());
            thread.setName("opendc-runner");
            thread.start();

            shutdownContext.addShutdownTask(thread::interrupt);
        }
    }
}
