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

package org.opendc.simulator.compute.workload

import org.opendc.simulator.compute.SimExecutionContext

/**
 * A model that characterizes the runtime behavior of some particular workload.
 *
 * Workloads are stateful objects that may be paused and resumed at a later moment. As such, be careful when using the
 * same [SimWorkload] from multiple contexts.
 */
public interface SimWorkload {
    /**
     * This method is invoked when the workload is started, before the (virtual) CPUs assigned to the workload will
     * start.
     */
    public fun onStart(ctx: SimExecutionContext)

    /**
     * This method is invoked when a (virtual) CPU assigned to the workload has started.
     *
     * @param ctx The execution context in which the workload runs.
     * @param cpu The index of the (virtual) CPU to start.
     * @return The command to perform on the CPU.
     */
    public fun onStart(ctx: SimExecutionContext, cpu: Int): SimResourceCommand

    /**
     * This method is invoked when a (virtual) CPU assigned to the workload was interrupted or reached its deadline.
     *
     * @param ctx The execution context in which the workload runs.
     * @param cpu The index of the (virtual) CPU to obtain the resource consumption of.
     * @param remainingWork The remaining work that was not yet completed.
     * @return The next command to perform on the CPU.
     */
    public fun onNext(ctx: SimExecutionContext, cpu: Int, remainingWork: Double): SimResourceCommand
}
