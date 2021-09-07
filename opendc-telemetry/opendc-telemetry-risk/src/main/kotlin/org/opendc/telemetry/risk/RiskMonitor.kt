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

package org.opendc.telemetry.risk

import io.opentelemetry.sdk.metrics.export.MetricProducer
import org.opendc.telemetry.risk.internal.RiskMonitorImpl
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * A class to monitor for risk in OpenDC components.
 */
public interface RiskMonitor : AutoCloseable {
    /**
     * Start the risk monitoring process.
     */
    public fun start()

    /**
     * Add the specified [listener] to this monitor.
     */
    public fun addListener(listener: RiskMonitorListener)

    /**
     * Remove the specified [listener] from this monitor.
     */
    public fun removeListener(listener: RiskMonitorListener)

    /**
     * Stop monitoring of risk.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a [RiskMonitor] instance.
         *
         * @param context The [CoroutineContext] to run the monitor in.
         * @param clock The [Clock] instance to track time.
         * @param producers The [MetricProducer]s to obtain metric data from.
         * @param factors The list of risk factors to consider.
         */
        public operator fun invoke(context: CoroutineContext, clock: Clock, producers: List<MetricProducer>, factors: List<RiskFactor>): RiskMonitor {
            return RiskMonitorImpl(context, clock, producers, factors)
        }
    }
}
