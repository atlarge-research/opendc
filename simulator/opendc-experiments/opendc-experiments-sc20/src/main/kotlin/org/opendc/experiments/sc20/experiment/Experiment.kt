/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.experiments.sc20.experiment

import org.opendc.experiments.sc20.runner.ContainerExperimentDescriptor
import org.opendc.experiments.sc20.runner.ExperimentDescriptor
import org.opendc.experiments.sc20.runner.execution.ExperimentExecutionContext
import org.opendc.experiments.sc20.runner.execution.ExperimentExecutionListener
import org.opendc.experiments.sc20.telemetry.RunEvent
import org.opendc.experiments.sc20.telemetry.parquet.ParquetRunEventWriter
import org.opendc.format.trace.PerformanceInterferenceModelReader
import java.io.File

/**
 * The global configuration of the experiment.
 *
 * @param environments The path to the topologies directory.
 * @param traces The path to the traces directory.
 * @param output The output directory.
 * @param performanceInterferenceModel The optional performance interference model that has been specified.
 * @param vmPlacements Original VM placement in the trace.
 * @param bufferSize The buffer size of the event reporters.
 */
public abstract class Experiment(
    public val environments: File,
    public val traces: File,
    public val output: File,
    public val performanceInterferenceModel: PerformanceInterferenceModelReader?,
    public val vmPlacements: Map<String, String>,
    public val bufferSize: Int
) : ContainerExperimentDescriptor() {
    override val parent: ExperimentDescriptor? = null

    override suspend fun invoke(context: ExperimentExecutionContext) {
        val writer = ParquetRunEventWriter(File(output, "experiments.parquet"), bufferSize)
        try {
            val listener = object : ExperimentExecutionListener by context.listener {
                override fun descriptorRegistered(descriptor: ExperimentDescriptor) {
                    if (descriptor is Run) {
                        writer.write(RunEvent(descriptor, System.currentTimeMillis()))
                    }

                    context.listener.descriptorRegistered(descriptor)
                }
            }

            val newContext = object : ExperimentExecutionContext by context {
                override val listener: ExperimentExecutionListener = listener
            }

            super.invoke(newContext)
        } finally {
            writer.close()
        }
    }
}
