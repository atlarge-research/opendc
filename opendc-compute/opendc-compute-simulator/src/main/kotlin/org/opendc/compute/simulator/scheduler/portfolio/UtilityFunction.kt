/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.simulator.scheduler.portfolio

import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask

/**
 * Represents a hypothetical placement of a task on a host,
 * used by utility functions to simulate the effect of a scheduling decision.
 *
 * @param host The host on which the task would be placed.
 * @param task The task to be placed.
 */
public data class SimulatedPlacement(
    val host: HostView,
    val task: ServiceTask,
)

/**
 * A utility function used by the portfolio scheduler to evaluate the risk score
 * of the current system state. Lower scores indicate less risk.
 */
public interface UtilityFunction {
    /**
     * Evaluate the risk score for the current system state, optionally considering
     * a simulated placement that has not yet been committed.
     *
     * @param hosts The set of all hosts in the system.
     * @param simulatedPlacement An optional hypothetical placement to factor into the evaluation.
     * @return A risk score in [0, 1], where lower is better.
     */
    public fun evaluate(
        hosts: Collection<HostView>,
        simulatedPlacement: SimulatedPlacement? = null,
    ): Double
}
