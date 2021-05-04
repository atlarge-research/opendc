/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.harness.runner.cli

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import mu.KotlinLogging
import org.opendc.harness.api.Trial
import org.opendc.harness.engine.ExperimentExecutionListener

/**
 * A reporter that reports the experiment progress to the console.
 */
public class ConsoleExperimentReporter : ExperimentExecutionListener, AutoCloseable {
    /**
     * The active [Trial]s.
     */
    private val trials: MutableSet<Trial> = mutableSetOf()

    /**
     * The total number of runs.
     */
    private var total = 0

    /**
     * The logger for this reporter.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The progress bar to keep track of the progress.
     */
    private val pb: ProgressBar = ProgressBarBuilder()
        .setTaskName("")
        .setInitialMax(1)
        .build()

    override fun trialFinished(trial: Trial, throwable: Throwable?) {
        trials -= trial

        pb.stepTo(total - trials.size.toLong())
        if (trials.isEmpty()) {
            pb.close()
        }

        if (throwable != null) {
            logger.warn(throwable) { "Trial $trial failed" }
        }
    }

    override fun trialStarted(trial: Trial) {
        trials += trial
        pb.maxHint((++total).toLong())
    }

    override fun close() {
        pb.close()
    }
}
