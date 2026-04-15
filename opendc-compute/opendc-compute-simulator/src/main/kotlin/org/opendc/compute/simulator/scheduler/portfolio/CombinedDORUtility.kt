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

/**
 * Combined Disaster-Operational Risk (DOR) utility function.
 *
 * Combines Operational Risk and Disaster Recovery Risk as a weighted average.
 * Based on Equation 4 from "Portfolio Scheduling for Managing Operational and
 * Disaster-Recovery Risks" (van Beek et al., ISPDC 2019).
 *
 * rod = (wo * ro + wd * rd) / (wo + wd)
 *
 * @param operationalWeight The weight for the Operational Risk component.
 * @param disasterRecoveryWeight The weight for the Disaster Recovery Risk component.
 */
public class CombinedDORUtility(
    private val operationalWeight: Double = 1.0,
    private val disasterRecoveryWeight: Double = 1.0,
    forwardLookMs: Long = 3600000L,
) : UtilityFunction {
    private val orUtility = OperationalRiskUtility(forwardLookMs)
    private val drrUtility = DisasterRecoveryRiskUtility()

    init {
        require(operationalWeight >= 0.0) { "Operational weight must be non-negative" }
        require(disasterRecoveryWeight >= 0.0) { "Disaster recovery weight must be non-negative" }
        require(operationalWeight + disasterRecoveryWeight > 0.0) { "At least one weight must be positive" }
    }

    override fun evaluate(
        hosts: Collection<HostView>,
        simulatedPlacement: SimulatedPlacement?,
    ): Double {
        val or = orUtility.evaluate(hosts, simulatedPlacement)
        val drr = drrUtility.evaluate(hosts, simulatedPlacement)
        return (operationalWeight * or + disasterRecoveryWeight * drr) / (operationalWeight + disasterRecoveryWeight)
    }
}
