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

package org.opendc.compute.simulator.provisioner

import org.opendc.compute.carbon.CarbonTrace
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.ComputeMetricReader
import org.opendc.compute.simulator.telemetry.ComputeMonitor
import java.time.Duration

/**
 * A [ProvisioningStep] that provisions a [ComputeMetricReader] to periodically collect the metrics of a [ComputeService]
 * and report them to a [ComputeMonitor].
 */
public class ComputeMonitorProvisioningStep(
    private val serviceDomain: String,
    private val monitor: ComputeMonitor,
    private val exportInterval: Duration,
    private val startTime: Duration = Duration.ofMillis(0),
    private val carbonTrace: CarbonTrace = CarbonTrace(null),
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }
        val metricReader =
            ComputeMetricReader(
                ctx.dispatcher,
                service,
                monitor,
                exportInterval,
                startTime,
                carbonTrace,
            )
        return AutoCloseable { metricReader.close() }
    }
}
