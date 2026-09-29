/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.simulator.compute.scheduler.filters

import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.task.SimTask

/**
 * A [HostFilter] that only passes hosts with enough room of some kind, such as free cores or free memory, measured as
 * a single number. The [org.opendc.simulator.compute.scheduler.FilterScheduler] uses this to rule out hosts, and whole
 * blocks of hosts, without testing them.
 *
 * [test] may do more checks than the threshold, but it may only pass when [available] is at least [required].
 */
public interface ThresholdFilter : HostFilter {
    /**
     * How much room [host] has. This may only change when the resources in use on the host change.
     */
    public fun available(host: SimHost): Double

    /**
     * How much room [task] needs.
     */
    public fun required(task: SimTask): Double
}
